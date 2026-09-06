package com.aria.rythme.ui.component

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.ui.theme.rythmeColors

/** 页面持有查询与激活状态；Header 只读取它来渲染输入与动画。 */
@Stable
class PageSearchState(query: String = "", active: Boolean = false) {
    var query by mutableStateOf(query)
    var active by mutableStateOf(active)
        private set
    val transition = Animatable(if (active) 1f else 0f)
    var bounds = Rect.Zero
    var origin by mutableStateOf(Rect.Zero)
        private set

    fun open() {
        origin = bounds
        active = true
    }

    fun close() {
        active = false
        query = ""
    }

    fun matches(vararg values: String?): Boolean = query.isBlank() ||
        values.any { it?.contains(query.trim(), ignoreCase = true) == true }

    companion object {
        val Saver = listSaver<PageSearchState, Any>(
            save = { listOf(it.query, it.active) },
            restore = { PageSearchState(it[0] as String, it[1] as Boolean) }
        )
    }
}

@Composable
fun rememberPageSearchState(): PageSearchState = rememberSaveable(saver = PageSearchState.Saver) { PageSearchState() }

/** Header 与 List/Grid 共用的尺寸；不再用 Scaffold 的占位高度反推页面结构。 */
internal object HeaderLayout {
    val toolbar = 68.dp
    val title = 36.dp
    val gap = 6.dp
    val search = 56.dp
    val horizontalPadding = 21.dp
    const val navigationDuration = 400

    fun contentHeight(hasTitle: Boolean, hasSearch: Boolean, searchHeight: Dp): Dp =
        (if (hasTitle) title else 0.dp) +
            (if (hasTitle || hasSearch) gap * 2 else 0.dp) +
            (if (hasSearch) searchHeight else 0.dp)
}

@Composable
internal fun rememberPageHeader(
    routeKey: NavKey,
    title: String?,
    config: TopBarConfig?,
    mode: HeaderMode,
    search: PageSearchState
): CollapsibleHeaderState {
    val topBar = LocalTopBarState.current
    val isRoot = routeKey in ALL_TOP_LEVEL_ROUTES
    val collapse = rememberCollapsibleHeaderState(if (isRoot) HeaderMode.HIDDEN else mode, HeaderLayout.search)
    val visibility = LocalNavAnimatedContentScope.current.transition.animateFloat(
        transitionSpec = { tween(HeaderLayout.navigationDuration, easing = FastOutSlowInEasing) },
        label = "headerNavigationVisibility"
    ) { if (it == EnterExitState.Visible) 1f else 0f }
    SideEffect {
        topBar.updateConfig(routeKey, (config ?: topBar.getConfig(routeKey)).copy(
            title = title,
            search = if (isRoot || mode == HeaderMode.HIDDEN) null else search,
            navigationVisibility = visibility
        ))
    }
    LaunchedEffect(search.active) {
        search.transition.animateTo(if (search.active) 1f else 0f, tween(ANIM_DURATION))
    }
    return collapse
}

/** 同一份页内标题/搜索，分别放入 LazyColumn 或 LazyGrid 的首项。 */
@Composable
internal fun PageHeaderContent(
    title: String?,
    topPadding: Dp,
    isRoot: Boolean,
    mode: HeaderMode,
    search: PageSearchState,
    collapse: CollapsibleHeaderState,
    titleAlpha: Float,
    horizontalPadding: Dp
) {
    val density = LocalDensity.current
    val searchHeight = with(density) { collapse.currentOffset.toDp() }
    val extra = if (isRoot) 0.dp else HeaderLayout.contentHeight(!title.isNullOrEmpty(), mode != HeaderMode.HIDDEN, searchHeight)
    val progress = if (mode == HeaderMode.HIDDEN || isRoot) 0f else search.transition.value
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(topPadding))
        Box(Modifier.fillMaxWidth().height(extra * (1f - progress))) {
            if (!isRoot) Column(Modifier.padding(horizontal = horizontalPadding).alpha(1f - progress)) {
                if (!title.isNullOrEmpty()) Text(
                    text = title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.rythmeColors.textColor,
                    modifier = Modifier.height(HeaderLayout.title).alpha(titleAlpha)
                        .graphicsLayer { translationY = -HeaderLayout.toolbar.toPx() * progress }
                )
                if (!title.isNullOrEmpty() || mode != HeaderMode.HIDDEN) Spacer(Modifier.height(HeaderLayout.gap))
                if (mode != HeaderMode.HIDDEN) Box(Modifier.height(searchHeight).clipToBounds()) {
                    SearchPlaceholder(
                        onClick = search::open,
                        contentAlpha = ((collapse.searchFraction - 0.9f) / 0.1f).coerceIn(0f, 1f),
                        // 激活期间只绘制顶部的真实输入框，避免玻璃再次采样页内占位文字造成重影。
                        modifier = Modifier.alpha(if (search.active || progress > 0f) 0f else 1f)
                            .onGloballyPositioned { if (!search.active) search.bounds = it.boundsInRoot() }
                    )
                }
                if (!title.isNullOrEmpty() || mode != HeaderMode.HIDDEN) Spacer(Modifier.height(HeaderLayout.gap))
            }
        }
    }
}
