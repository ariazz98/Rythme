package com.aria.rythme.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aria.rythme.core.navigation.LocalInnerPadding

@Composable
fun MainListPage(
    titleRes: Int,
    hasMoreMenu: Boolean,
    hasAvatar: Boolean,
    onMoreClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null,
    mainContent: LazyListScope.() -> Unit
) {

    val listState = rememberLazyListState()

    val density = LocalDensity.current
    val innerPadding = LocalInnerPadding.current
    val topPadding = innerPadding.calculateTopPadding()
    val bottomPadding = innerPadding.calculateBottomPadding()

    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex < 1 && listState.firstVisibleItemScrollOffset < with(density) { 10.dp.toPx() }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部占位空间（用于标题栏）
            item {
                Spacer(modifier = Modifier.height(61.dp + topPadding))
            }

            mainContent()

            item {
                Spacer(modifier = Modifier.height(bottomPadding))
            }
        }

        // 可折叠的标题栏
        AnimatedVisibility(
            visible = isAtTop,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topPadding)
        ) {
            RythmeHeader(
                title = stringResource(titleRes),
                hasMoreMenu = hasMoreMenu,
                hasAvatar = hasAvatar,
                onMoreClick = {
                    onMoreClick?.invoke()
                },
                onAvatarClick = {
                    onAvatarClick?.invoke()
                }
            )
        }
    }
}

@Composable
fun MainGridPage(
    titleRes: Int,
    hasMoreMenu: Boolean,
    hasAvatar: Boolean,
    onMoreClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null,
    gridCount: Int = 2,
    mainContent: LazyGridScope.() -> Unit
) {
    val gridState = rememberLazyGridState()

    val density = LocalDensity.current
    val innerPadding = LocalInnerPadding.current
    val topPadding = innerPadding.calculateTopPadding()
    val bottomPadding = innerPadding.calculateBottomPadding()

    val isAtTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex < 1 && gridState.firstVisibleItemScrollOffset < with(density) { 10.dp.toPx() }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCount),
            state = gridState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 21.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部占位空间（用于标题栏）
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(61.dp + topPadding))
            }

            mainContent()

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(bottomPadding))
            }
        }

        // 可折叠的标题栏
        AnimatedVisibility(
            visible = isAtTop,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topPadding)
        ) {
            RythmeHeader(
                title = stringResource(titleRes),
                hasMoreMenu = hasMoreMenu,
                hasAvatar = hasAvatar,
                onMoreClick = {
                    onMoreClick?.invoke()
                },
                onAvatarClick = {
                    onAvatarClick?.invoke()
                }
            )
        }
    }
}