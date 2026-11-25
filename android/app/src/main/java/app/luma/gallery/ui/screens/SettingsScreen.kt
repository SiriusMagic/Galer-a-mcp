package app.luma.gallery.ui.screens

import app.luma.gallery.IconCard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.luma.gallery.R
import app.luma.gallery.ui.IndexStatus

/** Ajustes: the smart search (status, analyze now, stop, forget), privacy and version. */
@Composable fun SettingsScreen(status: IndexStatus, version: String, onIndexNow: () -> Unit, onStop: () -> Unit, onClear: () -> Unit,
    modifier: Modifier = Modifier, extra: @Composable ColumnScope.() -> Unit = {}, header: @Composable () -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
        header()
        extra()
        Group(stringResource(R.string.index_title)) {
            Text(stringResource(R.string.index_hint), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f))
            Text(pluralStringResource(R.plurals.index_progress, status.analyzed, status.analyzed, status.total), Modifier.padding(top = 8.dp),
                color = MaterialTheme.colorScheme.primary)
            if (status.running) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
            Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (status.running) OutlinedButton(onStop) { Text(stringResource(R.string.index_stop)) }
                else Button(onIndexNow, enabled = status.analyzed < status.total) { Text(stringResource(R.string.index_now)) }
                TextButton(onClear, enabled = status.analyzed > 0) { Text(stringResource(R.string.index_clear)) }
            }
        }
        IconCard(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.primary)
        Group(stringResource(R.string.privacy)) { Text(stringResource(R.string.privacy_text), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .7f)) }
        Group(stringResource(R.string.version)) { Text(version) }
    }
}

@Composable fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(20.dp)) {
        Text(title, fontSize = 18.sp, modifier = Modifier.padding(bottom = 6.dp))
        content()
    }
}
