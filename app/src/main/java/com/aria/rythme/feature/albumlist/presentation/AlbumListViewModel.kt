package com.aria.rythme.feature.albumlist.presentation

import androidx.lifecycle.viewModelScope
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.repository.MusicRepository
import com.aria.rythme.core.music.data.settings.AppSettingsRepository
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 专辑列表 ViewModel
 *
 * 从 MusicRepository 读取专辑列表并展示。
 */
class AlbumListViewModel(
    private val navigator: Navigator,
    private val musicRepository: MusicRepository,
    private val appSettings: AppSettingsRepository
) : BaseViewModel<AlbumListIntent, AlbumListState, AlbumListAction, AlbumListEffect>() {

    private var rawAlbums: List<Album> = emptyList()

    init {
        loadSavedPreferences()
        observeAlbums()
    }

    private fun loadSavedPreferences() {
        viewModelScope.launch {
            val savedSort = appSettings.getPagePreferenceValue(PAGE_KEY, KEY_SORT, AlbumSortBy.TITLE)
            val savedLayout = appSettings.getPagePreferenceValue(PAGE_KEY, KEY_LAYOUT, AlbumLayoutMode.GRID)
            reduceAndUpdate(AlbumListAction.SortChanged(savedSort, sortAlbums(rawAlbums, savedSort)))
            reduceAndUpdate(AlbumListAction.LayoutChanged(savedLayout))
        }
    }

    override fun createInitialState(): AlbumListState = AlbumListState()

    override fun handleIntent(intent: AlbumListIntent) {
        when (intent) {
            is AlbumListIntent.GoBack -> navigator.goBack()
            is AlbumListIntent.ClickAlbum -> navigator.navigate(
                RythmeRoute.AlbumDetail(intent.album.id.toString())
            )
            is AlbumListIntent.SetSort -> {
                reduceAndUpdate(AlbumListAction.SortChanged(intent.sortBy, sortAlbums(rawAlbums, intent.sortBy)))
                viewModelScope.launch { appSettings.setPagePreference(PAGE_KEY, KEY_SORT, intent.sortBy) }
            }
            is AlbumListIntent.SetLayout -> {
                reduceAndUpdate(AlbumListAction.LayoutChanged(intent.layoutMode))
                viewModelScope.launch { appSettings.setPagePreference(PAGE_KEY, KEY_LAYOUT, intent.layoutMode) }
            }
        }
    }

    override fun reduce(action: AlbumListAction): AlbumListState {
        return when (action) {
            is AlbumListAction.AlbumsLoaded -> currentState.copy(
                albums = action.albums,
                isLoading = false
            )
            is AlbumListAction.SortChanged -> currentState.copy(
                sortBy = action.sortBy,
                albums = action.albums
            )
            is AlbumListAction.LayoutChanged -> currentState.copy(
                layoutMode = action.layoutMode
            )
        }
    }

    private fun observeAlbums() {
        musicRepository.getAllAlbums()
            .onEach { albums ->
                rawAlbums = albums
                reduceAndUpdate(AlbumListAction.AlbumsLoaded(sortAlbums(albums, currentState.sortBy)))
            }
            .launchIn(viewModelScope)
    }

    companion object {
        private const val PAGE_KEY = "album_list"
        private const val KEY_SORT = "sort_by"
        private const val KEY_LAYOUT = "layout_mode"
    }

    private fun sortAlbums(albums: List<Album>, sortBy: AlbumSortBy): List<Album> {
        return when (sortBy) {
            AlbumSortBy.TITLE -> albums.sortedBy { it.title }
            AlbumSortBy.ARTIST -> albums.sortedBy { it.artist }
            AlbumSortBy.YEAR -> albums.sortedByDescending { it.year }
            AlbumSortBy.SONG_COUNT -> albums.sortedByDescending { it.songCount }
            AlbumSortBy.RECENTLY_ADDED -> albums.sortedByDescending { it.id }
        }
    }
}
