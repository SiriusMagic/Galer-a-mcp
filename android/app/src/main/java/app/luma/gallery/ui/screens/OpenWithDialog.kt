package app.luma.gallery.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import app.luma.gallery.R
import app.luma.gallery.data.Opener
import app.luma.gallery.data.Openers

/** «Abrir con…»: Stone first (marked as recommended), then every other app that can open it. */
@Composable fun OpenWithDialog(apps: List<Opener>, onPick: (Opener) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(Modifier.padding(vertical = 20.dp)) {
                Text(stringResource(R.string.open_with), Modifier.padding(horizontal = 24.dp), fontSize = 22.sp)
                if (apps.isEmpty()) Text(stringResource(R.string.open_with_none), Modifier.padding(24.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp).padding(top = 8.dp)) {
                    items(apps, key = { it.packageName + it.activity }) { app ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(app) }.padding(horizontal = 24.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            app.icon?.let { Image(it.toBitmap(96, 96).asImageBitmap(), null, Modifier.size(40.dp)) }
                            Column(Modifier.padding(start = 16.dp)) {
                                Text(app.label, fontSize = 17.sp)
                                if (app.packageName in Openers.OURS) Text(stringResource(R.string.open_with_ours), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            }
                        }
                    }
                }
                TextButton(onDismiss, Modifier.align(Alignment.End).padding(end = 16.dp)) { Text(stringResource(R.string.cancel)) }
            }
        }
    }
}
