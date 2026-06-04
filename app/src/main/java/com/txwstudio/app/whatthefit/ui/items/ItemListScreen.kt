package com.txwstudio.app.whatthefit.ui.items

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.entity.ItemWithDetails
import com.txwstudio.app.whatthefit.domain.model.TagKind
import com.txwstudio.app.whatthefit.ui.components.ColorSwatch
import com.txwstudio.app.whatthefit.ui.components.FabListBottomPadding

@Composable
fun ItemListScreen(
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ItemListViewModel = hiltViewModel(),
) {
    val items = viewModel.items.collectAsLazyPagingItems()
    val isEmpty = items.itemCount == 0 && items.loadState.refresh is LoadState.NotLoading
    val filtering by viewModel.isFiltering.collectAsStateWithLifecycle()

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val colors by viewModel.colors.collectAsStateWithLifecycle()
    val occasions by viewModel.occasions.collectAsStateWithLifecycle()
    val selectedCategoryIds by viewModel.selectedCategoryIds.collectAsStateWithLifecycle()
    val selectedBrandIds by viewModel.selectedBrandIds.collectAsStateWithLifecycle()
    val selectedColorIds by viewModel.selectedColorIds.collectAsStateWithLifecycle()
    val selectedOccasionIds by viewModel.selectedOccasionIds.collectAsStateWithLifecycle()

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // One row of dimension chips; each opens a multi-select dropdown of its labels.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (categories.isNotEmpty()) {
                    FilterChipMenu(
                        labelRes = R.string.item_field_parts,
                        options = categories.map { FilterOption(it.id, it.name, swatchArgb = null) },
                        selectedIds = selectedCategoryIds,
                        onToggle = viewModel::toggleCategory,
                    )
                }
                if (brands.isNotEmpty()) {
                    FilterChipMenu(
                        labelRes = R.string.item_field_brand,
                        options = brands.map { FilterOption(it.id, it.name, it.swatchArgb) },
                        selectedIds = selectedBrandIds,
                        onToggle = viewModel::toggleBrand,
                    )
                }
                if (colors.isNotEmpty()) {
                    FilterChipMenu(
                        labelRes = R.string.item_field_colors,
                        options = colors.map { FilterOption(it.id, it.name, it.swatchArgb) },
                        selectedIds = selectedColorIds,
                        onToggle = viewModel::toggleColor,
                    )
                }
                if (occasions.isNotEmpty()) {
                    FilterChipMenu(
                        labelRes = R.string.item_field_occasions,
                        options = occasions.map { FilterOption(it.id, it.name, it.swatchArgb) },
                        selectedIds = selectedOccasionIds,
                        onToggle = viewModel::toggleOccasion,
                    )
                }
            }

            if (isEmpty) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(
                            if (filtering) R.string.item_list_no_match else R.string.item_list_empty,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = FabListBottomPadding),
                ) {
                    items(items.itemCount) { index ->
                        items[index]?.let { item ->
                            ItemRow(
                                item = item,
                                onClick = { onEditItem(item.item.id) },
                                onToggleAvailable = { viewModel.setAvailability(item.item.id, it) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAddItem,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.item_add))
        }
    }
}

private data class FilterOption(val id: Long, val name: String, val swatchArgb: Long?)

/**
 * One filter dimension as a single chip that opens a multi-select dropdown of its options.
 * The chip shows the dimension name plus a (count) and an active highlight when anything is selected.
 */
@Composable
private fun FilterChipMenu(
    @StringRes labelRes: Int,
    options: List<FilterOption>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    if (options.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val label = stringResource(labelRes)
    val count = selectedIds.size

    Box {
        FilterChip(
            selected = count > 0,
            onClick = { expanded = true },
            label = { Text(if (count > 0) "$label ($count)" else label) },
            trailingIcon = {
                Icon(
                    imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                )
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                val checked = option.id in selectedIds
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = { onToggle(option.id) }, // multi-select: keep the menu open
                    leadingIcon = option.swatchArgb?.let { argb -> { ColorSwatch(argb = argb) } },
                    trailingIcon = if (checked) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun ItemRow(
    item: ItemWithDetails,
    onClick: () -> Unit,
    onToggleAvailable: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.item.name, style = MaterialTheme.typography.bodyLarge)
                val swatches = item.tags.mapNotNull { tag ->
                    tag.swatchArgb.takeIf { tag.kind == TagKind.COLOR }
                }
                if (swatches.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        swatches.forEach { ColorSwatch(argb = it, size = 12.dp) }
                    }
                }
            }
            val categoryText = if (item.categories.isEmpty()) {
                stringResource(R.string.item_uncategorized)
            } else {
                item.categories.joinToString("、") { it.name }
            }
            val brandText = item.tags.filter { it.kind == TagKind.BRAND }.joinToString(" / ") { it.name }
            Text(
                text = if (brandText.isNotBlank()) "$brandText · $categoryText" else categoryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = item.item.isAvailable, onCheckedChange = onToggleAvailable)
    }
}
