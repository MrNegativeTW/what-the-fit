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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
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
import com.txwstudio.app.whatthefit.data.entity.ItemWithDetails
import com.txwstudio.app.whatthefit.data.entity.Tag
import com.txwstudio.app.whatthefit.domain.model.TagKind
import com.txwstudio.app.whatthefit.ui.components.ColorSwatch
import com.txwstudio.app.whatthefit.ui.components.FabListBottomPadding
import com.txwstudio.app.whatthefit.ui.theme.WTFTheme

/**
 * Stateful entry point. Owns the [ClothingItemListViewModel]; collects the grouped [UiSection]s and
 * filter state, holds per-section expand state, and forwards them to [ClothingItemListContent].
 */
@Composable
fun ClothingItemListScreen(
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ClothingItemListViewModel = hiltViewModel(),
) {
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val filtering by viewModel.isFiltering.collectAsStateWithLifecycle()

    val brands by viewModel.brands.collectAsStateWithLifecycle()
    val colors by viewModel.colors.collectAsStateWithLifecycle()
    val occasions by viewModel.occasions.collectAsStateWithLifecycle()
    val fits by viewModel.fits.collectAsStateWithLifecycle()
    val selectedBrandIds by viewModel.selectedBrandIds.collectAsStateWithLifecycle()
    val selectedColorIds by viewModel.selectedColorIds.collectAsStateWithLifecycle()
    val selectedOccasionIds by viewModel.selectedOccasionIds.collectAsStateWithLifecycle()
    val selectedFitIds by viewModel.selectedFitIds.collectAsStateWithLifecycle()

    // Which sections the user has opened (browse mode). Survives rotation / process death.
    val expandedKeys = rememberSaveable(
        saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() }),
    ) { mutableStateListOf<Long>() }
    // Hoisted so the scroll position is restored when returning from add/edit (same pattern as the
    // Parts and tag sub-tabs). Survives navigation, config change, and process death.
    val listState = rememberLazyListState()

    ClothingItemListContent(
        sections = sections,
        filtering = filtering,
        listState = listState,
        expandedKeys = expandedKeys,
        onToggleExpand = { key ->
            if (key in expandedKeys) expandedKeys.remove(key) else expandedKeys.add(key)
        },
        brands = brands,
        colors = colors,
        occasions = occasions,
        fits = fits,
        selectedBrandIds = selectedBrandIds,
        selectedColorIds = selectedColorIds,
        selectedOccasionIds = selectedOccasionIds,
        selectedFitIds = selectedFitIds,
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

/** Stateless body. Takes the grouped sections plus plain filter state, so it renders in @Preview. */
@Composable
fun ClothingItemListContent(
    sections: List<UiSection>,
    filtering: Boolean,
    listState: LazyListState,
    expandedKeys: List<Long>,
    onToggleExpand: (Long) -> Unit,
    brands: List<Tag>,
    colors: List<Tag>,
    occasions: List<Tag>,
    fits: List<Tag>,
    selectedBrandIds: Set<Long>,
    selectedColorIds: Set<Long>,
    selectedOccasionIds: Set<Long>,
    selectedFitIds: Set<Long>,
    onToggleBrand: (Long) -> Unit,
    onToggleColor: (Long) -> Unit,
    onToggleOccasion: (Long) -> Unit,
    onToggleFit: (Long) -> Unit,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    onToggleAvailable: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // One row of dimension chips; each opens a multi-select dropdown of its labels.
            // Parts is not here — it groups the list below.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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

            if (sections.isEmpty()) {
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
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = FabListBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(sections, key = { it.key }) { section ->
                        ClothingSection(
                            title = section.name ?: stringResource(R.string.item_uncategorized),
                            count = section.count,
                            expanded = filtering || section.key in expandedKeys,
                            items = section.items,
                            onToggleExpanded = { onToggleExpand(section.key) },
                            onItemClick = onEditItem,
                            onToggleAvailable = onToggleAvailable,
                        )
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

private val sampleSections = listOf(
    UiSection(
        key = 1,
        name = "上衣",
        items = listOf(
            ItemWithDetails(
                item = ClothingItem(id = 1, name = "白色 T-Shirt"),
                categories = listOf(Category(id = 1, name = "上衣")),
                tags = listOf(
                    Tag(id = 10, kind = TagKind.BRAND, name = "Uniqlo"),
                    Tag(id = 20, kind = TagKind.COLOR, name = "白", swatchArgb = 0xFFFFFFFFL),
                ),
            ),
        ),
    ),
    UiSection(
        key = 2,
        name = "外套",
        items = listOf(
            ItemWithDetails(
                item = ClothingItem(id = 2, name = "牛仔外套", isAvailable = false),
                categories = listOf(Category(id = 2, name = "外套")),
                tags = listOf(Tag(id = 11, kind = TagKind.BRAND, name = "Levi's")),
            ),
        ),
    ),
)

private val sampleBrands = listOf(Tag(id = 10, kind = TagKind.BRAND, name = "Uniqlo"))
private val sampleColors =
    listOf(Tag(id = 20, kind = TagKind.COLOR, name = "白", swatchArgb = 0xFFFFFFFFL))

@Preview(name = "Clothes sections", showBackground = true)
@Composable
private fun ClothingItemListContentPreview() {
    WTFTheme(dynamicColor = false) {
        ClothingItemListContent(
            sections = sampleSections,
            filtering = false,
            listState = rememberLazyListState(),
            expandedKeys = listOf(1L),
            onToggleExpand = {},
            brands = sampleBrands,
            colors = sampleColors,
            occasions = emptyList(),
            fits = emptyList(),
            selectedBrandIds = emptySet(),
            selectedColorIds = emptySet(),
            selectedOccasionIds = emptySet(),
            selectedFitIds = emptySet(),
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
