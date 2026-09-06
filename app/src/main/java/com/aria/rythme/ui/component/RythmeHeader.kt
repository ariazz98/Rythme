package com.aria.rythme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.lerp
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.feature.navigationbar.domain.model.ALL_TOP_LEVEL_ROUTES
import com.aria.rythme.ui.theme.rythmeColors

internal const val ANIM_DURATION = 300

@Composable
fun RythmeHeader(
    routeKey: NavKey,
    config: TopBarConfig,
    skipAnimation: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    val isRoot = routeKey in ALL_TOP_LEVEL_ROUTES
    val search = config.search
    val searchProgress = search?.transition?.value ?: 0f
    val navigationProgress = if (skipAnimation) 1f else config.navigationVisibility?.value ?: 1f
    var lastBackVisible by remember { mutableStateOf(config.showBackButton) }
    val sourceBackVisible = remember(routeKey) { lastBackVisible }
    SideEffect { lastBackVisible = config.showBackButton }
    val backVisibility = androidx.compose.ui.util.lerp(
        if (sourceBackVisible) 1f else 0f,
        if (config.showBackButton) 1f else 0f,
        navigationProgress
    )
    val density = LocalDensity.current
    val statusHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box {
        Box(
            Modifier.fillMaxWidth().height(statusHeight + HeaderLayout.toolbar)
                .background(Brush.verticalGradient(listOf(
                    MaterialTheme.rythmeColors.surface.copy(alpha = 0.5f), Color.Transparent
                )))
        )
        Box(Modifier.statusBarsPadding().fillMaxWidth().height(HeaderLayout.toolbar)) {
            if (searchProgress < 1f) Row(
                Modifier.fillMaxWidth().height(HeaderLayout.toolbar)
                    .padding(horizontal = 9.dp)
                    .glassHdrFadeAndBlur(alpha = { (1f - searchProgress * 3f).coerceIn(0f, 1f) }),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (config.showBackButton) Spacer(Modifier.width(HeaderLayout.toolbar))
                if (isRoot && !config.title.isNullOrEmpty()) Text(
                    text = config.title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.rythmeColors.textColor,
                    modifier = Modifier.weight(1f).padding(start = 12.dp).alpha(navigationProgress)
                ) else Spacer(Modifier.weight(1f))

                AnimatedHeaderActions(
                    routeKey = routeKey,
                    auxiliaryActions = config.auxiliaryActions,
                    actions = config.actions,
                    skipAnimation = skipAnimation,
                    navigationProgress = { if (skipAnimation) 1f else config.navigationVisibility?.value ?: 1f },
                    enabled = search?.active != true
                )
            }

            // 返回按钮独立叠放，退出时不挤动一级页面标题。
            if (searchProgress < 1f) Box(
                Modifier.padding(start = 9.dp)
                    .glassHdrFadeAndBlur(alpha = { (1f - searchProgress * 3f).coerceIn(0f, 1f) })
            ) {
                BackButton(
                    visible = config.showBackButton,
                    skipAnimation = skipAnimation,
                    visibilityFraction = backVisibility,
                    onClick = { if (config.showBackButton && search?.active != true) onBackClick() }
                )
            }

            if (search != null && (search.active || searchProgress > 0f)) {
                // 起点取自页面内实际搜索框；44dp 表面在 68dp 输入行内垂直居中。
                val restY = if (search.origin.isEmpty) HeaderLayout.toolbar else
                    with(density) { search.origin.top.toDp() } - statusHeight - 12.dp
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
                        onClose = search::close
                    )
                }
            }
        }
    }
}
