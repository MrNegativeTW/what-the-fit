package com.txwstudio.app.whatthefit.ui.result

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.domain.model.OutfitSlot

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val slots by viewModel.slots.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = viewModel::rerollAll,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text(stringResource(R.string.result_reroll_all))
            }
        },
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            items(slots, key = { it.category.id }) { slot ->
                ResultRow(slot = slot, onReroll = { viewModel.rerollSingle(slot.category.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun ResultRow(
    slot: OutfitSlot,
    onReroll: () -> Unit,
) {
    val hasItem = slot.item != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = slot.category.name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(72.dp),
        )
        Text(
            text = slot.item?.name ?: stringResource(R.string.result_no_item),
            style = MaterialTheme.typography.bodyLarge,
            color = if (hasItem) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
        )
        IconButton(onClick = onReroll) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.result_reroll_one, slot.category.name),
            )
        }
    }
}
