package com.aria.rythme.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.ui.theme.rythmeColors
import com.aria.rythme.LocalBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop

internal const val ANIM_DURATION = 300

@Composable
internal fun RythmeHeader(
    entry: TopBarEntry,
    profileName: String,
    enabled: Boolean = true,
    skipAnimation: Boolean = false,
    onBackClick: () -> Unit = {},
    onAvatarClick: () -> Unit = {},
    isRoot: Boolean = entry.route in ALL_TOP_LEVEL_ROUTES,
    showDivider: Boolean = true,
    profileAvatar: String? = null
) {
    val config = requireNotNull(entry.config)
    fun bindAvatar(actions: List<Action>) = actions.map { action ->
        if (action is Action.Avatar && action.key == "avatar") action.copy(name = profileName, url = profileAvatar, contentDescription = "打开个人面板", onClick = action.onClick ?: onAvatarClick) else action
    }
    val search = entry.search
    val searchProgress = search?.transition?.value ?: 0f
    val navigationProgress = if (skipAnimation) 1f else entry.navigationVisibility.value
    var lastBackVisible by remember { mutableStateOf(config.showBackButton) }
    val sourceBackVisible = remember(entry.key) { lastBackVisible }
    SideEffect { lastBackVisible = config.showBackButton }
    val backVisibility = androidx.compose.ui.util.lerp(
        if (sourceBackVisible) 1f else 0f,
        if (config.showBackButton) 1f else 0f,
        navigationProgress
    )
    val density = LocalDensity.current
    val referenceScale = TopBarComponentMetrics.referenceScale(
        LocalWindowInfo.current.containerSize.width / density.density
    )
    val statusHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val pageBackdrop = LocalBackdrop.current
    val hdr = LocalGlassHdr.current
    val frostedLayer = key(hdr.enabled, hdr.generation) { rememberLayerBackdrop() }
    val buttonBackdrop = rememberCombinedBackdrop(pageBackdrop, frostedLayer)
    val surfaceColor = MaterialTheme.rythmeColors.surface

    Box {
        // 只记录背景，不把标题和按钮录进去；按钮采样“页面＋当前模糊遮罩”，避免容器内突然变清。
        Box(
            Modifier.fillMaxWidth().height(statusHeight + HeaderLayout.toolbar)
                .layerBackdrop(frostedLayer)
                .glassHdrFadeAndBlur(alpha = { entry.scroll.chrome.value * navigationProgress })
                .drawBackdrop(
                    backdrop = pageBackdrop,
                    shape = { RectangleShape },
                    effects = { headerProgressiveBlur() },
                    highlight = null,
                    shadow = null,
                    innerShadow = null,
                    onDrawSurface = { drawHeaderBackdropSurface(surfaceColor, showDivider) }
                )
        )
        CompositionLocalProvider(LocalBackdrop provides buttonBackdrop) {
        Box(Modifier.statusBarsPadding().fillMaxWidth().height(HeaderLayout.toolbar)) {
            if (searchProgress < 1f) Row(
                Modifier.fillMaxWidth().height(HeaderLayout.toolbar)
                    .offset(y = (-1).dp)
                    .padding(horizontal = (TopBarComponentMetrics.rowPadding * referenceScale).dp)
                    .glassHdrFadeAndBlur(alpha = { HeaderSearchMotion.chromeAlpha(searchProgress) }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (config.showBackButton) Spacer(Modifier.width(HeaderLayout.toolbar))
                if (isRoot && !config.title.isNullOrEmpty()) TopBarTitle(
                    text = config.title,
                    referenceScale = referenceScale,
                    modifier = Modifier.weight(1f)
                        .padding(start = (TopBarComponentMetrics.titlePadding * referenceScale).dp)
                        .alpha(navigationProgress)
                ) else Spacer(Modifier.weight(1f))

                AnimatedHeaderActions(
                    sourceKey = entry.key,
                    auxiliaryActions = bindAvatar(config.auxiliaryActions),
                    actions = bindAvatar(config.actions),
                    referenceScale = referenceScale,
                    skipAnimation = skipAnimation,
                    navigationProgress = { if (skipAnimation) 1f else entry.navigationVisibility.value },
                    enabled = enabled && search?.active != true
                )
            }

            if (!isRoot && !config.title.isNullOrEmpty()) CompactHeaderTitle(
                title = config.title,
                entry = entry,
                referenceScale = referenceScale,
                opacity = { entry.scroll.chrome.value * navigationProgress * HeaderSearchMotion.chromeAlpha(searchProgress) }
            )

            // 返回按钮独立叠放，退出时不挤动一级页面标题。
            if (searchProgress < 1f) Box(
                Modifier.padding(start = ((TopBarComponentMetrics.TrailingInset + TopBarComponentMetrics.SurfaceHeight / 2f) * referenceScale - HeaderLayout.toolbar.value / 2f).coerceAtLeast(0f).dp)
                    .offset(y = (-1).dp)
                    .glassHdrFadeAndBlur(alpha = { HeaderSearchMotion.chromeAlpha(searchProgress) })
            ) {
                BackButton(
                    visible = config.showBackButton,
                    referenceScale = referenceScale,
                    skipAnimation = skipAnimation,
                    visibilityFraction = backVisibility,
                    onClick = { if (enabled && config.showBackButton && search?.active != true) onBackClick() }
                )
            }

            if (search != null && (search.active || searchProgress > 0f)) {
                // 起点取自页面内实际搜索框；44dp 表面在 68dp 输入行内垂直居中。
                val restY = if (search.origin.isEmpty) HeaderLayout.toolbar else
                    with(density) { search.origin.top.toDp() } - statusHeight - HeaderSearchLayout.toolbarSurfaceInset
                Box(
                    Modifier.padding(start = HeaderLayout.horizontalPadding)
                        .offset(y = lerp(restY, 0.dp, searchProgress))
                        .fillMaxWidth().height(HeaderLayout.toolbar)
                ) {
                    HeaderSearchBar(
                        active = search.active,
                        value = search.query,
                        onValueChange = { search.query = it },
                        progress = searchProgress,
                        clearable = isRoot,
                        hint = androidx.compose.ui.res.stringResource(if (isRoot) com.aria.rythme.R.string.search_hint else com.aria.rythme.R.string.title_search),
                        onClose = search::close
                    )
                }
            }
        }
        }
    }
}

/** 优先屏幕居中；长标题或 2＋1 操作挤占空间时，仅在实际可用区域内避让、截断。 */
@Composable
private fun CompactHeaderTitle(title: String, entry: TopBarEntry, referenceScale: Float, opacity: () -> Float) {
    val config = requireNotNull(entry.config)
    val actionWidth = headerActionLayout(config.auxiliaryActions, config.actions).width
    val leading = (TopBarComponentMetrics.TrailingInset + if (config.showBackButton) TopBarComponentMetrics.SurfaceHeight + 8f else 0f) * referenceScale
    val trailing = (if (actionWidth == 0f) TopBarComponentMetrics.TrailingInset else
        actionWidth + TopBarComponentMetrics.rowPadding - TopBarComponentMetrics.GroupGap / 2f + 8f) * referenceScale
    Layout(
        modifier = Modifier.fillMaxSize().offset(y = (-1).dp),
        content = {
            Text(
                text = title,
                fontSize = (17f * referenceScale).sp,
                lineHeight = (22f * referenceScale).sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.rythmeColors.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .glassHdrFadeAndBlur(alpha = opacity)
                    .semantics { if (opacity() <= .001f) hideFromAccessibility() }
            )
        }
    ) { measurables, constraints ->
        val left = leading.dp.roundToPx()
        val right = trailing.dp.roundToPx()
        val available = (constraints.maxWidth - left - right).coerceAtLeast(0)
        val text = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = available))
        val maxX = (constraints.maxWidth - right - text.width).coerceAtLeast(left)
        val x = ((constraints.maxWidth - text.width) / 2).coerceIn(left, maxX)
        layout(constraints.maxWidth, constraints.maxHeight) { text.place(x, (constraints.maxHeight - text.height) / 2) }
    }
}
