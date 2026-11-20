package app.luma.gallery

import app.luma.gallery.data.Opener
import app.luma.gallery.data.Openers
import org.junit.Assert.assertEquals
import org.junit.Test

/** «Abrir con…»: Stone first, then the rest by name, never Luma itself, no repeats. */
class OpenersTest {
    @Test fun stoneFirstThenByName() {
        val apps = listOf(Opener("org.videolan.vlc", "a", "VLC"), Opener("com.mi.video", "b", "Mi Video"),
            Opener("app.luma.gallery", "c", "Luma"), Opener("com.zirakusa.stone.kotlin.preview", "d", "Stone"),
            Opener("org.videolan.vlc", "a", "VLC"))
        assertEquals(listOf("Stone", "Mi Video", "VLC"), Openers.order(apps, "app.luma.gallery").map { it.label })
    }
}
