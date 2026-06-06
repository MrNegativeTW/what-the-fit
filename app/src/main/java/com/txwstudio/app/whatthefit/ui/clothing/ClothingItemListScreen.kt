package com.txwstudio.app.whatthefit.ui.clothing

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.txwstudio.app.whatthefit.R
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ClothingItem
import com.txwstudio.app.whatthefit.data.entity.ItemWithDetails
import com.txwstudio.app.whatthefit.data.entity.Tag
import com.txwstudio.app.whatthefit.domain.model.TagKind
import com.txwstudio.app.whatthefit.ui.components.ClothingItemRow
import com.txwstudio.app.whatthefit.ui.components.ColorSwatch
import com.txwstudio.app.whatthefit.ui.components.FabListBottomPadding
import com.txwstudio.app.whatthefit.ui.theme.WTFTheme
import kotlinx.coroutines.flow.flowOf

/**
 * Stateful entry point. Owns the [ClothingItemListViewModel] and forwards its paged list and filter
 * state to [ClothingItemListContent]. Not previewable: it builds a Hilt ViewModel that reads Room.
 * Preview [ClothingItemListContent] with a fake [PagingData].
 */
@Composable
fun ClothingItemListScreen(
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClothingItemListViewModel = hiltViewModel(),
) {
    val items = viewModel.items.collectAsLazyPagingItems()
    val filtering by viewModel.isFiltering.collectAsStateWithLifecycle()

    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val colors by viewModel.colors.collectAsStateWithLifecycle()
    val occasions by viewModel.occasions.collectAsStateWithLifecycle()
    val fits by viewModel.fits.collectAsStateWithLifecycle()
    val selectedCategoryIds by viewModel.selectedCategoryIds.collectAsStateWithLifecycle()
    val selectedBrandIds by viewModel.selectedBrandIds.collectAsStateWithLifecycle()
    val selectedColorIds by viewModel.selectedColorIds.collectAsStateWithLifecycle()
    val selectedOccasionIds by viewModel.selectedOccasionIds.collectAsStateWithLifecycle()
    val selectedFitIds by viewModel.selectedFitIds.collectAsStateWithLifecycle()

    ClothingItemListContent(
        items = items,
        filtering = filtering,
        categories = categories,
        brands = brands,
        colors = colors,
        occasions = occasions,
        fits = fits,
        selectedCategoryIds = selectedCategoryIds,
        selectedBrandIds = selectedBrandIds,
        selectedColorIds = selectedColorIds,
        selectedOccasionIds = selectedOccasionIds,
        selectedFitIds = selectedFitIds,
        onToggleCategory = viewModel::toggleCategory,
        onToggleBrand = viewModel::toggleBrand,
        onToggleColor = viewModel::toggleColor,
        onToggleOccasion = viewModel::toggleOccasion,
        onToggleFit = viewModel::toggleFit,
        onAddItem = onAddItem,
        onEditItem = onEditItem,
        onToggleAvailable = viewModel::setAvailability,
        modifier = modifier,
    )
}

/** Stateless body. Takes the paged list plus plain filter state, so it renders in @Preview. */
@Composable
fun ClothingItemListContent(
    items: LazyPagingItems<ItemWithDetails>,
    filtering: Boolean,
    categories: List<Category>,
    brands: List<Tag>,
    colors: List<Tag>,
    occasions: List<Tag>,
    fits: List<Tag>,
    selectedCategoryIds: Set<Long>,
    selectedBrandIds: Set<Long>,
    selectedColorIds: Set<Long>,
    selectedOccasionIds: Set<Long>,
    selectedFitIds: Set<Long>,
    onToggleCategory: (Long) -> Unit,
    onToggleBrand: (Long) -> Unit,
    onToggleColor: (Long) -> Unit,
    onToggleOccasion: (Long) -> Unit,
    onToggleFit: (Long) -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    onToggleAvailable: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEmpty = items.itemCount == 0 && items.loadState.refresh is LoadState.NotLoading

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
                        options = categories.map {
                            FilterOption(
                                it.id,
                                it.name,
                                swatchArgb = null
                            )
                        },
                        selectedIds = selectedCategoryIds,
                        onToggle = onToggleCategory,
                    )
                }
                if (fits.isNotEmpty()) {
                    FilterChipMenu(
                        labelRes = R.string.item_field_fit,
                        options = fits.map { FilterOption(it.id, it.name, it.swatchArgb) },
                        selectedIds = selectedFitIds,
                        onToggle = onToggleFit,
                    )
                }
                if (brands.isNotEmpty()) {
                    FilterChipMenu(
                        labelRes = R.string.item_field_brand,
                        options = brands.map { FilterOption(it.id, it.name, it.swatchArgb) },
                        selectedIds = selectedBrandIds,
                        onToggle = onToggleBrand,
                    )
                }
                if (colors.isNotEmpty()) {
                    FilterChipMenu(
                        labelRes = R.string.item_field_colors,
                        options = colors.map { FilterOption(it.id, it.name, it.swatchArgb) },
                        selectedIds = selectedColorIds,
                        onToggle = onToggleColor,
                    )
                }
                if (occasions.isNotEmpty()) {
                    FilterChipMenu(
                        labelRes = R.string.item_field_occasions,
                        options = occasions.map { FilterOption(it.id, it.name, it.swatchArgb) },
                        selectedIds = selectedOccasionIds,
                        onToggle = onToggleOccasion,
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
                            ClothingItemRow(
                                item = item,
                                onClick = { onEditItem(item.item.id) },
                                trailing = {
                                    Switch(
                                        checked = item.item.isAvailable,
                                        onCheckedChange = { onToggleAvailable(item.item.id, it) },
                                    )
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = onAddItem,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
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

private val sampleItems = listOf(
    ItemWithDetails(
        item = ClothingItem(id = 1, name = "白色 T-Shirt"),
        categories = listOf(Category(id = 1, name = "上衣")),
        tags = listOf(
            Tag(id = 10, kind = TagKind.BRAND, name = "Uniqlo"),
            Tag(id = 20, kind = TagKind.COLOR, name = "白", swatchArgb = 0xFFFFFFFFL),
        ),
    ),
    ItemWithDetails(
        item = ClothingItem(id = 2, name = "牛仔外套", isAvailable = false),
        categories = listOf(Category(id = 2, name = "外套")),
        tags = listOf(
            Tag(id = 11, kind = TagKind.BRAND, name = "Levi's"),
            Tag(id = 21, kind = TagKind.COLOR, name = "藍", swatchArgb = 0xFF1E88E5L),
        ),
    ),
)

private val sampleCategories =
    listOf(Category(id = 1, name = "上衣"), Category(id = 2, name = "外套"))
private val sampleBrands = listOf(Tag(id = 10, kind = TagKind.BRAND, name = "Uniqlo"))
private val sampleColors =
    listOf(Tag(id = 20, kind = TagKind.COLOR, name = "白", swatchArgb = 0xFFFFFFFFL))

@Preview(name = "Item list", showBackground = true)
@Composable
private fun ClothingItemListContentPreview() {
    val items = flowOf(PagingData.from(sampleItems)).collectAsLazyPagingItems()
    WTFTheme(dynamicColor = false) {
        ClothingItemListContent(
            items = items,
            filtering = false,
            categories = sampleCategories,
            brands = sampleBrands,
            colors = sampleColors,
            occasions = emptyList(),
            fits = emptyList(),
            selectedCategoryIds = emptySet(),
            selectedBrandIds = emptySet(),
            selectedColorIds = emptySet(),
            selectedOccasionIds = emptySet(),
            selectedFitIds = emptySet(),
            onToggleCategory = {},
            onToggleBrand = {},
            onToggleColor = {},
            onToggleOccasion = {},
            onToggleFit = {},
            onAddItem = {},
            onEditItem = {},
            onToggleAvailable = { _, _ -> },
        )
    }
}
