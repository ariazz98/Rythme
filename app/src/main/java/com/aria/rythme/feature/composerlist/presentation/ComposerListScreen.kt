package com.aria.rythme.feature.composerlist.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aria.rythme.core.extensions.collectAsUiState
import com.aria.rythme.feature.navigationbar.domain.model.RythmeRoute
import com.aria.rythme.ui.component.CommonListItem
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.theme.rythmeColors
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ComposerListScreen(
    viewModel: ComposerListViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsUiState()
    val composers = state.value.composers

    MainListPage(
        routeKey = RythmeRoute.ComposerList
    ) {
        itemsIndexed(composers, key = { _, composer -> composer }) { index, composer ->
            CommonListItem(
                title = composer,
                showDivider = index != composers.size - 1,
                onClick = { viewModel.sendIntent(ComposerListIntent.ClickComposer(composer)) }
            )
        }
    }
}
