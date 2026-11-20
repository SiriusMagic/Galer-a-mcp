package app.luma.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.luma.gallery.data.Favorites
import app.luma.gallery.data.Media
import app.luma.gallery.data.MediaStoreSource
import app.luma.gallery.ui.GalleryState
import app.luma.gallery.ui.LumaApp
import app.luma.gallery.ui.screens.ViewerActions
import app.luma.gallery.ui.theme.LumaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val favorites = Favorites(this)
        val source = MediaStoreSource(this)
        setContent {
            LumaTheme {
                val state = remember { GalleryState(favorites.read(), favorites::write) }
                val scope = rememberCoroutineScope()
                var granted by remember { mutableStateOf(MediaAccess.granted(this)) }
                fun reload() { if (granted) scope.launch { state.show(source.load()) } }
                val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    granted = MediaAccess.granted(this); reload()
                }
                // Android asks the user before deleting (system dialog); the list is read again afterwards.
                val confirmDelete = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                    if (it.resultCode == RESULT_OK) { state.viewing = null; reload() }
                }
                // Coming back to Luma (a photo taken, deleted elsewhere, access changed) shows the current state.
                LifecycleResumeEffect(Unit) { granted = MediaAccess.granted(this@MainActivity); reload(); onPauseOrDispose { } }
                LumaApp(state, granted, { ask.launch(MediaAccess.permissions()) }, ViewerActions(
                    favorite = state::toggleFavorite,
                    share = ::share,
                    edit = { open(Intent.ACTION_EDIT, it) },
                    delete = if (Build.VERSION.SDK_INT >= 30) { item ->
                        val request = MediaStore.createDeleteRequest(contentResolver, listOf(item.uri))
                        confirmDelete.launch(IntentSenderRequest.Builder(request.intentSender).build())
                    } else null,
                    play = { open(Intent.ACTION_VIEW, it) }))
            }
        }
    }

    private fun share(item: Media) = startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND)
        .setType(if (item.isVideo) "video/*" else "image/*").putExtra(Intent.EXTRA_STREAM, item.uri)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), getString(R.string.share)))

    /** Edit or play in another app; if the phone has none, nothing happens. */
    private fun open(action: String, item: Media) {
        val intent = Intent(action).setDataAndType(item.uri, if (item.isVideo) "video/*" else "image/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        runCatching { startActivity(Intent.createChooser(intent, null)) }
    }
}

/** Read access to photos and videos only (never all files). Android 14+ may give access to a selection. */
object MediaAccess {
    fun permissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun granted(context: android.content.Context): Boolean = permissions().any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
}
