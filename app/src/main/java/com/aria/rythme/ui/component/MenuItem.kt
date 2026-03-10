package com.aria.rythme.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.R
import com.aria.rythme.ui.theme.AvatarDefaultBgEnd
import com.aria.rythme.ui.theme.AvatarDefaultBgStart
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule

/**
 * 右侧操作按钮区域，动画由 [HeaderActionsAnimState] 统一编排：
 *
 * - 整体进出场（actions 空↔非空）：虚化渐显/渐隐
 * - 更多按钮显示/隐藏：水滴弹性滑入/滑出
 * - 内容变更（actions 非空→非空）：虚化交叉过渡，容器宽度平滑变化
 */
@Composable
fun AnimatedHeaderActions(
    showMoreButton: Boolean,
    actions: List<Action>,
    skipAnimation: Boolean = false,
    onMoreClick: () -> Unit = {},
    backdrop: Backdrop = LocalBackdrop.current,
) {
    val scope = rememberCoroutineScope()
    val animState = remember { HeaderActionsAnimState(scope) }
    val distance = with(LocalDensity.current) { 60.dp.toPx() }

    // 初始化（仅首次）
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animState.initialize(actions, showMoreButton)
        initialized = true
    }

    // 输入变化时统一驱动动画
    LaunchedEffect(actions, showMoreButton) {
        if (!initialized) return@LaunchedEffect
        animState.update(actions, showMoreButton, skipAnimation, distance)
    }

    // 同步 lambda 引用（不触发动画）
    SideEffect {
        animState.syncActionRefs(actions)
    }

    // 未进入可见阶段，不渲染
    if (animState.phase == HeaderActionsAnimState.Phase.Hidden) return

    val containerColor = MaterialTheme.rythmeColors.bottomBackground

    Box(
        modifier = Modifier
            .height(68.dp)
            .graphicsLayer {
                alpha = animState.overallAlpha.value
                compositingStrategy = CompositingStrategy.ModulateAlpha
            }
            .thenBlur(animState.overallBlur.value),
        contentAlignment = Alignment.Center
    ) {
        // ---- 更多按钮（底层，独立定位，用 padding 控制间距） ----
        if (animState.showMore || showMoreButton) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 60.dp)
                    .size(68.dp)
                    .thenBlur(animState.moreDroplet.blur),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { CircleShape },
                            shadow = { Shadow.Default.copy(radius = 12.dp, offset = DpOffset(0.dp, 0.dp)) },
                            effects = {
                                vibrancy()
                                blur(2f.dp.toPx())
                                lens(24f.dp.toPx(), 32f.dp.toPx())
                            },
                            layerBlock = {
                                translationX = animState.moreDroplet.offsetX
                                scaleX = animState.moreDroplet.scaleX
                                scaleY = animState.moreDroplet.scaleY
                            },
                            onDrawSurface = {
                                drawRect(color = containerColor)
                            }
                        )
                        .size(44.dp)
                        .clickable(
                            interactionSource = null,
                            indication = null
                        ) { onMoreClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more),
                        contentDescription = "更多",
                        tint = MaterialTheme.rythmeColors.textColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ---- 自适应操作按钮容器（顶层，决定容器宽度） ----
        if (animState.displayActions.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(68.dp)
                    .padding(horizontal = 12.dp)
                    .thenBlur(animState.contentBlur.value),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .drawBackdrop(
                            backdrop = backdrop,
                            shape = { ContinuousCapsule },
                            shadow = { Shadow.Default.copy(radius = 12.dp, offset = DpOffset(0.dp, 0.dp)) },
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
                        .animateContentSize(tween(ANIM_DURATION / 2)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    animState.displayActions.forEach { action ->
                        ActionItem(action)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionItem(action: Action) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clickable(
                interactionSource = null,
                indication = null
            ) { action.onClick() },
        contentAlignment = Alignment.Center
    ) {
        when (action) {
            is Action.Icon -> {
                Icon(
                    painter = painterResource(action.iconRes),
                    contentDescription = action.contentDescription,
                    tint = MaterialTheme.rythmeColors.textColor,
                    modifier = Modifier.size(action.iconSize)
                )
            }
            is Action.Avatar -> {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            verticalGradient(
                                colors = listOf(AvatarDefaultBgStart, AvatarDefaultBgEnd)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!action.url.isNullOrEmpty()) {
                        AsyncImage(
                            model = action.url,
                            contentDescription = "avatar",
                            modifier = Modifier.size(44.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        text = if (action.name.isNullOrEmpty()) "R" else action.name.take(2),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---- 返回按钮（独立样式，不属于统一组件） ----

@Composable
fun BackButton(
    backdrop: Backdrop = LocalBackdrop.current,
    visible: Boolean,
    skipAnimation: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.rythmeColors.bottomBackground

    val blur by animateFloatAsState(
        targetValue = if (visible) 0f else 10f,
        animationSpec = if (skipAnimation) tween(0) else tween(ANIM_DURATION),
        label = "backBlur"
    )

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = if (skipAnimation) tween(0) else tween(ANIM_DURATION),
        label = "backAlpha"
    )

    if (alpha > 0.001f) {
        Box(modifier = Modifier
            .size(68.dp)
            .alpha(alpha)
            .thenBlur(blur),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { ContinuousCapsule },
                        shadow = { Shadow.Default.copy(radius = 12.dp, offset = DpOffset(0.dp, 0.dp)) },
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
    }
}

@Composable
fun CloseButton(
    backdrop: Backdrop = LocalBackdrop.current,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.rythmeColors.bottomBackground

    Box(
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
            .size(44.dp)
            .clickable(
                interactionSource = null,
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = "返回",
            tint = MaterialTheme.rythmeColors.textColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

// ---- 工具函数 ----

/** 当模糊半径足够大时才应用 blur，使用 Unbounded 避免矩形裁剪阴影 */
internal fun Modifier.thenBlur(radius: Float): Modifier =
    if (radius > 0.5f) this.blur(radius.dp, BlurredEdgeTreatment.Unbounded) else this
