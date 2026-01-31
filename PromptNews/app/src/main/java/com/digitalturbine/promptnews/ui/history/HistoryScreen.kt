package com.digitalturbine.promptnews.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalturbine.promptnews.data.history.HistoryEntry
import com.digitalturbine.promptnews.data.history.HistoryEntryType
import com.digitalturbine.promptnews.data.history.HistoryRepository
import com.digitalturbine.promptnews.util.TimeLabelFormatter

@Composable
fun HistoryScreen(
    onEntrySelected: (HistoryEntry) -> Unit,
    vm: HistoryViewModel = run {
        val context = LocalContext.current
        val repository = remember(context) { HistoryRepository.getInstance(context) }
        viewModel(factory = HistoryViewModelFactory(repository))
    }
) {
    val entries by vm.entries.collectAsState()

    LaunchedEffect(Unit) {
        vm.pruneOldEntries()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Your recent searches and article visits will appear here.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(entries, key = { it.id }) { entry ->
                val title = when (entry.type) {
                    HistoryEntryType.QUERY -> entry.query.orEmpty()
                    HistoryEntryType.ARTICLE_CLICK -> entry.title ?: entry.url.orEmpty()
                }.ifBlank { "Untitled" }
                val icon = when (entry.type) {
                    HistoryEntryType.QUERY -> Icons.Filled.Search
                    HistoryEntryType.ARTICLE_CLICK -> Icons.Filled.OpenInNew
                }
                ListItem(
                    leadingContent = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text(title) },
                    supportingContent = { Text(TimeLabelFormatter.formatTimeLabel(entry.timestampMs)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEntrySelected(entry) }
                )
            }
        }
    }
}
