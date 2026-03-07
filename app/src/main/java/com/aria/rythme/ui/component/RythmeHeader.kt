package com.aria.rythme.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun RythmeHeader(
    isShow: Boolean,
    config: TopBarConfig,
    onBackClick: () -> Unit = {}
) {
    val headerAlpha by animateFloatAsState(
        targetValue = if (isShow) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "headerAlpha"
    )

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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 21.dp, vertical = 8.dp)
            .alpha(headerAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 左侧：返回按钮
        if (config.showBackButton) {
            BackButton(onClick = onBackClick)
        }

        Spacer(modifier = Modifier.weight(1f))

        // 右侧：按钮组
        config.actions.forEachIndexed { index, group ->
            if (index > 0) Spacer(modifier = Modifier.width(16.dp))
            when (group) {
                is ActionGroup.Single -> SingleActionItem(group.item)
                is ActionGroup.Segmented -> SegmentedActionGroup(group.items)
            }
        }
    }
}