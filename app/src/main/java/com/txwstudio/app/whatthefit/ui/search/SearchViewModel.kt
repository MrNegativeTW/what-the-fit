package com.txwstudio.app.whatthefit.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.txwstudio.app.whatthefit.data.entity.ItemWithDetails
import com.txwstudio.app.whatthefit.data.entity.OotdWithItems
import com.txwstudio.app.whatthefit.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Backs the dedicated search screen: bounded clothes search plus an in-memory OOTD history match. */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    repository: WardrobeRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(value: String) {
        _query.value = value
    }

    val clothesResults: StateFlow<List<ItemWithDetails>> = _query
        .debounce(250)
        .flatMapLatest { q ->
            val term = q.trim()
            if (term.isBlank()) flowOf(emptyList()) else flow { emit(repository.searchClothes(term)) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val ootdResults: StateFlow<List<OotdWithItems>> = combine(
        _query.debounce(250),
        repository.observeOotds(),
    ) { q, ootds ->
        val term = q.trim()
        if (term.isBlank()) {
            emptyList()
        } else {
            ootds.filter { ootd ->
                ootd.record.notes.contains(term, ignoreCase = true) ||
                    ootd.slots.any { it.item?.name?.contains(term, ignoreCase = true) == true }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
