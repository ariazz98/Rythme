package com.aria.rythme.ui.component.utils

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.animation.core.FloatAnimationSpec
import androidx.compose.animation.core.FloatTweenSpec
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.lerp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/** 按参考的同屏宽视觉比例校准；不对整组及其文字/图标额外缩放。 */
internal object BottomBarMetrics {
    const val ExpandedMiniHeight = 46f
    const val ExpandedTabHeight = 59f
    // 选择器上下仍各留 3.5dp，采样表面和可见选择器共用此高度。
    const val ExpandedSelectorHeight = ExpandedTabHeight - 7f
    // 外层液滴按当前 52dp 表面校准：折射范围 7.9dp、强度 8.5dp。
    // 参数随表面高度同比缩放，与隐藏胶囊材质独立。
    const val SelectorRefractionHeightRatio = 7.9f / ExpandedSelectorHeight
    const val SelectorRefractionAmountRatio = 8.5f / ExpandedSelectorHeight
    // 收起表面乘预缩放后为 47.5 × 0.95 ≈ 45.1dp。
    const val CompactHeight = 47.5f
    const val VerticalGap = 7f
    const val CompactGap = 14f
    const val CompactScale = 0.95f
}

/** 两种着色的位置共用整组按压变换；宽度和增长量必须使用相同单位。 */
internal fun bottomTabPressScale(width: Float, widthGrowth: Float, progress: Float): Float =
    if (width > 0f) lerp(1f, 1f + widthGrowth / width, progress) else 1f

/** 围绕图标+标题组合中心放大；抵消父级缩放，最终强调倍率为 1.15。 */
internal fun bottomTabEmphasisScale(progress: Float, inheritedScale: Float = 1f): Float =
    lerp(1f, 1.15f, progress) / inheritedScale

/** 与 navigationBarsPadding 配合：至少 21dp 视觉留白，导航区之外至少再留 8dp。 */
internal fun bottomBarBottomSpacing(navigationInset: Float): Float =
    maxOf(8f, 21f - navigationInset.coerceAtLeast(0f))

/** 正文始终为展开态预留空间；动画中的实际高度只用于底栏自身布局。单位为 dp。 */
internal fun expandedBottomBarContentInset(navigationInset: Float): Float =
    BottomBarMetrics.ExpandedMiniHeight + BottomBarMetrics.VerticalGap +
        BottomBarMetrics.ExpandedTabHeight + bottomBarBottomSpacing(navigationInset) +
        navigationInset.coerceAtLeast(0f)

/** 从真实表面位置推导占位高度，不再给整组高度另设一条动画。所有输入使用同一单位。 */
internal class BottomBarGeometry(
    val tabHeight: Float,
    val miniHeight: Float,
    miniExpansion: Float,
    expandedTabHeight: Float = BottomBarMetrics.ExpandedTabHeight,
    expandedMiniHeight: Float = BottomBarMetrics.ExpandedMiniHeight,
    verticalGap: Float = BottomBarMetrics.VerticalGap
) {
    val centerTravel = expandedMiniHeight / 2f + verticalGap + expandedTabHeight / 2f
    // 只允许位置通道轻微越过展开端点；占位高度同步容纳回正，Tab 中心线仍固定。
    private val miniDistance = centerTravel * miniExpansion.coerceAtLeast(0f)
    val tabCenterY = maxOf(tabHeight / 2f, miniHeight / 2f + miniDistance)
    val height = tabCenterY + expandedTabHeight / 2f
    val tabTop = tabCenterY - tabHeight / 2f
    val miniTop = tabCenterY - miniDistance - miniHeight / 2f
    val scalePivotY = tabCenterY / height
}

/** 主外壳覆盖 Search 后半部分时消去内部表面，宽度回弹到端点后不重新露出内沿。 */
internal fun searchSurfaceMergeProgress(primary: Rect, search: Rect): Float {
    if (search.width <= 0f) return 1f
    val overlap = (minOf(primary.right, search.right) - maxOf(primary.left, search.left)).coerceAtLeast(0f)
    val progress = ((overlap / search.width - 0.5f) / 0.5f).coerceIn(0f, 1f)
    return progress * progress * (3f - 2f * progress)
}

internal const val TAB_MOTION_STIFFNESS = 700f / (0.7f * 0.7f)
internal const val MINI_MOTION_STIFFNESS = 700f / (1.2f * 1.2f)

/** 宽度回弹峰值按目标宽度的约 2% 计算，两态宽度差不同也保持相近的视觉幅度。 */
internal fun capsuleWidthDamping(targetWidth: Float, travel: Float): Float {
    if (travel <= 0f) return 1f
    val peakRatio = (targetWidth * 0.02f / travel).coerceIn(0.001f, 0.25f)
    val logPeak = ln(peakRatio)
    return -logPeak / sqrt(PI.toFloat() * PI.toFloat() + logPeak * logPeak)
}

// 参考展开 #381–384：封面上移约 56px，横坐标不变；约 50ms 后才开始铺宽。
private const val MINI_EXPANSION_WIDTH_DELAY_NANOS = 50_000_000L

internal val miniPlayerExpansionAnimationSpec: FloatAnimationSpec = RestingExpansionDelaySpec(
    spring = FloatSpringSpec(1f, MINI_MOTION_STIFFNESS, 0.01f),
    collapsedValue = 0f
)

/** 输入与输出均为 dp 值，保持原宽度弹簧及其 0.1dp 的结束阈值。 */
internal fun miniPlayerWidthAnimationSpec(
    compactWidth: Float,
    expandedWidth: Float,
    targetWidth: Float
): FloatAnimationSpec = RestingExpansionDelaySpec(
    spring = FloatSpringSpec(
        capsuleWidthDamping(targetWidth, expandedWidth - compactWidth),
        MINI_MOTION_STIFFNESS,
        0.1f
    ),
    collapsedValue = compactWidth
)

/** 仅从收起静止端点展开时错开起动；收起、反向打断和尺寸变化仍直接承接原弹簧。 */
private class RestingExpansionDelaySpec(
    private val spring: FloatSpringSpec,
    private val collapsedValue: Float
) : FloatAnimationSpec {
    private fun delay(initialValue: Float, targetValue: Float, initialVelocity: Float): Long =
        if (targetValue > initialValue && abs(initialValue - collapsedValue) < 0.0001f &&
            abs(initialVelocity) < 0.0001f
        ) MINI_EXPANSION_WIDTH_DELAY_NANOS else 0L

    override fun getValueFromNanos(
        playTimeNanos: Long, initialValue: Float, targetValue: Float, initialVelocity: Float
    ): Float {
        val time = playTimeNanos - delay(initialValue, targetValue, initialVelocity)
        return if (time < 0L) initialValue
        else spring.getValueFromNanos(time, initialValue, targetValue, initialVelocity)
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long, initialValue: Float, targetValue: Float, initialVelocity: Float
    ): Float {
        val time = playTimeNanos - delay(initialValue, targetValue, initialVelocity)
        return if (time < 0L) 0f
        else spring.getVelocityFromNanos(time, initialValue, targetValue, initialVelocity)
    }

    override fun getDurationNanos(initialValue: Float, targetValue: Float, initialVelocity: Float): Long =
        delay(initialValue, targetValue, initialVelocity) +
            spring.getDurationNanos(initialValue, targetValue, initialVelocity)

    override fun getEndVelocity(initialValue: Float, targetValue: Float, initialVelocity: Float): Float =
        spring.getEndVelocity(initialValue, targetValue, initialVelocity)
}

// 纵向位移比上一版缩短 20%，贴近宽度变化的节奏；宽度弹簧参数不变。
private val miniPlayerPositionDurationMillis = (
    FloatSpringSpec(dampingRatio = 1f, stiffness = MINI_MOTION_STIFFNESS, visibilityThreshold = 0.01f)
        .getDurationNanos(0f, 1f, 0f) / 1_000_000L
    ).toInt().let { (it * 0.8f).toInt().coerceAtLeast(1) }

// 保留收起的后段加速，但末端斜率回到零，避免 EaseInQuad 到终点仍在运动却突然停住。
private val MiniCollapseEasing = CubicBezierEasing(0.55f, 0f, 0.8f, 1f)

// 原片 #381–410 的归一化位移拟合约为 stiffness=400、damping=0.71；
// 使用 0.72 将越位控制在约 3.8% 行程，即当前布局约 2.3dp，不改宽高或图标比例。
private val MiniExpandPositionSpring = FloatSpringSpec(
    dampingRatio = 0.72f,
    stiffness = 400f,
    visibilityThreshold = 0.001f
)
private val MiniCollapsePositionTween = FloatTweenSpec(
    duration = miniPlayerPositionDurationMillis,
    easing = MiniCollapseEasing
)

/** 展开短促提速后减速回正；收起仍沿原曲线，宽度继续独立保留 2% 回弹。 */
internal fun miniPlayerPositionAnimationSpec(expanding: Boolean): FloatAnimationSpec =
    if (expanding) MiniExpandPositionSpring else MiniCollapsePositionTween

/** 独立胶囊、图标和点击区域共享坐标；展开态仍由完整的四 Tab 胶囊承载。 */
internal class BottomTabMorphGeometry(
    val width: Float,
    val height: Float,
    val diameter: Float,
    val padding: Float,
    val expansion: Float,
    val isLtr: Boolean,
    private val labelHeight: Float = diameter * 0.28f
) {
    val tabWidth = (width - padding * 2f) / 4f
    private val split = padding + tabWidth * 3f
    // Search 独立后主胶囊仍从完整宽度开始收缩，初期允许两枚独立玻璃面重叠。
    val primary = physical(Rect(0f, 0f, lerp(diameter, width, expansion), height))
    val search = physical(Rect(width - lerp(diameter, width - split, expansion), 0f, width, height))

    // 位置跟随同一个实际宽度：正常形变时固定外侧边缘，只有超出端点的回弹才向两边扩散。
    // 不能用 expansion 的中心承载独立宽度弹簧，否则两条曲线的相位差会把外沿额外顶出去。
    fun primaryBounds(animatedWidth: Float): Rect {
        val baseWidth = animatedWidth.coerceIn(diameter, width)
        val left = (baseWidth - animatedWidth) / 2f
        return physical(Rect(left, 0f, left + animatedWidth, height))
    }

    fun searchBounds(animatedWidth: Float): Rect {
        val baseWidth = animatedWidth.coerceIn(diameter, width - split)
        val right = width + (animatedWidth - baseWidth) / 2f
        return physical(Rect(right - animatedWidth, 0f, right, height))
    }

    fun iconCenter(index: Int): Offset {
        val expandedX = padding + tabWidth * (index + 0.5f)
        val compactX = if (index == 3) width - diameter / 2f else diameter / 2f
        val x = lerp(compactX, expandedX, expansion)
        return Offset(if (isLtr) x else width - x, height / 2f - labelHeight / 2f * expansion)
    }

    /** 选择器沿同一条图标轨迹移动；小数索引保留展开态的连续拖动。 */
    fun selectorCenterX(index: Float, edgeOutset: Float = 0f): Float {
        val position = index.coerceIn(0f, 3f)
        val start = position.toInt()
        val end = (start + 1).coerceAtMost(3)
        val outward = (position / 3f * 2f - 1f) * edgeOutset
        return lerp(iconCenter(start).x, iconCenter(end).x, position - start) +
            if (isLtr) outward else -outward
    }

    /** 两端与隐藏胶囊的间距等于上下间距；传入各自完成缩放后的真实宽高。 */
    fun selectorEdgeOutset(
        capsuleWidth: Float,
        capsuleHeight: Float,
        selectorWidth: Float,
        selectorHeight: Float
    ): Float {
        val verticalGap = (selectorHeight - capsuleHeight) / 2f
        val alignedSelectorCenter = (width - capsuleWidth + selectorWidth) / 2f - verticalGap
        return padding + tabWidth / 2f - alignedSelectorCenter
    }

    private fun physical(rect: Rect): Rect = if (isLtr) rect else {
        Rect(width - rect.right, rect.top, width - rect.left, rect.bottom)
    }
}
