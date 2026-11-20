package app.luma.gallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.luma.gallery.R

/** Big title (as in the phone's gallery) with back on the left and a floating pill of actions on the right. */
@Composable fun Header(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) Pill { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) } }
            Spacer(Modifier.weight(1f))
            Pill(content = actions)
        }
        Text(title, Modifier.padding(start = 6.dp, top = 18.dp, bottom = 8.dp), fontSize = 40.sp, fontWeight = FontWeight.Normal,
            maxLines = 1)
    }
}

@Composable fun Pill(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable fun Action(icon: ImageVector, label: Int, onClick: () -> Unit) {
    IconButton(onClick) { Icon(icon, stringResource(label), Modifier.size(26.dp)) }
}

/** Floating bottom bar: Fotos · Álbumes · Memorias in a pill, and a round Buscar button apart, like the screenshots. */
@Composable fun BoxScope.FloatingNav(page: Page, onPage: (Page) -> Unit) {
    Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).shadow(8.dp, CircleShape).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(6.dp)) {
            NavTab(page == Page.PHOTOS, Icons.Default.Image, R.string.photos, Modifier.weight(1f)) { onPage(Page.PHOTOS) }
            NavTab(page == Page.ALBUMS || page == Page.TRASH || page == Page.VAULT, Icons.Default.PhotoLibrary, R.string.albums, Modifier.weight(1f)) { onPage(Page.ALBUMS) }
            NavTab(page == Page.MEMORIES, Icons.Default.AutoAwesomeMotion, R.string.memories, Modifier.weight(1f)) { onPage(Page.MEMORIES) }
        }
        Box(Modifier.padding(start = 12.dp).size(64.dp).shadow(8.dp, CircleShape).clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { onPage(Page.SEARCH) }, contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Search, stringResource(R.string.search), Modifier.size(30.dp),
                tint = if (page == Page.SEARCH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable private fun NavTab(active: Boolean, icon: ImageVector, label: Int, modifier: Modifier, onClick: () -> Unit) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Column(modifier.clip(CircleShape).background(if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
        .clickable(onClick = onClick).padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = tint)
        Text(stringResource(label), color = tint, fontSize = 13.sp)
    }
}

/** While picking: how many, select all, and the actions on them; replaces the floating bar. */
@Composable fun BoxScope.SelectionBar(count: Int, actions: List<Triple<ImageVector, Int, () -> Unit>>,
    more: List<Pair<Int, () -> Unit>> = emptyList()) {
    Row(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp).fillMaxWidth().shadow(8.dp, CircleShape)
        .clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(pluralLabel(count), Modifier.weight(1f).padding(start = 8.dp), fontWeight = FontWeight.Medium)
        actions.forEach { (icon, label, onClick) -> Action(icon, label, onClick) }
        if (more.isNotEmpty()) Box {
            var open by remember { mutableStateOf(false) }
            Action(Icons.Default.MoreVert, R.string.more) { open = true }
            DropdownMenu(open, { open = false }) {
                more.forEach { (label, onClick) -> DropdownMenuItem({ Text(stringResource(label)) }, { open = false; onClick() }) }
            }
        }
    }
}

@Composable private fun pluralLabel(count: Int) = androidx.compose.ui.res.pluralStringResource(R.plurals.selected, count, count)
