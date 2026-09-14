package com.aria.rythme.feature.pitch.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.feature.pitch.data.MelodyModelManager
import kotlinx.coroutines.launch

class MelodyModelViewModel(val manager: MelodyModelManager) : ViewModel() {
    val state = manager.state
    init { viewModelScope.launch { manager.refresh() } }
    suspend fun isReady(): Boolean {
        manager.refresh()
        return state.value.phase == com.aria.rythme.feature.pitch.data.ModelPhase.Ready
    }
    fun delete() { viewModelScope.launch { manager.delete() } }
}
