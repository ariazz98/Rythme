package com.aria.rythme.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.LocalInnerPadding
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun MainListPage(
    title: String? = null,
    routeKey: NavKey,
    autoHide: Boolean = true,
    mainContent: LazyListScope.() -> Unit
) {

    val isTopPage = routeKey in ALL_TOP_LEVEL_ROUTES

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

    val headerAlpha by animateFloatAsState(
        targetValue = if (isAtTop) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "headerAlpha"
    )

    // 同步滚动位置到全局 TopBar 可见性状态（按路由 key 存储，切换 Tab 时立即生效）
    val topBarState = LocalTopBarState.current
    LaunchedEffect(listState, autoHide) {
        if (autoHide) {
            snapshotFlow { isAtTop }.collect { atTop ->
                topBarState.updateIsShow(routeKey, atTop)
            }
        } else {
            topBarState.updateIsShow(routeKey, true)
        }
    }

    // 页面出栈时清除该路由的所有 TopBar 状态（包括搜索状态）
    DisposableEffect(routeKey) {
        onDispose {
            topBarState.resetSearchState(routeKey)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.rythmeColors.surface)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            // 顶部占位空间（为全局 TopBar 留出空间）
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isTopPage) topPadding - 104.dp else topPadding),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (!title.isNullOrEmpty() && !isTopPage) {
                        val hidePlaceholder = rememberSearchAnimating(
                            topBarState.isSearchActive(routeKey)
                        )
                        if (!hidePlaceholder) {
                            Column(
                                modifier = Modifier
                                    .padding(
                                        start = 21.dp,
                                        end = 21.dp
                                    )
                                    .height(104.dp)
                            ) {
                                Box(
                                    modifier = Modifier.height(36.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    // 标题
                                    Text(
                                        text = title,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.rythmeColors.textColor
                                    )
                                }

                                SearchPlaceholder(
                                    onClick = {
                                        topBarState.updateSearchActive(routeKey, true, title)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            mainContent()

            item {
                Spacer(modifier = Modifier.height(bottomPadding))
            }
        }

        if (!title.isNullOrEmpty() && routeKey in ALL_TOP_LEVEL_ROUTES) {
            // 标题
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.rythmeColors.textColor,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 21.dp, vertical = 16.dp)
                    .alpha(headerAlpha)
            )
        }
    }
}

@Composable
fun MainGridPage(
    title: String? = null,
    routeKey: NavKey,
    gridCount: Int = 2,
    autoHide: Boolean = true,
    mainContent: LazyGridScope.() -> Unit
) {
    val gridState = rememberLazyGridState()

    val isTopPage = routeKey in ALL_TOP_LEVEL_ROUTES

    val density = LocalDensity.current
    val innerPadding = LocalInnerPadding.current
    val topPadding = innerPadding.calculateTopPadding()
    val bottomPadding = innerPadding.calculateBottomPadding()

    val isAtTop by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex < 1 && gridState.firstVisibleItemScrollOffset < with(density) { 10.dp.toPx() }
        }
    }

    val headerAlpha by animateFloatAsState(
        targetValue = if (isAtTop) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "headerAlpha"
    )

    // 同步滚动位置到全局 TopBar 可见性状态（按路由 key 存储，切换 Tab 时立即生效）
    val topBarState = LocalTopBarState.current
    LaunchedEffect(gridState, autoHide) {
        if (autoHide) {
            snapshotFlow { isAtTop }.collect { atTop ->
                topBarState.updateIsShow(routeKey, atTop)
            }
        } else {
            topBarState.updateIsShow(routeKey, true)
        }
    }

    // 页面出栈时清除该路由的所有 TopBar 状态（包括搜索状态）
    DisposableEffect(routeKey) {
        onDispose {
            topBarState.resetSearchState(routeKey)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.rythmeColors.surface)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCount),
            state = gridState,
            modifier = Modifier.fillMaxSize().padding(horizontal = 21.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = spacedBySkipFirst(12.dp)
        ) {
            // 顶部占位空间（为全局 TopBar 留出空间）
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isTopPage) topPadding - 104.dp else topPadding),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (!title.isNullOrEmpty() && !isTopPage) {
                        val hidePlaceholder = rememberSearchAnimating(
                            topBarState.isSearchActive(routeKey)
                        )
                        if (!hidePlaceholder) {
                            Column(
                                modifier = Modifier
                                    .height(104.dp)
                            ) {
                                Box(
                                    modifier = Modifier.height(36.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    // 标题
                                    Text(
                                        text = title,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.rythmeColors.textColor
                                    )
                                }

                                SearchPlaceholder(
                                    onClick = {
                                        topBarState.updateSearchActive(routeKey, true, title)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            mainContent()

            item(span = { GridItemSpan(maxLineSpan) }) {
                Spacer(modifier = Modifier.height(bottomPadding))
            }
        }

        if (!title.isNullOrEmpty() && routeKey in ALL_TOP_LEVEL_ROUTES) {
            // 标题
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.rythmeColors.textColor,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 21.dp, vertical = 16.dp)
                    .alpha(headerAlpha)
            )
        }
    }
}

/**
 * 自定义垂直排列：跳过第一个 item 之后的间距，其余 item 之间使用指定间距。
 * 用于 Grid 中第一个占位 item 与内容之间不需要额外间距的场景。
 */
private fun spacedBySkipFirst(spacing: Dp): Arrangement.Vertical =
    object : Arrangement.Vertical {
        override fun Density.arrange(totalSize: Int, sizes: IntArray, outPositions: IntArray) {
            val spacingPx = spacing.roundToPx()
            var current = 0
            sizes.forEachIndexed { index, size ->
                outPositions[index] = current
                current += size
                if (index > 0) current += spacingPx
            }
        }
    }
