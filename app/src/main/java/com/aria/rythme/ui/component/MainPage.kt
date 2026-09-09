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
import com.aria.rythme.LocalInnerPadding
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.feature.navigationbar.presentation.LocalBottomBarState
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun MainListPage(
    title: String? = null,
    headerMode: HeaderMode = HeaderMode.COLLAPSED,
    topBar: TopBarConfig? = null,
    search: PageSearchState? = null,
    mainContent: LazyListScope.() -> Unit
) {
    val entry = LocalTopBarEntry.current
    val isRoot = entry.route in ALL_TOP_LEVEL_ROUTES
    val pageSearch = search ?: rememberPageSearchState()
    val hasSearch = if (isRoot) search != null else headerMode != HeaderMode.HIDDEN
    val listState = rememberLazyListState()
    val collapse = rememberPageHeader(title, topBar, headerMode, pageSearch, hasSearch)
    val innerPadding = LocalInnerPadding.current
    val bottomBar = LocalBottomBarState.current
    val bottomBarScrollConnection = remember(bottomBar, listState) {
        bottomBar.nestedScrollConnection { !listState.canScrollBackward }
    }

    val atTop = !listState.canScrollBackward
    val firstVisibleItemIndex = listState.firstVisibleItemIndex
    val scrollOffsetDp = listState.firstVisibleItemScrollOffset / LocalDensity.current.density
    SideEffect {
        entry.scroll.atTop = atTop
        entry.scroll.firstVisibleItemIndex = firstVisibleItemIndex
        entry.scroll.firstVisibleItemScrollOffsetDp = scrollOffsetDp
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.rythmeColors.surface)
            .nestedScroll(bottomBarScrollConnection)
            .then(if (!isRoot && !pageSearch.active) Modifier.nestedScroll(collapse.nestedScrollConnection) else Modifier)
    ) {
        item(key = "page-header") {
            PageHeaderContent(
                title, innerPadding.calculateTopPadding(), isRoot, headerMode, pageSearch,
                collapse, HeaderLayout.horizontalPadding, hasSearch
            )
        }
        mainContent()
        item(key = "bottom-bar-inset") { Spacer(Modifier.height(innerPadding.calculateBottomPadding())) }
    }
}

@Composable
fun MainGridPage(
    title: String? = null,
    gridCount: Int = 2,
    headerMode: HeaderMode = HeaderMode.COLLAPSED,
    topBar: TopBarConfig? = null,
    search: PageSearchState? = null,
    mainContent: LazyGridScope.() -> Unit
) {
    val entry = LocalTopBarEntry.current
    val isRoot = entry.route in ALL_TOP_LEVEL_ROUTES
    val pageSearch = search ?: rememberPageSearchState()
    val hasSearch = if (isRoot) search != null else headerMode != HeaderMode.HIDDEN
    val gridState = rememberLazyGridState()
    val collapse = rememberPageHeader(title, topBar, headerMode, pageSearch, hasSearch)
    val gridSpacing = 12.dp
    val innerPadding = LocalInnerPadding.current
    val bottomBar = LocalBottomBarState.current
    val bottomBarScrollConnection = remember(bottomBar, gridState) {
        bottomBar.nestedScrollConnection { !gridState.canScrollBackward }
    }

    val atTop = !gridState.canScrollBackward
    val firstVisibleItemIndex = gridState.firstVisibleItemIndex
    val scrollOffsetDp = gridState.firstVisibleItemScrollOffset / LocalDensity.current.density
    SideEffect {
        entry.scroll.atTop = atTop
        entry.scroll.firstVisibleItemIndex = firstVisibleItemIndex
        entry.scroll.firstVisibleItemScrollOffsetDp = scrollOffsetDp
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridCount),
        state = gridState,
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.rythmeColors.surface)
            .padding(horizontal = HeaderLayout.horizontalPadding)
            .nestedScroll(bottomBarScrollConnection)
            .then(if (!isRoot && !pageSearch.active) Modifier.nestedScroll(collapse.nestedScrollConnection) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(gridSpacing),
        verticalArrangement = Arrangement.spacedBy(gridSpacing)
    ) {
        item(key = "page-header", span = { GridItemSpan(maxLineSpan) }) {
            PageHeaderContent(title, innerPadding.calculateTopPadding(), isRoot, headerMode, pageSearch,
                collapse, 0.dp, hasSearch, rootSearchSpacing = gridSpacing)
        }
        mainContent()
        item(key = "bottom-bar-inset", span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(innerPadding.calculateBottomPadding()))
        }
    }
}
