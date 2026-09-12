package com.aria.rythme.feature.player.presentation

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.aria.rythme.ui.theme.rythmeColors
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalWindowInfo
import com.aria.rythme.core.utils.rememberScreenCornerRadiusDp
import com.aria.rythme.ui.component.utils.BottomBarMetrics
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

/** 两端用实际动画尺寸计算同一轮廓，不用各自的可见性动画猜圆角进度。 */
@Composable
internal fun rememberPlayerContainerShape(): Shape {
    val screenHeight = LocalWindowInfo.current.containerDpSize.height
    val screenRadius = rememberScreenCornerRadiusDp()
    return remember(screenHeight, screenRadius) {
        val corner = object : androidx.compose.foundation.shape.CornerSize {
            override fun toPx(shapeSize: Size, density: Density): Float =
                with(density) {
                    val size = shapeSize
                    val capsuleHeight = maxOf(BottomBarMetrics.CompactHeight,
                        BottomBarMetrics.ExpandedMiniHeight).dp.toPx()
                    if (size.height <= capsuleHeight) {
                        size.minDimension / 2f
                    } else {
                        val progress = ((size.height - capsuleHeight) /
                            (screenHeight.toPx() - capsuleHeight).coerceAtLeast(1f)).coerceIn(0f, 1f)
                        val radius = capsuleHeight / 2f +
                            (screenRadius.toPx() - capsuleHeight / 2f) * progress
                        radius.coerceIn(0f, size.minDimension / 2f)
                    }
                }
        }
        ContinuousRoundedRectangle(corner, corner, corner, corner)
    }
}

// 逐帧值只使读取它的封面/材质失效，不重组整个页面与背景录制源。
internal val LocalPlayerOverlayProgress = compositionLocalOf { 0f }
internal class PlayerSurfaceOpacity {
    private val covered = androidx.compose.runtime.mutableStateOf(false)
    var value: Boolean
        get() = covered.value
        set(value) { covered.value = value }
    var animation: androidx.compose.runtime.State<Float>? = null
}
internal val LocalPlayerDismissMotion = androidx.compose.runtime.staticCompositionLocalOf<androidx.compose.runtime.MutableState<PlayerDismissMotion>> {
    error("Player dismissal motion must be provided")
}

/** 一次手势的归一化初速度供容器、封面及材质共用，避免各自接续不同步。 */
internal class PlayerDismissMotion(velocity: Float = 0f, remainingTravel: Float = 1f) {
    private val speed = velocity.coerceAtLeast(0f) / remainingTravel.coerceAtLeast(1f)
    val durationMs = if (speed > 0f)
        minOf(PlayerOverlayMotion.CollapseMs, (3000f / speed).toInt().coerceAtLeast(1))
    else PlayerOverlayMotion.CollapseMs
    private val slope = (speed * durationMs / 1000f).coerceIn(0f, 3f)
    val easing = if (speed > 0f) CubicBezierEasing(.28f, .28f * slope, .25f, 1f)
        else PlayerOverlayMotion.CollapseEasing
}

@Composable
internal fun playerOverlayBounds(): BoundsTransform {
    val dismissal = LocalPlayerDismissMotion.current.value
    return remember(dismissal) {
        BoundsTransform { start, end ->
            if (end.height > start.height) tween(PlayerOverlayMotion.ExpandMs, easing = PlayerOverlayMotion.ExpandEasing)
            else tween(dismissal.durationMs, easing = dismissal.easing)
        }
    }
}
internal val LocalPlayerSurfaceOpaque = androidx.compose.runtime.staticCompositionLocalOf<PlayerSurfaceOpacity> {
    error("Player surface visibility must be provided")
}

internal fun shouldDrawMiniPlayerGlass(playerVisible: Boolean, surfaceOpaque: Boolean) =
    !playerVisible || !surfaceOpaque

@Composable
internal fun playerCoverBackground(): Color = lerp(
    MaterialTheme.rythmeColors.miniCoverBg, Color(0xFF606063), LocalPlayerOverlayProgress.current)

@Composable
internal fun playerCoverIcon(): Color = lerp(
    MaterialTheme.rythmeColors.miniCoverIcon, Color(0xFF737376), LocalPlayerOverlayProgress.current)

/** 全屏播放器外层过渡；与大封面/歌词/队列内部切换分开。 */
internal object PlayerOverlayMotion {
    const val ExpandMs = 450
    const val BackgroundExpandMs = 180 // 展开前 40% 完成遮挡，不改变几何/封面进度。
    const val CollapseMs = 400
    // 展开较早完成主要位移，收起保留更充分的中后段运动。
    val ExpandEasing = CubicBezierEasing(.16f, 0f, .12f, 1f)
    val CollapseEasing = CubicBezierEasing(.28f, 0f, .25f, 1f)
    fun easing(expanding: Boolean) = if (expanding) ExpandEasing else CollapseEasing
    val bounds = BoundsTransform { start, end ->
        val expanding = end.height > start.height
        tween(if (expanding) ExpandMs else CollapseMs, easing = easing(expanding))
    }
    fun contentAlpha(progress: Float, expanding: Boolean): Float =
        if (expanding) ((progress - .12f) / .7f).coerceIn(0f, 1f)
        else (progress / .5f).coerceIn(0f, 1f)
}
