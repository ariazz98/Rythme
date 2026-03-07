package com.aria.rythme.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.R
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule

@Composable
fun MenuItem(
    backdrop: Backdrop = LocalBackdrop.current,
    iconRes: Int,
    iconSize: Dp = 28.dp,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    Box(modifier = Modifier
        .drawBackdrop(
            backdrop = backdrop,
            shape = { CircleShape },
            effects = {
                vibrancy()
                blur(2f.dp.toPx())
                lens(24f.dp.toPx(), 32f.dp.toPx())
            },
            onDrawSurface = {
                drawRect(color = containerColor)
            }
        )
        .size(44.dp)
        .clickable(
            interactionSource = null,
            indication = null
        ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = "",
            tint = MaterialTheme.rythmeColors.textColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** 渲染单个操作项 */
@Composable
fun SingleActionItem(item: HeaderActionItem) {
    when (item) {
        is HeaderActionItem.Icon -> MenuItem(
            iconRes = item.iconRes,
            iconSize = item.iconSize,
            onClick = item.onClick
        )
        is HeaderActionItem.Avatar -> AvatarItem(
            url = item.url,
            name = item.name,
            onClick = item.onClick
        )
    }
}

/** 返回按钮（固定样式） */
@Composable
fun BackButton(
    backdrop: Backdrop = LocalBackdrop.current,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    Box(
        modifier = Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { CircleShape },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(24f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(color = containerColor)
                }
            )
            .size(44.dp)
            .clickable(
                interactionSource = null,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_back),
            contentDescription = "返回",
            tint = MaterialTheme.rythmeColors.textColor,
            modifier = Modifier.size(20.dp)
        )
    }
}

/** 组合按钮：多个图标共享一个胶囊容器 */
@Composable
fun SegmentedActionGroup(
    actions: List<HeaderActionItem.Icon>,
    backdrop: Backdrop = LocalBackdrop.current,
) {
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    Row(
        modifier = Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(24f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(color = containerColor)
                }
            )
            .height(44.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(36.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { action.onClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(action.iconRes),
                    contentDescription = action.contentDescription,
                    tint = MaterialTheme.rythmeColors.textColor,
                    modifier = Modifier.size(action.iconSize)
                )
            }
        }
    }
}