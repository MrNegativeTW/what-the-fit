package com.txwstudio.app.whatthefit.ui.ootd

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.ui.theme.WTFTheme
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** M3 date pickers work in UTC millis; OOTD dates are stored as `epochDay`. */
private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Stateful entry point. Owns the [OotdEditViewModel] and the photo-picker / camera launchers, then
 * forwards plain state to [OotdEditContent]. Not previewable: it builds a Hilt ViewModel and binds
 * Activity-result launchers. Preview [OotdEditContent].
 */
@Composable
fun OotdEditScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OotdEditViewModel = hiltViewModel(),
) {
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> viewModel.onPhotoPicked(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success -> viewModel.onPhotoCaptured(success) }

    OotdEditContent(
        isEditMode = viewModel.isEditMode,
        date = viewModel.date,
        categories = categories,
        itemsByCategory = viewModel.itemsByCategory,
        selectedItems = viewModel.selectedItems,
        notes = viewModel.notes,
        photoPath = viewModel.photoPath,
        onDateChange = viewModel::onDateChange,
        onSelectItem = viewModel::selectItem,
        onClearItem = viewModel::clearItem,
        onNotesChange = viewModel::onNotesChange,
        onPickGallery = {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        onCapturePhoto = { cameraLauncher.launch(viewModel.prepareCameraOutput()) },
        onRemovePhoto = viewModel::removePhoto,
        onSave = { viewModel.save(onDone) },
        onDelete = { viewModel.delete(onDone) },
        onBack = onDone,
        modifier = modifier,
    )
}

/** Stateless body. Plain state plus event callbacks, so it renders in @Preview without Hilt. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OotdEditContent(
    isEditMode: Boolean,
    date: LocalDate,
    categories: List<Category>,
    itemsByCategory: Map<Long, List<ClothingItem>>,
    selectedItems: Map<Long, Long>,
    notes: String,
    photoPath: String?,
    onDateChange: (Long) -> Unit,
    onSelectItem: (Long, Long) -> Unit,
    onClearItem: (Long) -> Unit,
    onNotesChange: (String) -> Unit,
    onPickGallery: () -> Unit,
    onCapturePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val canSave = selectedItems.isNotEmpty()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (isEditMode) R.string.ootd_edit_title_edit else R.string.ootd_edit_title_add,
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
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
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
            // Date
            Text(stringResource(R.string.ootd_field_date), style = MaterialTheme.typography.titleSmall)
            val dateLabel = remember(date) {
                date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
            }
            OutlinedButton(onClick = { showDatePicker = true }) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(dateLabel)
            }

            // Photo
            Text(stringResource(R.string.ootd_field_photo), style = MaterialTheme.typography.titleSmall)
            if (photoPath != null) {
                AsyncImage(
                    model = File(photoPath),
                    contentDescription = stringResource(R.string.ootd_field_photo),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.medium),
                )
                TextButton(onClick = onRemovePhoto) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ootd_photo_remove))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onPickGallery) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ootd_photo_gallery))
                    }
                    OutlinedButton(onClick = onCapturePhoto) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.ootd_photo_camera))
                    }
                }
            }

            // Items per part
            Text(stringResource(R.string.ootd_field_items), style = MaterialTheme.typography.titleSmall)
            if (categories.isEmpty()) {
                Text(
                    stringResource(R.string.item_no_parts_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                categories.forEach { category ->
                    PartItemSelector(
                        category = category,
                        items = itemsByCategory[category.id].orEmpty(),
                        selectedItemId = selectedItems[category.id],
                        onSelect = { itemId -> onSelectItem(category.id, itemId) },
                        onClear = { onClearItem(category.id) },
                    )
                }
            }

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text(stringResource(R.string.ootd_field_notes)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = date.toEpochDay() * MILLIS_PER_DAY)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateChange(it / MILLIS_PER_DAY) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.ootd_delete_confirm_title)) },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

/** One part row with a dropdown to pick (or clear) the item worn for that category. */
@Composable
private fun PartItemSelector(
    category: Category,
    items: List<ClothingItem>,
    selectedItemId: Long?,
    onSelect: (Long) -> Unit,
    onClear: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = items.firstOrNull { it.id == selectedItemId }?.name

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(category.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(72.dp))
        Box(Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = items.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = selectedName ?: stringResource(R.string.ootd_pick_item),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.name) },
                        onClick = { onSelect(item.id); expanded = false },
                        trailingIcon = if (item.id == selectedItemId) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else {
                            null
                        },
                    )
                }
                if (selectedItemId != null) {
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.ootd_item_clear)) },
                        onClick = { onClear(); expanded = false },
                    )
                }
            }
        }
    }
}

private val sampleCategories = listOf(
    Category(id = 1, name = "上衣"),
    Category(id = 2, name = "外套"),
    Category(id = 3, name = "褲子"),
)
private val sampleItemsByCategory = mapOf(
    1L to listOf(ClothingItem(id = 10, name = "白色 T-Shirt"), ClothingItem(id = 11, name = "條紋上衣")),
    2L to listOf(ClothingItem(id = 12, name = "牛仔外套")),
    3L to listOf(ClothingItem(id = 13, name = "黑色長褲")),
)

@Preview(name = "OOTD edit", showBackground = true)
@Composable
private fun OotdEditContentPreview() {
    WTFTheme(dynamicColor = false) {
        OotdEditContent(
            isEditMode = true,
            date = LocalDate.ofEpochDay(20_000),
            categories = sampleCategories,
            itemsByCategory = sampleItemsByCategory,
            selectedItems = mapOf(1L to 10L, 3L to 13L),
            notes = "Comfy fit",
            photoPath = null,
            onDateChange = {},
            onSelectItem = { _, _ -> },
            onClearItem = {},
            onNotesChange = {},
            onPickGallery = {},
            onCapturePhoto = {},
            onRemovePhoto = {},
            onSave = {},
            onDelete = {},
            onBack = {},
        )
    }
}
