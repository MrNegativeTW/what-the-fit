package com.txwstudio.app.whatthefit.ui.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.ui.components.CategoryChips
import com.txwstudio.app.whatthefit.ui.components.SeasonChips
import com.txwstudio.app.whatthefit.ui.components.TagChips

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemEditViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val colors by viewModel.colors.collectAsStateWithLifecycle()
    val occasions by viewModel.occasions.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (viewModel.isEditMode) R.string.item_edit_title_edit else R.string.item_edit_title_add,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (viewModel.isEditMode) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        }
                    }
                    TextButton(onClick = { viewModel.save(onDone) }, enabled = viewModel.canSave) {
                        Text(stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(R.string.item_field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.item_field_parts), style = MaterialTheme.typography.titleSmall)
            if (categories.isEmpty()) {
                Text(
                    stringResource(R.string.item_no_parts_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                CategoryChips(
                    categories = categories,
                    selectedIds = viewModel.selectedCategoryIds,
                    onToggle = viewModel::toggleCategory,
                )
            }

            if (brands.isNotEmpty()) {
                Text(stringResource(R.string.item_field_brand), style = MaterialTheme.typography.titleSmall)
                TagChips(tags = brands, selectedIds = viewModel.selectedTagIds, onToggle = viewModel::toggleTag)
            }

            if (colors.isNotEmpty()) {
                Text(stringResource(R.string.item_field_colors), style = MaterialTheme.typography.titleSmall)
                TagChips(tags = colors, selectedIds = viewModel.selectedTagIds, onToggle = viewModel::toggleTag)
            }

            if (occasions.isNotEmpty()) {
                Text(stringResource(R.string.item_field_occasions), style = MaterialTheme.typography.titleSmall)
                TagChips(tags = occasions, selectedIds = viewModel.selectedTagIds, onToggle = viewModel::toggleTag)
            }

            Text(stringResource(R.string.item_field_seasons), style = MaterialTheme.typography.titleSmall)
            SeasonChips(
                selectedSeasons = viewModel.selectedSeasons,
                onToggle = viewModel::toggleSeason,
            )

            OutlinedTextField(
                value = viewModel.notes,
                onValueChange = viewModel::onNotesChange,
                label = { Text(stringResource(R.string.item_field_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.item_delete_confirm_title)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(onDone)
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}
