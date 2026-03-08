package com.aria.rythme.ui.component

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.R
import com.aria.rythme.ui.component.utils.DropletSlideAnimation
import com.aria.rythme.ui.theme.AvatarDefaultBgEnd
import com.aria.rythme.ui.theme.AvatarDefaultBgStart
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule

/**
 * 右侧操作按钮区域，统一管理所有动画：
 *
 * - 整体进出场（actions 空↔非空）：统一虚化渐显/渐隐，子组件不做独立动画
 * - 更多按钮显示/隐藏：水滴弹性滑入/滑出
 * - 内容变更（actions 非空→非空）：虚化旧内容 → 切换 → 清晰新内容，容器宽度平滑过渡
 */
@Composable
fun AnimatedHeaderActions(
    showMoreButton: Boolean,
    actions: List<Action>,
    skipAnimation: Boolean = false,
    onMoreClick: () -> Unit = {},
    backdrop: Backdrop = LocalBackdrop.current,
) {
    val hasContent = actions.isNotEmpty()
    var isInitialized by remember { mutableStateOf(false) }
    var isOverallTransitioning by remember { mutableStateOf(false) }

    // 显示状态：动画完成后才更新，避免内容跳变
    var shouldRender by remember { mutableStateOf(hasContent) }
    var displayActions by remember { mutableStateOf(actions) }
    var showMore by remember { mutableStateOf(showMoreButton) }

    // 动画
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    val scope = rememberCoroutineScope()
    val overallBlur = remember { Animatable(0f) }
    val overallAlpha = remember { Animatable(1f) }
    val contentBlur = remember { Animatable(0f) }
    val moreDroplet = remember { DropletSlideAnimation(animationScope = scope) }
    val distance = with(LocalDensity.current) { 60.dp.toPx() }

    val actionsKey = remember(actions) { actions.contentKey() }

    // --- 1. 整体进出场动画 ---
    LaunchedEffect(hasContent) {
        if (!isInitialized) {
            isInitialized = true
            shouldRender = hasContent
            displayActions = actions
            showMore = showMoreButton
            if (hasContent) {
                overallBlur.snapTo(0f)
                overallAlpha.snapTo(1f)
                if (showMoreButton) moreDroplet.snapToVisible()
            }
            return@LaunchedEffect
        }

        if (hasContent && !shouldRender) {
            // 整体渐显：empty → has content
            isOverallTransitioning = true
            shouldRender = true
            displayActions = actions
            showMore = showMoreButton
            contentBlur.snapTo(0f)
            if (showMoreButton) moreDroplet.snapToVisible()
            if (skipAnimation) {
                overallBlur.snapTo(0f)
                overallAlpha.snapTo(1f)
            } else {
                overallBlur.snapTo(10f)
                overallAlpha.snapTo(0f)
                coroutineScope {
                    launch { overallBlur.animateTo(0f, spring(dampingRatio = 1f, stiffness = 500f)) }
                    launch { overallAlpha.animateTo(1f, tween(ANIM_DURATION)) }
                }
            }
            isOverallTransitioning = false
        } else if (!hasContent && shouldRender) {
            // 整体渐隐：has content → empty
            isOverallTransitioning = true
            if (skipAnimation) {
                overallBlur.snapTo(10f)
                overallAlpha.snapTo(0f)
            } else {
                coroutineScope {
                    launch { overallBlur.animateTo(10f, spring(dampingRatio = 1f, stiffness = 500f)) }
                    launch { overallAlpha.animateTo(0f, tween(ANIM_DURATION)) }
                }
            }
            shouldRender = false
            displayActions = emptyList()
            showMore = false
            isOverallTransitioning = false
        }
    }

    // --- 2. 更多按钮显示/隐藏（仅在非整体过渡时） ---
    LaunchedEffect(showMoreButton) {
        if (!isInitialized || isOverallTransitioning) return@LaunchedEffect

        if (showMoreButton && !showMore) {
            showMore = true
            if (skipAnimation) moreDroplet.snapToVisible()
            else moreDroplet.awaitSlideIn(fromLeft = false, distance = distance)
        } else if (!showMoreButton && showMore) {
            if (!skipAnimation) moreDroplet.awaitSlideOut(toLeft = false, distance = distance)
            showMore = false
        }
    }

    // --- 3. 内容变更动画（仅在非整体过渡时） ---
    LaunchedEffect(actionsKey) {
        if (!isInitialized || isOverallTransitioning) return@LaunchedEffect

        val displayKey = displayActions.contentKey()
        if (actionsKey != displayKey && displayKey.isNotEmpty()) {
            if (skipAnimation) {
                displayActions = actions
            } else {
                contentBlur.animateTo(10f, tween(ANIM_DURATION / 2))
                displayActions = actions
                contentBlur.animateTo(0f, tween(ANIM_DURATION / 2))
            }
        }
    }

    // lambda 引用同步（不触发动画）
    SideEffect {
        if (hasContent && actionsKey == displayActions.contentKey()) {
            displayActions = actions
        }
    }

    if (!shouldRender) return

    Box(modifier = Modifier.alpha(overallAlpha.value)) {
        // ---- 更多按钮（底层，固定在距右侧 60dp 处，不随容器宽度变化移动） ----
        if (showMore) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = (-60).dp)
                    .drawBackdrop(
                        backdrop = backdrop,
                        shape = { CircleShape },
                        effects = {
                            vibrancy()
                            blur(2f.dp.toPx())
                            lens(24f.dp.toPx(), 32f.dp.toPx())
                        },
                        layerBlock = {
                            translationX = moreDroplet.offsetX
                            scaleX = moreDroplet.scaleX
                            scaleY = moreDroplet.scaleY
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
                    modifier = Modifier
                        .size(22.dp)
                        .thenBlur(maxOf(moreDroplet.blur, overallBlur.value))
                )
            }
        }

        // ---- 自适应操作按钮容器（顶层，决定容器宽度） ----
        if (displayActions.isNotEmpty()) {
            val effectiveContentBlur = maxOf(contentBlur.value, overallBlur.value)

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
                    .animateContentSize(tween(ANIM_DURATION / 2)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                displayActions.forEach { action ->
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
                                    modifier = Modifier
                                        .size(action.iconSize)
                                        .thenBlur(effectiveContentBlur)
                                )
                            }
                            is Action.Avatar -> {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            verticalGradient(
                                                colors = listOf(
                                                    AvatarDefaultBgStart,
                                                    AvatarDefaultBgEnd
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.thenBlur(effectiveContentBlur)) {
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
        label = "backAlpha"
    )
    if (blur < 9.99f) {
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
                .thenBlur(blur)
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
                modifier = Modifier
                    .size(20.dp)
            )
        }
    }
}

// ---- 工具函数 ----

/** 当模糊半径足够大时才应用 blur，使用 Unbounded 避免矩形裁剪阴影 */
internal fun Modifier.thenBlur(radius: Float): Modifier =
    if (radius > 0.5f) this.blur(radius.dp, BlurredEdgeTreatment.Unbounded) else this

/** actions 内容指纹：相同 key 表示内容未变，跳过动画 */
private fun List<Action>.contentKey(): String = joinToString(",") { action ->
    when (action) {
        is Action.Icon -> "I:${action.iconRes}"
        is Action.Avatar -> "A:${action.url}:${action.name}"
    }
}
