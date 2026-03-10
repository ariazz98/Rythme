package com.aria.rythme.feature.genrelist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aria.rythme.R
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.CommonListItem
import com.aria.rythme.ui.component.HeaderMode
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.theme.rythmeColors
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun GenreListScreen(
    viewModel: GenreListViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsUiState()
    val genres = state.value.genres

    MainListPage(
        title = stringResource(R.string.title_type),
        routeKey = RythmeRoute.GenreList
    ) {
        itemsIndexed(genres, key = { _, genre -> genre }) { index, genre ->
            CommonListItem(
                title = genre,
                showDivider = index != genres.size - 1,
                onClick = { viewModel.sendIntent(GenreListIntent.ClickGenre(genre)) }
            )
        }
    }
}
