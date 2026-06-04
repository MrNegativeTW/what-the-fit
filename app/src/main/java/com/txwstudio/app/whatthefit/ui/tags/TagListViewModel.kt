package com.txwstudio.app.whatthefit.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.txwstudio.app.whatthefit.data.entity.Tag
import com.txwstudio.app.whatthefit.data.entity.TagWithCount
import com.txwstudio.app.whatthefit.data.repository.WardrobeRepository
import com.txwstudio.app.whatthefit.domain.model.TagKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Generic management VM for one [TagKind]. The screen calls [setKind] once; use a distinct
 * `hiltViewModel(key = ...)` per kind so each tab gets its own instance.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TagListViewModel @Inject constructor(
    private val repository: WardrobeRepository,
) : ViewModel() {
    private val kindFlow = MutableStateFlow<TagKind?>(null)

    val tags: StateFlow<List<TagWithCount>> = kindFlow
        .filterNotNull()
        .flatMapLatest { repository.observeTagsWithCounts(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setKind(kind: TagKind) {
        kindFlow.value = kind
    }

    fun add(name: String, swatchArgb: Long? = null) {
        val trimmed = name.trim()
        val kind = kindFlow.value
        if (trimmed.isEmpty() || kind == null) return
        viewModelScope.launch { repository.addTag(kind, trimmed, swatchArgb) }
    }

    fun update(tag: Tag, name: String, swatchArgb: Long? = tag.swatchArgb) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch { repository.updateTag(tag.copy(name = trimmed, swatchArgb = swatchArgb)) }
    }

    fun delete(tag: Tag) {
        viewModelScope.launch { repository.deleteTag(tag) }
    }

    fun reorder(orderedIds: List<Long>) {
        viewModelScope.launch { repository.reorderTags(orderedIds) }
    }
}
