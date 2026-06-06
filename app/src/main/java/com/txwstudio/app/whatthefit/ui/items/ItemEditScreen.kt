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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.Tag
import com.txwstudio.app.whatthefit.domain.model.TagKind
import com.txwstudio.app.whatthefit.ui.components.CategoryChips
import com.txwstudio.app.whatthefit.ui.components.SeasonChips
import com.txwstudio.app.whatthefit.ui.components.TagChips
import com.txwstudio.app.whatthefit.ui.theme.WTFTheme

/**
 * Stateful entry point. Owns the [ItemEditViewModel] and forwards its form state to [ItemEditContent].
 * Not previewable: it builds a Hilt ViewModel that reads and writes Room. Preview [ItemEditContent].
 */
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
    val fits by viewModel.fits.collectAsStateWithLifecycle()

    ItemEditContent(
        isEditMode = viewModel.isEditMode,
        name = viewModel.name,
        notes = viewModel.notes,
        canSave = viewModel.canSave,
        categories = categories,
        brands = brands,
        colors = colors,
        occasions = occasions,
        fits = fits,
        selectedCategoryIds = viewModel.selectedCategoryIds,
        selectedTagIds = viewModel.selectedTagIds,
        selectedSeasons = viewModel.selectedSeasons,
        onNameChange = viewModel::onNameChange,
        onNotesChange = viewModel::onNotesChange,
        onToggleCategory = viewModel::toggleCategory,
        onToggleTag = viewModel::toggleTag,
        onToggleSeason = viewModel::toggleSeason,
        onSave = { viewModel.save(onDone) },
        onDelete = { viewModel.delete(onDone) },
        onBack = onDone,
        modifier = modifier,
    )
}

/** Stateless body. Takes plain form state plus event callbacks, so it renders in @Preview without Hilt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditContent(
    isEditMode: Boolean,
    name: String,
    notes: String,
    canSave: Boolean,
    categories: List<Category>,
    brands: List<Tag>,
    colors: List<Tag>,
    occasions: List<Tag>,
    fits: List<Tag>,
    selectedCategoryIds: Set<Long>,
    selectedTagIds: Set<Long>,
    selectedSeasons: Set<Int>,
    onNameChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onToggleCategory: (Long) -> Unit,
    onToggleTag: (Long) -> Unit,
    onToggleSeason: (Int) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditMode) R.string.item_edit_title_edit else R.string.item_edit_title_add,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.action_delete),
                            )
                        }
                    }
                    TextButton(onClick = onSave, enabled = canSave) {
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
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.item_field_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                stringResource(R.string.item_field_parts),
                style = MaterialTheme.typography.titleSmall
            )
            if (categories.isEmpty()) {
                Text(
                    stringResource(R.string.item_no_parts_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                CategoryChips(
                    categories = categories,
                    selectedIds = selectedCategoryIds,
                    onToggle = onToggleCategory,
                )
            }

            if (fits.isNotEmpty()) {
                Text(
                    stringResource(R.string.item_field_fit),
                    style = MaterialTheme.typography.titleSmall
                )
                TagChips(tags = fits, selectedIds = selectedTagIds, onToggle = onToggleTag)
            }

            if (brands.isNotEmpty()) {
                Text(
                    stringResource(R.string.item_field_brand),
                    style = MaterialTheme.typography.titleSmall
                )
                TagChips(tags = brands, selectedIds = selectedTagIds, onToggle = onToggleTag)
            }

            if (colors.isNotEmpty()) {
                Text(
                    stringResource(R.string.item_field_colors),
                    style = MaterialTheme.typography.titleSmall
                )
                TagChips(tags = colors, selectedIds = selectedTagIds, onToggle = onToggleTag)
            }

            if (occasions.isNotEmpty()) {
                Text(
                    stringResource(R.string.item_field_occasions),
                    style = MaterialTheme.typography.titleSmall
                )
                TagChips(tags = occasions, selectedIds = selectedTagIds, onToggle = onToggleTag)
            }

            Text(
                stringResource(R.string.item_field_seasons),
                style = MaterialTheme.typography.titleSmall
            )
            SeasonChips(
                selectedSeasons = selectedSeasons,
                onToggle = onToggleSeason,
            )

            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
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
                    onDelete()
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

private val sampleCategories = listOf(
    Category(id = 1, name = "上衣"),
    Category(id = 2, name = "外套"),
)
private val sampleBrands = listOf(Tag(id = 10, kind = TagKind.BRAND, name = "Uniqlo"))
private val sampleColors = listOf(
    Tag(id = 20, kind = TagKind.COLOR, name = "白", swatchArgb = 0xFFFFFFFFL),
    Tag(id = 21, kind = TagKind.COLOR, name = "黑", swatchArgb = 0xFF000000L),
)
private val sampleOccasions = listOf(Tag(id = 30, kind = TagKind.OCCASION, name = "休閒"))
private val sampleFits = listOf(Tag(id = 40, kind = TagKind.FIT, name = "寬鬆"))

@Preview(name = "Item edit", showBackground = true)
@Composable
private fun ItemEditContentPreview() {
    WTFTheme(dynamicColor = false) {
        ItemEditContent(
            isEditMode = true,
            name = "白色 T-Shirt",
            notes = "",
            canSave = true,
            categories = sampleCategories,
            brands = sampleBrands,
            colors = sampleColors,
            occasions = sampleOccasions,
            fits = sampleFits,
            selectedCategoryIds = setOf(1),
            selectedTagIds = setOf(10, 20),
            selectedSeasons = emptySet(),
            onNameChange = {},
            onNotesChange = {},
            onToggleCategory = {},
            onToggleTag = {},
            onToggleSeason = {},
            onSave = {},
            onDelete = {},
            onBack = {},
        )
    }
}
