package app.luma.gallery

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.luma.gallery.data.Appearance
import app.luma.gallery.data.Favorites
import app.luma.gallery.ui.screens.Group
import app.luma.gallery.ui.screens.ThemePicker
import app.luma.gallery.ui.theme.ThemeMode
import app.luma.gallery.data.Media
import app.luma.gallery.data.MediaStoreSource
import app.luma.gallery.data.MediaWriter
import app.luma.gallery.data.Vault
import app.luma.gallery.index.IndexWorker
import app.luma.gallery.index.SqlFacts
import app.luma.gallery.ui.GalleryActions
import app.luma.gallery.ui.GalleryState
import app.luma.gallery.ui.IndexStatus
import app.luma.gallery.ui.LumaApp
import app.luma.gallery.ui.theme.LumaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** FragmentActivity: the vault asks for the phone's lock with BiometricPrompt. */
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val favorites = Favorites(this)
        val source = MediaStoreSource(this)
        val facts = SqlFacts(this)
        val writer = MediaWriter(this)
        val vault = Vault(this, writer)
        IndexWorker.schedule(this)
        val appearance = Appearance(this)
        setContent {
            var mode by remember { mutableStateOf(appearance.mode) }
            var accent by remember { mutableStateOf(appearance.accent) }
            val dark = mode == ThemeMode.DARK || (mode == ThemeMode.SYSTEM && androidx.compose.foundation.isSystemInDarkTheme())
            LumaTheme(dark, accent) {
                val state = remember { GalleryState(favorites.read(), favorites::write) }
                DisposableEffect(state) { lockOnStop = state::lockVault; onDispose { lockOnStop = null } }
                val scope = rememberCoroutineScope()
                var granted by remember { mutableStateOf(MediaAccess.granted(this)) }
                fun reload() = scope.launch {
                    if (!granted) return@launch
                    state.show(source.load(), if (Build.VERSION.SDK_INT >= 30) source.load(trashed = true) else emptyList())
                    state.showFacts(withContext(Dispatchers.IO) { facts.all() })
                    state.showHidden(withContext(Dispatchers.IO) { vault.list() })
                }
                /** What to do once Android's confirmation comes back (moving needs write access; hiding deletes originals). */
                var afterConfirm by remember { mutableStateOf<(suspend (Boolean) -> Unit)?>(null) }
                val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    granted = MediaAccess.granted(this); reload()
                }
                // Android asks the user before trashing, restoring or deleting; the lists are read again afterwards.
                val confirm = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                    val ok = it.resultCode == RESULT_OK
                    val then = afterConfirm; afterConfirm = null
                    scope.launch { then?.invoke(ok); if (ok) state.viewing = null; reload() }
                }
                fun ask(request: android.app.PendingIntent, then: (suspend (Boolean) -> Unit)? = null) {
                    afterConfirm = then
                    confirm.launch(IntentSenderRequest.Builder(request.intentSender).build())
                }
                fun say(text: String) = android.widget.Toast.makeText(this, text, android.widget.Toast.LENGTH_LONG).show()
                // Coming back to Luma (a photo taken, deleted elsewhere, access changed) shows the current state.
                LifecycleResumeEffect(Unit) { granted = MediaAccess.granted(this@MainActivity); reload(); onPauseOrDispose { } }
                val work by remember { WorkManager.getInstance(this).getWorkInfosForUniqueWorkFlow(IndexWorker.NOW) }.collectAsState(emptyList())
                val running = work.any { it.state == WorkInfo.State.RUNNING }
                LaunchedEffect(running) { if (!running) reload() } // new facts when an analysis ends
                val uris = { items: List<Media> -> items.map { it.uri } }
                LumaApp(state, granted, { ask.launch(MediaAccess.permissions()) }, GalleryActions(
                    share = ::share,
                    trash = if (Build.VERSION.SDK_INT >= 30) { items -> ask(MediaStore.createTrashRequest(contentResolver, uris(items), true)) } else null,
                    restore = { if (Build.VERSION.SDK_INT >= 30) ask(MediaStore.createTrashRequest(contentResolver, uris(it), false)) },
                    deleteForever = { if (Build.VERSION.SDK_INT >= 30) ask(MediaStore.createDeleteRequest(contentResolver, uris(it))) },
                    edit = { open(Intent.ACTION_EDIT, it) },
                    play = { open(Intent.ACTION_VIEW, it) },
                    wallpaper = { open(Intent.ACTION_ATTACH_DATA, it) },
                    openWith = { open(Intent.ACTION_VIEW, it) },
                    copyClipboard = {
                        getSystemService(android.content.ClipboardManager::class.java)
                            ?.setPrimaryClip(android.content.ClipData.newUri(contentResolver, it.name, it.uri))
                        android.widget.Toast.makeText(this, R.string.copied, android.widget.Toast.LENGTH_SHORT).show()
                    },
                    move = { items, album ->
                        if (Build.VERSION.SDK_INT >= 30) ask(MediaStore.createWriteRequest(contentResolver, uris(items))) { ok ->
                            if (ok) writer.move(items, album).let { n -> say(resources.getQuantityString(R.plurals.moved, n, n, album)) }
                        }
                    },
                    copy = { items, album -> scope.launch { writer.copy(items, album).let { n -> say(resources.getQuantityString(R.plurals.copied_to, n, n, album)) }; reload() } },
                    hide = { items ->
                        scope.launch {
                            val added = vault.hide(items.map { it to writer.mime(it) }) { contentResolver.openInputStream(it) }
                            val done = items.filter { m -> added.any { it.name == m.name } }
                            if (Build.VERSION.SDK_INT >= 30 && done.isNotEmpty()) ask(MediaStore.createDeleteRequest(contentResolver, uris(done))) { ok ->
                                if (ok) say(resources.getQuantityString(R.plurals.hidden_done, done.size, done.size)) else vault.erase(added) // cancelled: nothing hidden
                            } else { vault.erase(added); reload() }
                        }
                    },
                    pdf = { items ->
                        scope.launch {
                            val pdf = writer.exportPdf(items, "Luma " + java.time.LocalDateTime.now().toString().take(16).replace(':', '-'))
                            if (pdf == null) say(getString(R.string.pdf_failed)) else {
                                say(getString(R.string.pdf_done))
                                runCatching { startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(pdf, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                            }
                        }
                    },
                    openVault = { VaultLock.ask(this, { state.unlockVault() }) { say(getString(R.string.vault_needs_lock)) } },
                    unhide = { list -> scope.launch { vault.restore(list).let { n -> say(resources.getQuantityString(R.plurals.unhidden, n, n)) }; reload() } },
                    eraseHidden = { list -> scope.launch { vault.erase(list); reload() } },
                    indexNow = { IndexWorker.runNow(this) },
                    stopIndex = { IndexWorker.stop(this) },
                    clearIndex = { scope.launch { withContext(Dispatchers.IO) { facts.clear() }; reload() } },
                ), IndexStatus(state.facts.size.coerceAtMost(state.media.size), state.media.size, running), BuildConfigVersion.name(this)) {
                    Group(androidx.compose.ui.res.stringResource(R.string.theme)) {
                        ThemePicker(mode, accent, { mode = it; appearance.mode = it }, { accent = it; appearance.accent = it })
                    }
                }
            }
        }
    }

    /** Leaving Luma closes the vault: the lock is asked again next time. */
    override fun onStop() { super.onStop(); lockOnStop?.invoke() }
    var lockOnStop: (() -> Unit)? = null

    private fun share(items: List<Media>) {
        if (items.isEmpty()) return
        val intent = if (items.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, items[0].uri)
            else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(items.map { it.uri }))
        intent.type = when { items.all { it.isVideo } -> "video/*"; items.none { it.isVideo } -> "image/*"; else -> "*/*" }
        startActivity(Intent.createChooser(intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), getString(R.string.share)))
    }

    /** Edit or play in another app; if the phone has none, nothing happens. */
    private fun open(action: String, item: Media) {
        val intent = Intent(action).setDataAndType(item.uri, if (item.isVideo) "video/*" else "image/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        runCatching { startActivity(Intent.createChooser(intent, null)) }
    }
}

/** Version shown in Ajustes. */
object BuildConfigVersion {
    fun name(context: android.content.Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
}
