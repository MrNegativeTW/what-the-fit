package com.txwstudio.app.whatthefit.ui.generate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.txwstudio.app.whatthefit.data.entity.Category
import com.txwstudio.app.whatthefit.data.entity.OotdWithItems
import com.txwstudio.app.whatthefit.data.repository.WardrobeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GenerateViewModel @Inject constructor(
    repository: WardrobeRepository,
) : ViewModel() {
    /** All body-part categories; generating an OOTD always rolls every one of them. */
    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Past OOTD records shown on the Outfit page, newest first. */
    val ootds: StateFlow<List<OotdWithItems>> = repository.observeOotds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
