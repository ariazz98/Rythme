package com.aria.rythme.feature.artistlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.ArtistListItem
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.theme.rythmeColors
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ArtistListScreen(
    viewModel: ArtistListViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsUiState()
    val artists = state.value.artists

    MainListPage(
        routeKey = RythmeRoute.ArtistList
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
