package app.luma.gallery

import android.content.ComponentName
import android.content.pm.PackageManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** The five launcher icons («Acuarela» by default): one alias enabled at a time, and never none. */
@RunWith(RobolectricTestRunner::class)
class AppIconTest {
    private val context = RuntimeEnvironment.getApplication()
    private fun name(alias: String) = ComponentName(context, "app.luma.gallery.$alias")
    private fun state(alias: String) = context.packageManager.getComponentEnabledSetting(name(alias))
    private fun set(alias: String, state: Int) = context.packageManager.setComponentEnabledSetting(name(alias), state, PackageManager.DONT_KILL_APP)

    @Test fun oneIconAtATime() {
        AppIcon.load(context); assertEquals(AppIcon.WATERCOLOR, AppIcon.current)
        AppIcon.entries.forEach { icon ->
            AppIcon.apply(context, icon)
            assertEquals(icon, AppIcon.current)
            AppIcon.entries.forEach { other ->
                assertEquals(other.name, if (other == icon) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state(other.alias))
            }
            assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state(AppIcon.LEGACY))
        }
        AppIcon.entries.forEach { set(it.alias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED) }
        AppIcon.load(context); assertEquals(AppIcon.WATERCOLOR, AppIcon.current)      // never without an icon
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state(AppIcon.WATERCOLOR.alias))
    }

    @Test fun oldClaroMovesToHome() {
        // A phone updated from 0.6.3 with «Claro» chosen: .HomeClaro on, .Home off.
        set(AppIcon.WATERCOLOR.alias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED); set(AppIcon.LEGACY, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        AppIcon.load(context)
        assertEquals(AppIcon.WATERCOLOR, AppIcon.current)
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_ENABLED, state(AppIcon.WATERCOLOR.alias))
        assertEquals(PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state(AppIcon.LEGACY))
    }

    @Test fun oldOriginalBecomesWatercolor() {
        // A phone updated from 0.6.3 with «Original»: .Home stays on and now shows «Acuarela»; nothing else changes.
        set(AppIcon.WATERCOLOR.alias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        AppIcon.entries.filter { it != AppIcon.WATERCOLOR }.forEach { set(it.alias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED) }
        AppIcon.load(context); assertEquals(AppIcon.WATERCOLOR, AppIcon.current)
    }
}
