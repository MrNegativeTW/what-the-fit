package com.txwstudio.app.whatthefit.ui.clothing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.ItemWithDetails
import com.txwstudio.app.whatthefit.data.entity.Tag
import com.txwstudio.app.whatthefit.data.repository.WardrobeRepository
import com.txwstudio.app.whatthefit.domain.model.TagKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Sentinel section key for the Uncategorized bucket (items with no part). */
const val UNCATEGORIZED_KEY = -1L

/** One part section in the grouped Clothes list. [name] is null for the Uncategorized bucket. */
data class UiSection(
    val key: Long,
    val name: String?,
    val items: List<ItemWithDetails>,
) {
    val count: Int get() = items.size
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ClothingItemListViewModel @Inject constructor(
    private val repository: WardrobeRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _selectedBrandIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _selectedColorIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _selectedOccasionIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _selectedFitIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedBrandIds = _selectedBrandIds.asStateFlow()
    val selectedColorIds = _selectedColorIds.asStateFlow()
    val selectedOccasionIds = _selectedOccasionIds.asStateFlow()
    val selectedFitIds = _selectedFitIds.asStateFlow()

    // Label sources for the filter chip row. Parts is no longer a filter — it groups the list.
    val brands: StateFlow<List<Tag>> = repository.observeTags(TagKind.BRAND)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val colors: StateFlow<List<Tag>> = repository.observeTags(TagKind.COLOR)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val occasions: StateFlow<List<Tag>> = repository.observeTags(TagKind.OCCASION)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val fits: StateFlow<List<Tag>> = repository.observeTags(TagKind.FIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // The four tag dimensions folded into one flow. sorted() keeps each key stable so
    // distinctUntilChanged() downstream skips no-op rebuilds.
    private val filters: Flow<Filters> = combine(
        _selectedBrandIds,
        _selectedColorIds,
        _selectedOccasionIds,
        _selectedFitIds,
    ) { brandIds, colorIds, occIds, fitIds ->
        Filters(brandIds.sorted(), colorIds.sorted(), occIds.sorted(), fitIds.sorted())
    }

    /** True when a text query or any filter is active — drives auto-expand and the empty message. */
    val isFiltering: StateFlow<Boolean> = combine(_query, filters) { q, f ->
        q.isNotBlank() || f.brandIds.isNotEmpty() || f.colorIds.isNotEmpty() ||
                f.occasionIds.isNotEmpty() || f.fitIds.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * The grouped list. Each item appears under every part it belongs to (intended). Browse mode
     * (nothing active) shows every part incl. empty ones; filter mode keeps only sections with
     * matches. Uncategorized is always last and shown only when it has items.
     */
    val sections: StateFlow<List<UiSection>> = combine(
        _query.debounce(300), // only the text is debounced; chip filters apply immediately
        filters,
    ) { q, f -> Criteria(q.trim(), f) }
        .distinctUntilChanged()
        .flatMapLatest { c ->
            val filtering = c.query.isNotBlank() || c.filters.brandIds.isNotEmpty() ||
                    c.filters.colorIds.isNotEmpty() || c.filters.occasionIds.isNotEmpty() ||
                    c.filters.fitIds.isNotEmpty()
            combine(
                repository.observeMatchingItems(
                    c.query,
                    c.filters.brandIds,
                    c.filters.colorIds,
                    c.filters.occasionIds,
                    c.filters.fitIds,
                ),
                repository.observeCategories(),
            ) { items, categories -> buildSections(items, categories, filtering) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun toggleBrand(id: Long) = _selectedBrandIds.update { it.toggled(id) }
    fun toggleColor(id: Long) = _selectedColorIds.update { it.toggled(id) }
    fun toggleOccasion(id: Long) = _selectedOccasionIds.update { it.toggled(id) }
    fun toggleFit(id: Long) = _selectedFitIds.update { it.toggled(id) }

    fun setAvailability(id: Long, available: Boolean) {
        viewModelScope.launch { repository.setAvailability(id, available) }
    }

    private data class Filters(
        val brandIds: List<Long>,
        val colorIds: List<Long>,
        val occasionIds: List<Long>,
        val fitIds: List<Long>,
    )

    private data class Criteria(val query: String, val filters: Filters)
}

/**
 * Buckets [items] under each category they belong to (an item in several parts appears in each),
 * plus an Uncategorized bucket for items with no part. Sections follow [categories] order
 * (sortOrder); Uncategorized is last. When [filtering] is false every category is shown (even
 * empty); when true only non-empty sections survive. Uncategorized appears only when non-empty.
 */
internal fun buildSections(
    items: List<ItemWithDetails>,
    categories: List<Category>,
    filtering: Boolean,
): List<UiSection> {
    val byCategory: Map<Long, List<ItemWithDetails>> =
        items.flatMap { item -> item.categories.map { it.id to item } }
            .groupBy({ it.first }, { it.second })
    val uncategorized = items.filter { it.categories.isEmpty() }

    val parts = categories.mapNotNull { category ->
        val sectionItems = byCategory[category.id].orEmpty()
        if (filtering && sectionItems.isEmpty()) {
            null
        } else {
            UiSection(key = category.id, name = category.name, items = sectionItems)
        }
    }
    return if (uncategorized.isEmpty()) {
        parts
    } else {
        parts + UiSection(key = UNCATEGORIZED_KEY, name = null, items = uncategorized)
    }
}

private fun Set<Long>.toggled(id: Long): Set<Long> = if (id in this) this - id else this + id
