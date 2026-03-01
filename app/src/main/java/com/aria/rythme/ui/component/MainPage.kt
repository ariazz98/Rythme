package com.aria.rythme.ui.component

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.LocalInnerPadding
import com.aria.rythme.core.utils.rememberScreenCornerRadiusDp
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun MainListPage(
    routeKey: NavKey,
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

    // 同步滚动位置到全局 TopBar 可见性状态（按路由 key 存储，切换 Tab 时立即生效）
    val topBarState = LocalTopBarState.current
    LaunchedEffect(listState) {
        snapshotFlow { isAtTop }.collect { atTop ->
            topBarState.updateScrollAtTop(routeKey, atTop)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().clip(ContinuousRoundedRectangle(rememberScreenCornerRadiusDp()))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部占位空间（为全局 TopBar 留出空间）
            item {
                Spacer(modifier = Modifier.height(topPadding))
            }

            mainContent()

            item {
                Spacer(modifier = Modifier.height(bottomPadding))
            }
        }
    }
}

@Composable
fun MainGridPage(
    routeKey: NavKey,
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

    // 同步滚动位置到全局 TopBar 可见性状态（按路由 key 存储，切换 Tab 时立即生效）
    val topBarState = LocalTopBarState.current
    LaunchedEffect(gridState) {
        snapshotFlow { isAtTop }.collect { atTop ->
            topBarState.updateScrollAtTop(routeKey, atTop)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().clip(ContinuousRoundedRectangle(rememberScreenCornerRadiusDp()))
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCount),
            state = gridState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 21.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部占位空间（为全局 TopBar 留出空间）
            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(topPadding))
            }

            mainContent()

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(bottomPadding))
            }
        }
    }
}
