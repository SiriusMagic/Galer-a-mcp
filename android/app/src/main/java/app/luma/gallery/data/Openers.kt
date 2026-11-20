package app.luma.gallery.data

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri

/** An app that can open a photo or video («Abrir con…»). */
data class Opener(val packageName: String, val activity: String, val label: String, val icon: Drawable? = null)

/** Apps that open [Media]: our own first (Stone for videos), then the rest by name; Luma itself is never listed. */
object Openers {
    /** Same developer, so they come first: Stone plays videos (and music) in its full player. */
    val OURS = listOf("com.zirakusa.stone.kotlin.preview")

    fun order(apps: List<Opener>, self: String): List<Opener> = apps.filter { it.packageName != self }
        .distinctBy { it.packageName to it.activity }
        .sortedWith(compareBy<Opener> { OURS.indexOf(it.packageName).let { i -> if (i < 0) Int.MAX_VALUE else i } }.thenBy { it.label.lowercase() })

    fun intent(uri: Uri, video: Boolean): Intent = Intent(Intent.ACTION_VIEW).setDataAndType(uri, if (video) "video/*" else "image/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    fun find(context: Context, item: Media): List<Opener> {
        val pm = context.packageManager
        val found = pm.queryIntentActivities(intent(item.uri, item.isVideo), 0).map {
            Opener(it.activityInfo.packageName, it.activityInfo.name, it.loadLabel(pm).toString(), runCatching { it.loadIcon(pm) }.getOrNull())
        }
        return order(found, context.packageName)
    }
}
