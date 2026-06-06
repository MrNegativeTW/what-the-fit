package com.txwstudio.app.whatthefit.ui.result

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumFloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.domain.model.OutfitSlot
import com.txwstudio.app.whatthefit.ui.components.FabListBottomPadding
import com.txwstudio.app.whatthefit.ui.theme.WTFTheme

/**
 * Stateful entry point. Owns the [ResultViewModel] and forwards its state to [ResultContent]. Not
 * previewable: it builds a Hilt ViewModel that runs the outfit generator. Preview [ResultContent].
 */
@Composable
fun ResultScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ResultViewModel = hiltViewModel(),
) {
    val slots by viewModel.slots.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val savedMessage = stringResource(R.string.ootd_saved)
    LaunchedEffect(Unit) {
        viewModel.saved.collect { snackbarHostState.showSnackbar(savedMessage) }
    }
    ResultContent(
        slots = slots,
        onBack = onBack,
        onRerollAll = viewModel::rerollAll,
        onRerollSingle = viewModel::rerollSingle,
        onSaveOotd = viewModel::saveAsOotd,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

/** Stateless body. Takes plain state plus event callbacks, so it renders in @Preview without Hilt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultContent(
    slots: List<OutfitSlot>,
    onBack: () -> Unit,
    onRerollAll: () -> Unit,
    onRerollSingle: (Long) -> Unit,
    onSaveOotd: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var showExitConfirm by remember { mutableStateOf(false) }
    var showRerollConfirm by remember { mutableStateOf(false) }

    BackHandler { showExitConfirm = true }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = { showExitConfirm = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSaveOotd) {
                        Icon(
                            Icons.Default.BookmarkAdd,
                            contentDescription = stringResource(R.string.ootd_save_as),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            MediumFloatingActionButton(onClick = { showRerollConfirm = true }) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = stringResource(R.string.result_reroll_all),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            // Same guard as the wardrobe lists: keep the last row clear of the floating FAB.
            contentPadding = PaddingValues(bottom = FabListBottomPadding),
        ) {
            items(slots, key = { it.category.id }) { slot ->
                ResultRow(slot = slot, onReroll = { onRerollSingle(slot.category.id) })
                HorizontalDivider()
            }
        }
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text(stringResource(R.string.dialog_exit_title)) },
            confirmButton = {
                TextButton(onClick = { showExitConfirm = false; onBack() }) {
                    Text(stringResource(R.string.action_leave))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showRerollConfirm) {
        AlertDialog(
            onDismissRequest = { showRerollConfirm = false },
            title = { Text(stringResource(R.string.result_reroll_confirm_title)) },
            confirmButton = {
                TextButton(onClick = { showRerollConfirm = false; onRerollAll() }) {
                    Text(stringResource(R.string.result_reroll_all))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRerollConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
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
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        IconButton(onClick = onReroll) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = stringResource(R.string.result_reroll_one, slot.category.name),
            )
        }
    }
}

private val sampleSlots = listOf(
    OutfitSlot(Category(id = 1, name = "上衣"), ClothingItem(id = 10, name = "白色 T-Shirt")),
    OutfitSlot(Category(id = 2, name = "外套"), ClothingItem(id = 11, name = "牛仔外套")),
    OutfitSlot(Category(id = 3, name = "褲子"), ClothingItem(id = 12, name = "黑色長褲")),
    // A null item renders the "no available item" row.
    OutfitSlot(Category(id = 4, name = "鞋子"), item = null),
)

@Preview(name = "Result", showBackground = true)
@Composable
private fun ResultContentPreview() {
    WTFTheme(dynamicColor = false) {
        ResultContent(
            slots = sampleSlots,
            onBack = {},
            onRerollAll = {},
            onRerollSingle = {},
            onSaveOotd = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
