package com.aria.rythme.feature.songlist.presentation

import com.aria.rythme.core.mvi.InternalAction
import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent
import com.aria.rythme.core.music.data.model.Song

/**
 * 歌曲列表页面的 MVI Contract
 */

/**
 * UI 状态
 */
data class SongListState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true
) : UiState

/**
 * 用户意图
 */
sealed interface SongListIntent : UserIntent {
    data object GoBack : SongListIntent
    data object PlayAll : SongListIntent
    data object ShufflePlay : SongListIntent
    data class PlaySong(val song: Song) : SongListIntent
    data class ShowSongOptions(val song: Song) : SongListIntent
}

/**
 * 内部动作
 */
sealed interface SongListAction : InternalAction {
    data class SongsLoaded(val songs: List<Song>) : SongListAction
}

/**
 * 副作用
 */
sealed interface SongListEffect : SideEffect {
    data class ShowToast(val message: String) : SongListEffect
}
