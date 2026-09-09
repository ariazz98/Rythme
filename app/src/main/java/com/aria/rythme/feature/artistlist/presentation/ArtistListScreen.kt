package com.aria.rythme.feature.artistlist.presentation

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.Artist
import com.aria.rythme.ui.component.Action
import com.aria.rythme.ui.component.TopBarConfig
import com.aria.rythme.ui.component.ArtistListItem
import com.aria.rythme.ui.component.HeaderMode
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.component.rememberPageSearchState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ArtistListScreen(
    onArtistClick: (Artist) -> Unit,
    viewModel: ArtistListViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val search = rememberPageSearchState()
    val overlayMenu = com.aria.rythme.ui.component.LocalOverlayMenu.current
    val artists = state.artists.filter { search.matches(it.name) }

    MainListPage(
        title = stringResource(R.string.title_artist),
        topBar = TopBarConfig(
            showBackButton = true,
            actions = listOf(Action.Icon(actionKey = "filter", iconRes = R.drawable.ic_filter,
                menu = { com.aria.rythme.ui.component.previewFilterMenu(overlayMenu::dismiss) }))
        ),
        search = search
    ) {
        itemsIndexed(artists, key = { _, artist -> artist.id }) { index, artist ->
            ArtistListItem(
                artist = artist,
                showDivider = index != artists.size - 1,
                onClick = { onArtistClick(artist) }
            )
        }
    }
}
