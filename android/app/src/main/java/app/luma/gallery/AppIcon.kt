package app.luma.gallery

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The app's launcher icons: its own drawing in four colour styles, «Claro» by default. Each is an `activity-alias` in the
 * manifest; exactly one is enabled. Alias names never change (Android keeps the enabled state by name): «Claro» lives in
 * `.Home` since 0.6.4, and the old `.HomeClaro` ([LEGACY]) is only moved over and switched off.
 */
enum class AppIcon(val alias: String, val label: Int, val preview: Int, val background: Long) {
    LIGHT("Home", R.string.icon_light, R.drawable.ic_launcher_foreground_light, 0xFFF2F2F2),
    GOLD("HomeDorado", R.string.icon_gold, R.drawable.ic_launcher_foreground_gold, 0xFFF5C518),
    BLUE("HomeAzul", R.string.icon_blue, R.drawable.ic_launcher_foreground_blue, 0xFF1565C0),
    GREEN("HomeVerde", R.string.icon_green, R.drawable.ic_launcher_foreground_green, 0xFF1B5E20);

    private fun component(context: Context) = component(context, alias)
    fun isEnabled(context: Context): Boolean = enabled(context, alias, byDefault = this == DEFAULT)

    companion object {
        val DEFAULT = LIGHT
        /** «Claro» before 0.6.4: still in the manifest so an update never leaves the phone without an icon. */
        const val LEGACY = "HomeClaro"
        var current by mutableStateOf(DEFAULT); private set

        private fun component(context: Context, alias: String) = ComponentName(context.packageName, "app.luma.gallery.$alias")
        private fun enabled(context: Context, alias: String, byDefault: Boolean) =
            when (context.packageManager.getComponentEnabledSetting(component(context, alias))) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
                PackageManager.COMPONENT_ENABLED_STATE_DEFAULT -> byDefault                  // as the manifest says
                else -> false
            }

        /** Reads the enabled one; the old «Claro» alias or none enabled (an odd update) brings back the default. */
        fun load(context: Context) {
            val on = entries.firstOrNull { it.isEnabled(context) }
            if (on == null || enabled(context, LEGACY, byDefault = false)) apply(context, on ?: DEFAULT) else current = on
        }

        /** Enables [icon] first and then disables the others, without closing the app. Launchers refresh on their own. */
        fun apply(context: Context, icon: AppIcon) {
            val pm = context.packageManager
            pm.setComponentEnabledSetting(icon.component(context), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
            (entries.filter { it != icon }.map { it.alias } + LEGACY).forEach {
                pm.setComponentEnabledSetting(component(context, it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            }
            current = icon
        }
    }
}

/** «Icono de la app»: the four icons; a tap changes the home-screen icon. */
@Composable fun IconCard(card: Color, text: Color, muted: Color, accent: Color) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(card).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.icon_title), color = text, fontSize = 18.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AppIcon.entries.forEach { icon ->
                val on = icon == AppIcon.current
                val name = stringResource(icon.label)
                Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClickLabel = name) { AppIcon.apply(context, icon) }.padding(2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color(icon.background))
                        .border(if (on) 3.dp else 0.dp, if (on) accent else Color.Transparent, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                        Image(painterResource(icon.preview), null, Modifier.size(52.dp))
                    }
                    Text(name, color = if (on) accent else muted, fontSize = 11.sp, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
        Text(stringResource(R.string.icon_hint), color = muted, fontSize = 13.sp)
    }
}
