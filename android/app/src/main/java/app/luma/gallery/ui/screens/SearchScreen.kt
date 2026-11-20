package app.luma.gallery.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.luma.gallery.R
import app.luma.gallery.data.Media

/** Search by name or album (every word must appear). Searching by what the photo shows comes with the MCP phase. */
@Composable fun SearchScreen(query: String, onQuery: (String) -> Unit, results: List<Media>, favorites: Set<String>,
    onOpen: (Media) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize()) {
        OutlinedTextField(query, onQuery, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), singleLine = true,
            shape = CircleShape, leadingIcon = { Icon(Icons.Default.Search, null) },
            placeholder = { Text(stringResource(R.string.search_hint)) })
        MediaGrid(results, favorites, stringResource(if (query.isBlank()) R.string.search_empty else R.string.search_none), onOpen)
    }
}
