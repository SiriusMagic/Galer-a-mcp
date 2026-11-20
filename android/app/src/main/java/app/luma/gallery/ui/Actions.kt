package app.luma.gallery.ui

import app.luma.gallery.data.Media

/** What needs Android (other apps, system confirmations, background work); the screens only call these. */
class GalleryActions(
    val share: (List<Media>) -> Unit = {},
    /** Moves to «Eliminado recientemente» (Android asks first); null before Android 11. */
    val trash: ((List<Media>) -> Unit)? = null,
    val restore: (List<Media>) -> Unit = {},
    val deleteForever: (List<Media>) -> Unit = {},
    val edit: (Media) -> Unit = {},
    val wallpaper: (Media) -> Unit = {},
    val copyClipboard: (Media) -> Unit = {},
    val openWith: (Media) -> Unit = {},
    val move: (List<Media>, String) -> Unit = { _, _ -> },
    val copy: (List<Media>, String) -> Unit = { _, _ -> },
    /** Into the vault (Android asks before deleting the originals). */
    val hide: (List<Media>) -> Unit = {},
    val pdf: (List<Media>) -> Unit = {},
    /** Asks for the phone's lock, then opens the vault. */
    val openVault: () -> Unit = {},
    val unhide: (List<app.luma.gallery.data.Hidden>) -> Unit = {},
    val eraseHidden: (List<app.luma.gallery.data.Hidden>) -> Unit = {},
    /** Modo MCP: copy a link (never shown whole) and open the Gateway Hub app. */
    val copyLink: (String) -> Unit = {},
    val goToHub: () -> Unit = {},
    val indexNow: () -> Unit = {},
    val stopIndex: () -> Unit = {},
    val clearIndex: () -> Unit = {},
)

/** How far the content index is: [analyzed] of [total] photos; [running] while the analysis works now. */
data class IndexStatus(val analyzed: Int = 0, val total: Int = 0, val running: Boolean = false)
