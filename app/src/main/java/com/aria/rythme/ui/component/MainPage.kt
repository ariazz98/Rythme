package com.aria.rythme.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.LocalInnerPadding
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun MainListPage(
    title: String? = null,
    routeKey: NavKey,
    autoHide: Boolean = true,
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

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.rythmeColors.surface)
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

        if (!title.isNullOrEmpty()) {
            // 标题
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.rythmeColors.textColor,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 21.dp, vertical = 8.dp)
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

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.rythmeColors.surface)
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

        if (!title.isNullOrEmpty()) {
            // 标题
            Text(
                text = title,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.rythmeColors.textColor,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(horizontal = 21.dp, vertical = 8.dp)
                    .alpha(headerAlpha)
            )
        }
    }
}
