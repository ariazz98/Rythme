package com.aria.rythme.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.ui.theme.rythmeColors
import kotlinx.coroutines.delay

internal const val ANIM_DURATION = 300

@Composable
fun RythmeHeader(
    isShow: Boolean,
    config: TopBarConfig,
    isSearchActive: Boolean = false,
    searchTitle: String = "",
    onSearchClose: () -> Unit = {},
    skipAnimation: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    // isShow && !isSearchActive控制顶部按钮区域的显隐
    val headerAlpha by animateFloatAsState(
        targetValue = if (isShow && !isSearchActive) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "headerAlpha"
    )

    val titleTranslationY by animateDpAsState(
        targetValue = if (isSearchActive) 0.dp else 68.dp,
        animationSpec = tween(durationMillis = ANIM_DURATION),
        label = "titleTranslationY"
    )

    val titleAlpha by animateFloatAsState(
        targetValue = if (isSearchActive) 0f else 1f,
        animationSpec = tween(durationMillis = ANIM_DURATION),
        label = "titleAlpha"
    )

    val searchBarTranslationY by animateDpAsState(
        targetValue = if (isSearchActive) 0.dp else 104.dp,
        animationSpec = tween(durationMillis = ANIM_DURATION),
        label = "searchBarTranslationY"
    )

    val topBarHeight by animateDpAsState(
        targetValue = if (isSearchActive) 68.dp else 172.dp,
        animationSpec = tween(durationMillis = ANIM_DURATION),
        label = "topBarHeight"
    )

    Box {
        // 渐变背景，固定不变
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.rythmeColors.surface.copy(0.5f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Header高度固定
        Box(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .height(topBarHeight)
        ) {

            // 顶部按钮区域，高度固定，在折叠时快速渐隐
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 9.dp)
                    .alpha(headerAlpha),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BackButton(
                    visible = config.showBackButton,
                    skipAnimation = skipAnimation,
                    onClick = onBackClick
                )

                Spacer(modifier = Modifier.weight(1f))

                AnimatedHeaderActions(
                    showMoreButton = config.showMoreButton,
                    actions = config.actions,
                    skipAnimation = skipAnimation
                )
            }

            // 搜索框active时可见，立即替换content中对应的内容
            if (isSearchActive) {
                Box(
                    modifier = Modifier
                        .padding(start = 21.dp, end = 21.dp, top = titleTranslationY, bottom = 7.dp)
                        .fillMaxWidth()
                        .height(36.dp)
                        .alpha(titleAlpha)
                ) {
                    Text(
                        text = searchTitle,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.rythmeColors.textColor
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(start = 21.dp, end = 21.dp, top = searchBarTranslationY)
                        .fillMaxWidth()
                        .height(68.dp)
                ) {
                    HeaderSearchBar(
                        active = isSearchActive,
                        onClose = onSearchClose,
                    )
                }
            }
        }
    }
}
