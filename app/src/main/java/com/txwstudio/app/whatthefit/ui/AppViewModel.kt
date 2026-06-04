package com.txwstudio.app.whatthefit.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.txwstudio.app.whatthefit.data.prefs.ThemePreferences
import com.txwstudio.app.whatthefit.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Supplies the app-wide theme mode consumed by [WtfApp]. */
@HiltViewModel
class AppViewModel @Inject constructor(
    themePreferences: ThemePreferences,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
}
