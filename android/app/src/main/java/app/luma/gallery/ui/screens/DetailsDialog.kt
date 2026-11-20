package app.luma.gallery.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.luma.gallery.R
import app.luma.gallery.data.Library
import app.luma.gallery.data.Media
import app.luma.gallery.index.Facts
import app.luma.gallery.index.Labels
import java.text.DateFormat
import java.util.Date

/** Detalles: file data, and what the smart search found in it (what the AI will be able to search for). */
@Composable fun DetailsDialog(item: Media, facts: Facts?, onClose: () -> Unit) {
    val context = LocalContext.current
    val notYet = stringResource(R.string.detail_not_yet)
    val nothing = stringResource(R.string.detail_nothing)
    val rows = buildList {
        add(R.string.detail_name to item.name)
        add(R.string.detail_date to DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(item.takenAt)))
        add(R.string.detail_album to item.folder)
        if (item.size > 0) add(R.string.detail_size to Formatter.formatShortFileSize(context, item.size))
        if (item.width > 0) add(R.string.detail_resolution to "${item.width} × ${item.height}")
        if (item.isVideo) add(R.string.detail_duration to Library.duration(item.durationMs))
        add(R.string.detail_seen to (facts?.labels?.entries?.sortedByDescending { it.value }?.joinToString { Labels.spanish(it.key) }
            ?.ifBlank { null } ?: if (facts == null) notYet else nothing))
        facts?.text?.takeIf { it.isNotBlank() }?.let { add(R.string.detail_text to it.take(300)) }
    }
    AlertDialog(onClose, confirmButton = { TextButton(onClose) { Text(stringResource(R.string.close)) } },
        title = { Text(stringResource(R.string.details)) },
        text = {
            Column {
                rows.forEach { (label, value) ->
                    Text(stringResource(label), Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold)
                    Text(value, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .75f))
                }
            }
        })
}
