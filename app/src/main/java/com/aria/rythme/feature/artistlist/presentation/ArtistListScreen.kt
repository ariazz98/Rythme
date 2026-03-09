package com.aria.rythme.feature.artistlist.presentation

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aria.rythme.R
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.ArtistListItem
import com.aria.rythme.ui.component.MainListPage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ArtistListScreen(
    viewModel: ArtistListViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsUiState()
    val artists = state.value.artists

    MainListPage(
        title = stringResource(R.string.title_artist),
        routeKey = RythmeRoute.ArtistList,
        autoHide = false
    ) {
        itemsIndexed(artists, key = { _, artist -> artist.id }) { index, artist ->
            ArtistListItem(
                artist = artist,
                showDivider = index != artists.size - 1,
                onClick = { viewModel.sendIntent(ArtistListIntent.ClickArtist(artist)) }
            )
        }
    }
}
