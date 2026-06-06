package com.txwstudio.app.whatthefit.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.txwstudio.app.whatthefit.data.repository.WardrobeRepository
import com.txwstudio.app.whatthefit.domain.OutfitGenerator
import com.txwstudio.app.whatthefit.domain.model.OutfitSlot
import com.txwstudio.app.whatthefit.ui.navigation.WtfRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    private val generator: OutfitGenerator,
    private val repository: WardrobeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val categoryIds: List<Long> =
        (savedStateHandle.get<String>(WtfRoutes.ARG_CATEGORY_IDS) ?: "")
            .split(",")
            .mapNotNull { it.toLongOrNull() }

    private val _slots = MutableStateFlow<List<OutfitSlot>>(emptyList())
    val slots = _slots.asStateFlow()

    // One-shot signal that the generated outfit was saved as an OOTD, for a confirmation snackbar.
    private val _saved = Channel<Unit>(Channel.CONFLATED)
    val saved = _saved.receiveAsFlow()

    init {
        rerollAll()
    }

    fun rerollAll() {
        viewModelScope.launch { _slots.value = generator.generate(categoryIds) }
    }

    fun rerollSingle(categoryId: Long) {
        viewModelScope.launch {
            val currentItemId = _slots.value.firstOrNull { it.category.id == categoryId }?.item?.id
            val newItem = generator.rerollSingle(categoryId, currentItemId)
            _slots.value = _slots.value.map { slot ->
                if (slot.category.id == categoryId) slot.copy(item = newItem) else slot
            }
        }
    }

    /** Saves the current generated outfit as a new OOTD for today, then signals [saved]. */
    fun saveAsOotd() {
        val outfitSlots = _slots.value.mapNotNull { slot ->
            slot.item?.let { slot.category.id to it.id }
        }
        if (outfitSlots.isEmpty()) return
        viewModelScope.launch {
            repository.saveOotd(
                id = 0L,
                epochDay = LocalDate.now().toEpochDay(),
                notes = "",
                photoPath = null,
                slots = outfitSlots,
            )
            _saved.send(Unit)
        }
    }
}

