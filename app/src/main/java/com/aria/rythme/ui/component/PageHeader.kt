package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.ui.theme.rythmeColors

/** 页面持有查询与激活状态；Header 只读取它来渲染输入与动画。 */
@Stable
class PageSearchState private constructor(
    active: Boolean,
    private val readQuery: () -> String,
    private val writeQuery: (String) -> Unit
) {
    private constructor(active: Boolean, query: MutableState<String>) :
        this(active, { query.value }, { query.value = it })

    constructor(query: String = "", active: Boolean = false) : this(active, mutableStateOf(query))

    var query: String
        get() = readQuery()
        set(value) = writeQuery(value)
    var active by mutableStateOf(active)
        private set
    val transition = Animatable(if (active) 1f else 0f)
    var bounds = Rect.Zero
    var origin by mutableStateOf(Rect.Zero)
        private set

    fun open() {
        if (active) return
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
        internal fun controlled(active: Boolean, readQuery: () -> String, writeQuery: (String) -> Unit) =
            PageSearchState(active, readQuery, writeQuery)
        val Saver = listSaver<PageSearchState, Any>(
            save = { listOf(it.query, it.active) },
            restore = { PageSearchState(it[0] as String, it[1] as Boolean) }
        )
    }
}

@Composable
fun rememberPageSearchState(): PageSearchState = rememberSaveable(saver = PageSearchState.Saver) { PageSearchState() }

/** ViewModel 仍拥有查询业务；输入立即回显，避免跨 entry 的 Flow 回传晚一帧造成快速输入丢字。 */
@Composable
fun rememberPageSearchState(query: String, onQueryChange: (String) -> Unit): PageSearchState {
    // 同一轮外部值下立即接续输入；外部查询更新（包括清空）后使用新的权威值。
    val inputQuery = remember(query) { mutableStateOf(query) }
    val currentQuery = rememberUpdatedState(inputQuery)
    val currentChange = rememberUpdatedState(onQueryChange)
    fun create(active: Boolean) = PageSearchState.controlled(active, { currentQuery.value.value }, {
        currentQuery.value.value = it
        currentChange.value(it)
    })
    return rememberSaveable(saver = listSaver<PageSearchState, Boolean>(
        save = { listOf(it.active) },
        restore = { create(it[0]) }
    )) { create(false) }
}

/** Header 与 List/Grid 共用的尺寸；不再用 Scaffold 的占位高度反推页面结构。 */
internal object HeaderLayout {
    val toolbar = 68.dp
    val title = 36.dp
    val gap = 6.dp
    val search get() = HeaderSearchLayout.inlineHeight
    val horizontalPadding = 21.dp
    val titleScrollThreshold = title + gap * 2
    const val navigationDuration = 400

    fun contentHeight(hasTitle: Boolean, hasSearch: Boolean, searchHeight: Dp): Dp =
        (if (hasTitle) title else 0.dp) +
            (if (hasTitle || hasSearch) gap * 2 else 0.dp) +
            (if (hasSearch) searchHeight else 0.dp)
}

/** 每个实际导航 entry 自己的滚动呈现；不把滚动事件或列表状态放到全局单例。 */
@Stable
internal class HeaderScrollState {
    var atTop by mutableStateOf(true)
    var firstVisibleItemIndex by mutableIntStateOf(0)
    var firstVisibleItemScrollOffsetDp by mutableFloatStateOf(0f)
    val chrome = Animatable(0f)

    fun shouldShowChrome(isRoot: Boolean, hasStandardTitle: Boolean): Boolean {
        if (atTop) return false
        if (isRoot) return true
        return hasStandardTitle && (firstVisibleItemIndex > 0 ||
            firstVisibleItemScrollOffsetDp >= HeaderLayout.titleScrollThreshold.value)
    }
}

/** 标准大标题与 Header 小标题共用一份交接进度；不测量文字位置。 */
@Composable
private fun ContentHeaderTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    fontWeight: FontWeight = FontWeight.ExtraBold
) {
    val entry = LocalTopBarEntry.current
    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = MaterialTheme.rythmeColors.textColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.graphicsLayer { alpha = 1f - entry.scroll.chrome.value }
            .semantics { if (entry.scroll.chrome.value >= .999f) hideFromAccessibility() }
    )
}

@Composable
internal fun rememberPageHeader(
    title: String?,
    config: TopBarConfig?,
    mode: HeaderMode,
    search: PageSearchState,
    hasSearch: Boolean
): CollapsibleHeaderState {
    val entry = LocalTopBarEntry.current
    val isRoot = entry.route in ALL_TOP_LEVEL_ROUTES
    val collapse = rememberCollapsibleHeaderState(if (isRoot) HeaderMode.HIDDEN else mode, HeaderLayout.search)
    SideEffect {
        entry.update(
            (config ?: TopBarConfig(showBackButton = !isRoot)).copy(title = title ?: config?.title),
            search = if (hasSearch) search else null
        )
    }
    LaunchedEffect(search.active) {
        if (search.active) collapse.cancelSettling()
        search.transition.animateTo(if (search.active) 1f else 0f, tween(HeaderSearchMotion.activationDuration))
    }
    val showChrome = entry.scroll.shouldShowChrome(isRoot, hasStandardTitle = !title.isNullOrEmpty())
    LaunchedEffect(showChrome) {
        entry.scroll.chrome.animateTo(if (showChrome) 1f else 0f, tween(if (isRoot) 120 else 150))
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
    horizontalPadding: Dp,
    hasSearch: Boolean,
    rootSearchSpacing: Dp = 0.dp
) {
    val density = LocalDensity.current
    val searchHeight = with(density) { collapse.currentOffset.toDp() }
    val extra = if (isRoot) 0.dp else HeaderLayout.contentHeight(!title.isNullOrEmpty(), mode != HeaderMode.HIDDEN, searchHeight)
    val progress = if (hasSearch) search.transition.value else 0f
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(topPadding))
        if (isRoot && hasSearch) {
            // 根页不采用下拉折叠；激活时只交接这一行，保留原来的网格间距。
            Box(Modifier.fillMaxWidth().height((rootSearchSpacing + HeaderSearchLayout.surfaceHeight) * (1f - progress))) {
                SearchPlaceholder(
                    onClick = search::open,
                    enabled = !search.active && progress == 0f,
                    value = search.query,
                    hint = androidx.compose.ui.res.stringResource(com.aria.rythme.R.string.search_hint),
                    verticalPadding = 0.dp,
                    modifier = Modifier.padding(top = rootSearchSpacing, start = horizontalPadding, end = horizontalPadding)
                        .alpha(if (search.active || progress > 0f) 0f else 1f)
                        .onGloballyPositioned { if (!search.active && progress == 0f) search.bounds = it.boundsInRoot() }
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(extra * (1f - progress))) {
            if (!isRoot) Column(Modifier.padding(horizontal = horizontalPadding).alpha(1f - progress)) {
                if (!title.isNullOrEmpty()) ContentHeaderTitle(
                    text = title,
                    modifier = Modifier.height(HeaderLayout.title)
                        .graphicsLayer { translationY = -HeaderLayout.toolbar.toPx() * progress }
                )
                if (!title.isNullOrEmpty() || mode != HeaderMode.HIDDEN) Spacer(Modifier.height(HeaderLayout.gap))
                if (mode != HeaderMode.HIDDEN) Box(Modifier.height(searchHeight).clipToBounds()) {
                    SearchPlaceholder(
                        onClick = search::open,
                        contentAlpha = HeaderSearchMotion.placeholderAlpha(collapse.searchFraction),
                        enabled = !search.active && progress == 0f && HeaderSearchMotion.placeholderInteractive(collapse.searchFraction),
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
