package com.aria.rythme.feature.artistlist.presentation

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.Artist
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
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
    val artists = state.artists.filter { search.matches(it.name) }

    MainListPage(
        title = stringResource(R.string.title_artist),
        routeKey = RythmeRoute.ArtistList,
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
