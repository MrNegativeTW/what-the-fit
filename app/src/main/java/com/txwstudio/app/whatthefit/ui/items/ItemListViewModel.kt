package com.txwstudio.app.whatthefit.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
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

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemListViewModel @Inject constructor(
    private val repository: WardrobeRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _selectedBrandIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _selectedColorIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _selectedOccasionIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedCategoryIds = _selectedCategoryIds.asStateFlow()
    val selectedBrandIds = _selectedBrandIds.asStateFlow()
    val selectedColorIds = _selectedColorIds.asStateFlow()
    val selectedOccasionIds = _selectedOccasionIds.asStateFlow()

    // Label sources for the filter chip rows.
    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val brands: StateFlow<List<Tag>> = repository.observeTags(TagKind.BRAND)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val colors: StateFlow<List<Tag>> = repository.observeTags(TagKind.COLOR)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val occasions: StateFlow<List<Tag>> = repository.observeTags(TagKind.OCCASION)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** True when a text query or any filter is active — picks the "no match" vs "empty" message. */
    val isFiltering: StateFlow<Boolean> = combine(
        _query,
        _selectedCategoryIds,
        _selectedBrandIds,
        _selectedColorIds,
        _selectedOccasionIds,
    ) { q, catIds, brandIds, colorIds, occIds ->
        q.isNotBlank() || catIds.isNotEmpty() || brandIds.isNotEmpty() ||
            colorIds.isNotEmpty() || occIds.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val items: Flow<PagingData<ItemWithDetails>> = combine(
        _query.debounce(300), // only the text is debounced; filters apply immediately
        _selectedCategoryIds,
        _selectedBrandIds,
        _selectedColorIds,
        _selectedOccasionIds,
    ) { q, catIds, brandIds, colorIds, occIds ->
        // sorted() makes the key stable so distinctUntilChanged() skips no-op rebuilds.
        Criteria(q, catIds.sorted(), brandIds.sorted(), colorIds.sorted(), occIds.sorted())
    }
        .distinctUntilChanged()
        .flatMapLatest { c ->
            Pager(PagingConfig(pageSize = 30, enablePlaceholders = false)) {
                repository.searchItems(c.query, c.categoryIds, c.brandIds, c.colorIds, c.occasionIds)
            }.flow
        }
        .cachedIn(viewModelScope)

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun toggleCategory(id: Long) = _selectedCategoryIds.update { it.toggled(id) }
    fun toggleBrand(id: Long) = _selectedBrandIds.update { it.toggled(id) }
    fun toggleColor(id: Long) = _selectedColorIds.update { it.toggled(id) }
    fun toggleOccasion(id: Long) = _selectedOccasionIds.update { it.toggled(id) }

    fun setAvailability(id: Long, available: Boolean) {
        viewModelScope.launch { repository.setAvailability(id, available) }
    }

    private data class Criteria(
        val query: String,
        val categoryIds: List<Long>,
        val brandIds: List<Long>,
        val colorIds: List<Long>,
        val occasionIds: List<Long>,
    )
}

private fun Set<Long>.toggled(id: Long): Set<Long> = if (id in this) this - id else this + id
