package app.luma.gallery.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.luma.gallery.R

/**
 * Where the photos go: an existing album or a new name, then Mover or Copiar. With [onlyName] (the «+» button) it only
 * asks the name of the new album; the photos are picked afterwards. A plain Dialog lays out three buttons in one row.
 */
@Composable fun AlbumPicker(albums: List<String>, onlyName: Boolean, onDismiss: () -> Unit, onMove: (String) -> Unit, onCopy: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var chosen by remember { mutableStateOf<String?>(null) }
    val target = name.trim().ifEmpty { chosen.orEmpty() }
    Dialog(onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(24.dp)) {
                Text(stringResource(if (onlyName) R.string.new_album else R.string.to_album), fontSize = 22.sp)
                OutlinedTextField(name, { name = it.take(60); chosen = null }, Modifier.fillMaxWidth().padding(top = 16.dp), singleLine = true,
                    placeholder = { Text(stringResource(R.string.new_album_name)) })
                if (!onlyName) LazyColumn(Modifier.heightIn(max = 300.dp).padding(top = 8.dp)) {
                    items(albums) { album ->
                        Row(Modifier.fillMaxWidth().clickable { chosen = album; name = "" }, verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(chosen == album, { chosen = album; name = "" })
                            Text(album)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onDismiss) { Text(stringResource(R.string.cancel)) }
                    if (onlyName) TextButton({ onMove(target) }, enabled = target.isNotBlank()) { Text(stringResource(R.string.pick_photos)) }
                    else {
                        TextButton({ onCopy(target) }, enabled = target.isNotBlank()) { Text(stringResource(R.string.copy)) }
                        TextButton({ onMove(target) }, enabled = target.isNotBlank()) { Text(stringResource(R.string.move)) }
                    }
                }
            }
        }
    }
}
