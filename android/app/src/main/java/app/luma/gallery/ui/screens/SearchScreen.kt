package app.luma.gallery.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.luma.gallery.R
import app.luma.gallery.ui.IndexStatus

/** Search box, then suggestions (what appears most in the photos) or, before the analysis, how it works. */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun SearchBox(query: String, onQuery: (String) -> Unit, suggestions: List<String>, status: IndexStatus, showHelp: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), singleLine = true, shape = CircleShape,
            leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text(stringResource(R.string.search_hint)) },
            // The broom: clears the search in one tap (only while something is written).
            trailingIcon = if (query.isEmpty()) null else {
                { IconButton({ onQuery("") }) { Icon(Icons.Outlined.CleaningServices, stringResource(R.string.clear_search)) } }
            })
        if (showHelp) {
            if (suggestions.isNotEmpty()) FlowRow(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { SuggestionChip({ onQuery(it) }, { Text(it) }) }
            }
            Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.AutoAwesome, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.index_title), Modifier.padding(top = 12.dp), fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.index_hint), Modifier.padding(top = 4.dp), textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                if (status.total > 0) Text(pluralStringResource(R.plurals.index_progress, status.analyzed, status.analyzed, status.total),
                    Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
