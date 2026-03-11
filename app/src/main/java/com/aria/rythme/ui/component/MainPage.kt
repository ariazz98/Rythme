package com.aria.rythme.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.first
import com.aria.rythme.LocalInnerPadding
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun MainListPage(
    title: String? = null,
    routeKey: NavKey,
    headerMode: HeaderMode = HeaderMode.COLLAPSED,
    defaultTitleHidden: Boolean = false,
    mainContent: LazyListScope.() -> Unit
) {

    val isTopPage = routeKey in ALL_TOP_LEVEL_ROUTES

    val density = LocalDensity.current
    val titleHeightPx = with(density) { 48.dp.roundToPx() }
    val listState = rememberLazyListState()

    // 默认隐藏标题：等列表可滚动后滚动到标题刚好隐藏的位置
    if (!isTopPage && defaultTitleHidden) {
        LaunchedEffect(Unit) {
            snapshotFlow { listState.canScrollForward }
                .first { it }
            listState.scrollToItem(0, titleHeightPx)
        }
    }

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

    // 非顶级页面标题渐隐：列表滚动 48dp 时完全透明
    val titleAlpha by remember {
        derivedStateOf {
            if (isTopPage) 1f
            else if (listState.firstVisibleItemIndex > 0) 0f
            else (1f - listState.firstVisibleItemScrollOffset / titleHeightPx.toFloat()).coerceIn(0f, 1f)
        }
    }

    // 可折叠标题状态（非顶级页面使用）
    val collapsibleState = rememberCollapsibleHeaderState(headerMode)
    val collapsibleHeightDp = with(density) { collapsibleState.currentOffset.toDp() }

    // 同步滚动位置到全局 TopBar 可见性状态（按路由 key 存储，切换 Tab 时立即生效）
    val topBarState = LocalTopBarState.current

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(listState, isTopPage) {
        if (isTopPage) {
            snapshotFlow { isAtTop }.collect { atTop ->
                topBarState.updateIsShow(routeKey, atTop)
            }
        } else {
            // 非顶级页面始终显示 TopBar
            topBarState.updateIsShow(routeKey, true)
        }
    }

    // 页面出栈时清除该路由的所有 TopBar 状态（包括搜索状态）
    DisposableEffect(routeKey) {
        onDispose {
            topBarState.onPageDispose(routeKey)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.rythmeColors.surface)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isTopPage) Modifier.nestedScroll(collapsibleState.nestedScrollConnection)
                    else Modifier
                )
        ) {
            // 顶部占位空间（为全局 TopBar 留出空间）
            item {
                val isSearchActive = topBarState.isSearchActive(routeKey)
                val isHidden = headerMode == HeaderMode.HIDDEN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            if (isTopPage) topPadding - 104.dp
                            else if (isHidden) { topPadding - 68.dp - if (title.isNullOrEmpty()) 36.dp else 0.dp }
                            else topPadding - ((if (title.isNullOrEmpty()) 36 else 0) * (topPadding.value - statusBarHeight.value - 68) / 104).dp - 56.dp + collapsibleHeightDp
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (!isTopPage) {
                        val hidePlaceholder = rememberSearchAnimating(isSearchActive)
                        if (!hidePlaceholder) {
                            Column(
                                modifier = Modifier
                                    .padding(
                                        start = 21.dp,
                                        end = 21.dp
                                    )
                            ) {
                                if (!title.isNullOrEmpty()) {
                                    // 标题：跟随列表滚动，渐隐
                                    Box(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .alpha(titleAlpha),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.rythmeColors.textColor
                                        )
                                    }
                                }

                                if (!title.isNullOrEmpty() || !isHidden) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                if (!isHidden) {

                                    // 搜索框：通过嵌套滚动折叠/展开
                                    Box(
                                        modifier = Modifier
                                            .height(collapsibleHeightDp)
                                            .clipToBounds()
                                    ) {
                                        SearchPlaceholder(
                                            contentAlpha = ((collapsibleState.searchFraction - 0.9f) / 0.1f).coerceIn(0f, 1f),
                                            onClick = {
                                                topBarState.updateSearchActive(routeKey, true, title ?: "")
                                            }
                                        )
                                    }
                                }

                                if (!title.isNullOrEmpty() || !isHidden) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
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
    headerMode: HeaderMode = HeaderMode.COLLAPSED,
    defaultTitleHidden: Boolean = false,
    mainContent: LazyGridScope.() -> Unit
) {
    val isTopPage = routeKey in ALL_TOP_LEVEL_ROUTES

    val density = LocalDensity.current
    val titleHeightPx = with(density) { 48.dp.roundToPx() }
    val gridState = rememberLazyGridState()

    // 默认隐藏标题：等列表可滚动后滚动到标题刚好隐藏的位置
    if (!isTopPage && defaultTitleHidden) {
        LaunchedEffect(Unit) {
            snapshotFlow { gridState.canScrollForward }
                .first { it }
            gridState.scrollToItem(0, titleHeightPx)
        }
    }

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

    // 非顶级页面标题渐隐：列表滚动 48dp 时完全透明
    val titleAlpha by remember {
        derivedStateOf {
            if (isTopPage) 1f
            else if (gridState.firstVisibleItemIndex > 0) 0f
            else (1f - gridState.firstVisibleItemScrollOffset / titleHeightPx.toFloat()).coerceIn(0f, 1f)
        }
    }

    // 可折叠标题状态（非顶级页面使用）
    val collapsibleState = rememberCollapsibleHeaderState(headerMode)
    val collapsibleHeightDp = with(density) { collapsibleState.currentOffset.toDp() }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 同步滚动位置到全局 TopBar 可见性状态（按路由 key 存储，切换 Tab 时立即生效）
    val topBarState = LocalTopBarState.current

    LaunchedEffect(gridState, isTopPage) {
        if (isTopPage) {
            snapshotFlow { isAtTop }.collect { atTop ->
                topBarState.updateIsShow(routeKey, atTop)
            }
        } else {
            // 非顶级页面始终显示 TopBar
            topBarState.updateIsShow(routeKey, true)
        }
    }

    // 页面出栈时清除该路由的所有 TopBar 状态（包括搜索状态）
    DisposableEffect(routeKey) {
        onDispose {
            topBarState.onPageDispose(routeKey)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.rythmeColors.surface)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridCount),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 21.dp)
                .then(
                    if (!isTopPage) Modifier.nestedScroll(collapsibleState.nestedScrollConnection)
                    else Modifier
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 顶部占位空间（为全局 TopBar 留出空间）
            item(span = { GridItemSpan(maxLineSpan) }) {
                val isSearchActive = topBarState.isSearchActive(routeKey)
                val isHidden = headerMode == HeaderMode.HIDDEN
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            if (isTopPage) topPadding - 104.dp
                            else if (isHidden) { topPadding - 68.dp - if (title.isNullOrEmpty()) 36.dp else 0.dp }
                            else topPadding - ((if (title.isNullOrEmpty()) 36 else 0) * (topPadding.value - statusBarHeight.value - 68) / 104).dp - 56.dp + collapsibleHeightDp
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (!isTopPage) {
                        val hidePlaceholder = rememberSearchAnimating(isSearchActive)
                        if (!hidePlaceholder) {
                            Column {
                                if (!title.isNullOrEmpty()) {
                                    // 标题：跟随列表滚动，渐隐
                                    Box(
                                        modifier = Modifier
                                            .height(36.dp)
                                            .alpha(titleAlpha),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = title,
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.rythmeColors.textColor
                                        )
                                    }
                                }

                                if (!title.isNullOrEmpty() || !isHidden) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }

                                if (!isHidden) {

                                    // 搜索框：通过嵌套滚动折叠/展开
                                    Box(
                                        modifier = Modifier
                                            .height(collapsibleHeightDp)
                                            .clipToBounds()
                                    ) {
                                        SearchPlaceholder(
                                            contentAlpha = ((collapsibleState.searchFraction - 0.9f) / 0.1f).coerceIn(0f, 1f),
                                            onClick = {
                                                topBarState.updateSearchActive(routeKey, true, title ?: "")
                                            }
                                        )
                                    }
                                }

                                if (!title.isNullOrEmpty() || !isHidden) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
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
