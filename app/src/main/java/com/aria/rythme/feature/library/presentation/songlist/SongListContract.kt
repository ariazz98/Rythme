package com.aria.rythme.feature.library.presentation.songlist

import com.aria.rythme.core.mvi.SideEffect
import com.aria.rythme.core.mvi.UiState
import com.aria.rythme.core.mvi.UserIntent
import com.aria.rythme.feature.player.data.model.Song

/**
 * 歌曲列表页面的 MVI Contract
 */

/**
 * UI 状态
 */
data class SongListState(
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val isShuffleEnabled: Boolean = false
) : UiState

/**
 * 用户意图
 */
sealed interface SongListIntent : UserIntent {
    /**
     * 加载歌曲列表
     */
    data object LoadSongs : SongListIntent

    /**
     * 播放所有歌曲
     */
    data object PlayAll : SongListIntent

    /**
     * 切换随机播放
     */
    data object ToggleShuffle : SongListIntent

    /**
     * 播放指定歌曲
     */
    data class PlaySong(val song: Song) : SongListIntent

    /**
     * 显示歌曲更多选项
     */
    data class ShowSongOptions(val song: Song) : SongListIntent
}

/**
 * 副作用
 */
sealed interface SongListEffect : SideEffect {
    /**
     * 显示提示信息
     */
    data class ShowToast(val message: String) : SongListEffect
}
