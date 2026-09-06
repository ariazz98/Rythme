package com.aria.rythme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.LocalInnerPadding
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.feature.navigationbar.presentation.LocalBottomBarState
import com.aria.rythme.ui.theme.rythmeColors
import kotlinx.coroutines.flow.first

@Composable
fun MainListPage(
    title: String? = null,
    routeKey: NavKey,
    headerMode: HeaderMode = HeaderMode.COLLAPSED,
    defaultTitleHidden: Boolean = false,
    topBar: TopBarConfig? = null,
    search: PageSearchState = rememberPageSearchState(),
    mainContent: LazyListScope.() -> Unit
) {
    val isRoot = routeKey in ALL_TOP_LEVEL_ROUTES
    val density = LocalDensity.current
    val titleHeightPx = with(density) { (HeaderLayout.title + HeaderLayout.gap * 2).roundToPx() }
    val listState = rememberLazyListState()
    val collapse = rememberPageHeader(routeKey, title, topBar, headerMode, search)
    val innerPadding = LocalInnerPadding.current
    val bottomBar = LocalBottomBarState.current

    if (!isRoot && defaultTitleHidden) LaunchedEffect(Unit) {
        snapshotFlow { listState.canScrollForward }.first { it }
        listState.scrollToItem(0, titleHeightPx)
    }
    val titleAlpha by remember {
        derivedStateOf {
            if (isRoot) 1f else if (listState.firstVisibleItemIndex > 0) 0f
            else (1f - listState.firstVisibleItemScrollOffset / titleHeightPx.toFloat()).coerceIn(0f, 1f)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.rythmeColors.surface)
            .nestedScroll(bottomBar.nestedScrollConnection)
            .then(if (!isRoot && !search.active) Modifier.nestedScroll(collapse.nestedScrollConnection) else Modifier)
    ) {
        item(key = "page-header") {
            PageHeaderContent(
                title, innerPadding.calculateTopPadding(), isRoot, headerMode, search,
                collapse, titleAlpha, HeaderLayout.horizontalPadding
            )
        }
        mainContent()
        item(key = "bottom-bar-inset") { Spacer(Modifier.height(innerPadding.calculateBottomPadding())) }
    }
}

@Composable
fun MainGridPage(
    title: String? = null,
    routeKey: NavKey,
    gridCount: Int = 2,
    headerMode: HeaderMode = HeaderMode.COLLAPSED,
    defaultTitleHidden: Boolean = false,
    topBar: TopBarConfig? = null,
    search: PageSearchState = rememberPageSearchState(),
    mainContent: LazyGridScope.() -> Unit
) {
    val isRoot = routeKey in ALL_TOP_LEVEL_ROUTES
    val density = LocalDensity.current
    val titleHeightPx = with(density) { (HeaderLayout.title + HeaderLayout.gap * 2).roundToPx() }
    val gridState = rememberLazyGridState()
    val collapse = rememberPageHeader(routeKey, title, topBar, headerMode, search)
    val innerPadding = LocalInnerPadding.current
    val bottomBar = LocalBottomBarState.current

    if (!isRoot && defaultTitleHidden) LaunchedEffect(Unit) {
        snapshotFlow { gridState.canScrollForward }.first { it }
        gridState.scrollToItem(0, titleHeightPx)
    }
    val titleAlpha by remember {
        derivedStateOf {
            if (isRoot) 1f else if (gridState.firstVisibleItemIndex > 0) 0f
            else (1f - gridState.firstVisibleItemScrollOffset / titleHeightPx.toFloat()).coerceIn(0f, 1f)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridCount),
        state = gridState,
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.rythmeColors.surface)
            .padding(horizontal = HeaderLayout.horizontalPadding)
            .nestedScroll(bottomBar.nestedScrollConnection)
            .then(if (!isRoot && !search.active) Modifier.nestedScroll(collapse.nestedScrollConnection) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "page-header", span = { GridItemSpan(maxLineSpan) }) {
            PageHeaderContent(title, innerPadding.calculateTopPadding(), isRoot, headerMode, search, collapse, titleAlpha, 0.dp)
        }
        mainContent()
        item(key = "bottom-bar-inset", span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}
