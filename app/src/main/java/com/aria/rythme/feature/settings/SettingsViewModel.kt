package com.aria.rythme.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.music.data.indexer.MusicIndexer
import com.aria.rythme.core.music.data.model.ScanProgress
import com.aria.rythme.core.music.data.settings.AppSettingsRepository
import com.aria.rythme.core.music.data.settings.ScanSettings
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsState(val name: String = "ARiA", val scan: ScanSettings = ScanSettings(), val progress: ScanProgress = ScanProgress.Idle)

class SettingsViewModel(private val repository: AppSettingsRepository, private val indexer: MusicIndexer) : ViewModel() {
    val state = combine(repository.displayName, repository.settings, indexer.scanProgress, ::SettingsState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState())
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()

    fun saveName(name: String) = perform {
        repository.updateDisplayName(name)
        _message.value = "名称已保存"
    }

    fun saveScan(settings: ScanSettings) = perform {
        repository.updateSettings(settings)
        indexer.forceFullRescan()
        reportEmptyScan()
    }

    fun rescan() = perform { indexer.forceFullRescan(); reportEmptyScan() }

    private fun reportEmptyScan() {
        if (indexer.scanProgress.value == ScanProgress.Idle) {
            _message.value = "本次未发现可导入的歌曲，已保留旧曲库。请检查媒体权限或调整扫描规则。"
        }
    }

    private fun perform(action: suspend () -> Unit) {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            try { _message.value = null; action() }
            catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (error: Exception) { _message.value = "操作失败：${error.message ?: "请稍后重试"}" }
            finally { _busy.value = false }
        }
    }
}
