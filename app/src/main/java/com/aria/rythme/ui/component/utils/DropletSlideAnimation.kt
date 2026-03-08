package com.aria.rythme.ui.component.utils

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * 水滴弹性滑入/滑出动画
 *
 * 圆形组件从左或右侧滑入时，沿运动方向拉伸、垂直方向压缩，
 * 到达目标位置后通过不同参数的弹簧动画恢复为圆形，
 * X 和 Y 轴振荡频率不同，产生水滴般的弹性形变效果。
 *
 * @param animationScope 动画协程作用域
 * @param stretchAmount 沿运动方向的最大拉伸量 (1.0 = 不变形, >1.0 = 拉伸)
 * @param squeezeAmount 垂直方向的最大压缩量 (1.0 = 不变形, <1.0 = 压缩)
 */
class DropletSlideAnimation(
    private val animationScope: CoroutineScope,
    private val stretchAmount: Float = 1.15f,
    private val squeezeAmount: Float = 0.85f,
) {

    // 位移弹簧：中等阻尼，允许轻微过冲
    private val offsetSpec =
        spring(dampingRatio = 0.65f, stiffness = 200f, visibilityThreshold = 0.5f)

    // X 轴缩放弹簧：低阻尼 → 更多振荡，低刚度 → 更慢恢复
    private val scaleXSpec =
        spring(dampingRatio = 0.4f, stiffness = 220f, visibilityThreshold = 0.001f)

    // Y 轴缩放弹簧：略高阻尼 → 更少振荡，略高刚度 → 更快恢复
    // 与 X 轴的差异造成非均匀形变
    private val scaleYSpec =
        spring(dampingRatio = 0.55f, stiffness = 320f, visibilityThreshold = 0.001f)

    // 模糊弹簧：临界阻尼，快速收敛
    private val blurSpec =
        spring(dampingRatio = 1f, stiffness = 500f, visibilityThreshold = 0.01f)

    /** 最大模糊半径 (dp)，隐藏状态时的模糊程度 */
    private val maxBlur = 10f

    private val offsetXAnim = Animatable(0f, 0.5f)
    private val scaleXAnim = Animatable(1f, 0.001f)
    private val scaleYAnim = Animatable(1f, 0.001f)
    private val blurAnim = Animatable(maxBlur, 0.01f)

    /** 当前 X 轴偏移量 (px) */
    val offsetX: Float get() = offsetXAnim.value

    /** 当前 X 轴缩放 */
    val scaleX: Float get() = scaleXAnim.value

    /** 当前 Y 轴缩放 */
    val scaleY: Float get() = scaleYAnim.value

    /** 当前模糊半径 (dp)，0 = 清晰，maxBlur = 完全模糊 */
    val blur: Float get() = blurAnim.value

    /** 是否处于可见状态 */
    val isVisible: Boolean get() = blurAnim.value < maxBlur - 0.1f

    /**
     * 从指定方向弹性滑入
     *
     * @param fromLeft true = 从左侧滑入, false = 从右侧滑入
     * @param distance 滑动距离 (px)
     */
    fun slideIn(fromLeft: Boolean, distance: Float) {
        val startOffset = if (fromLeft) -distance else distance
        animationScope.launch {
            // 初始状态：屏幕外 + 水滴形变
            offsetXAnim.snapTo(startOffset)
            scaleXAnim.snapTo(stretchAmount)
            scaleYAnim.snapTo(squeezeAmount)
            blurAnim.snapTo(maxBlur)

            // 各轴独立弹簧动画，不同参数产生水滴弹性效果
            launch { offsetXAnim.animateTo(0f, offsetSpec) }
            launch { scaleXAnim.animateTo(1f, scaleXSpec) }
            launch { scaleYAnim.animateTo(1f, scaleYSpec) }
            launch { blurAnim.animateTo(0f, blurSpec) }
        }
    }

    /**
     * 向指定方向弹性滑出
     *
     * @param toLeft true = 向左滑出, false = 向右滑出
     * @param distance 滑动距离 (px)
     */
    fun slideOut(toLeft: Boolean, distance: Float) {
        val endOffset = if (toLeft) -distance else distance
        animationScope.launch {
            // 滑出时拉伸形变 + 移动 + 虚化
            launch { offsetXAnim.animateTo(endOffset, offsetSpec) }
            launch { scaleXAnim.animateTo(stretchAmount, scaleXSpec) }
            launch { scaleYAnim.animateTo(squeezeAmount, scaleYSpec) }
            launch { blurAnim.animateTo(maxBlur, blurSpec) }
        }
    }

    /**
     * 挂起版滑入，等待所有动画完成后返回
     */
    suspend fun awaitSlideIn(fromLeft: Boolean, distance: Float) {
        val startOffset = if (fromLeft) -distance else distance
        offsetXAnim.snapTo(startOffset)
        scaleXAnim.snapTo(stretchAmount)
        scaleYAnim.snapTo(squeezeAmount)
        blurAnim.snapTo(maxBlur)

        coroutineScope {
            launch { offsetXAnim.animateTo(0f, offsetSpec) }
            launch { scaleXAnim.animateTo(1f, scaleXSpec) }
            launch { scaleYAnim.animateTo(1f, scaleYSpec) }
            launch { blurAnim.animateTo(0f, blurSpec) }
        }
    }

    /**
     * 挂起版滑出，虚化完成即返回
     *
     * offset/scale 在 animationScope 中继续执行（已模糊不可见，无需等振荡收敛）。
     * 若随后调用 snapToVisible/reset，snapTo 会自动取消这些残留动画。
     */
    suspend fun awaitSlideOut(toLeft: Boolean, distance: Float) {
        val endOffset = if (toLeft) -distance else distance
        animationScope.launch { offsetXAnim.animateTo(endOffset, offsetSpec) }
        animationScope.launch { scaleXAnim.animateTo(stretchAmount, scaleXSpec) }
        animationScope.launch { scaleYAnim.animateTo(squeezeAmount, scaleYSpec) }
        // 只等虚化完成
        blurAnim.animateTo(maxBlur, blurSpec)
    }

    /**
     * 立即跳到可见状态（无动画），用于首次显示已存在的内容
     */
    suspend fun snapToVisible() {
        offsetXAnim.snapTo(0f)
        scaleXAnim.snapTo(1f)
        scaleYAnim.snapTo(1f)
        blurAnim.snapTo(0f)
    }

    /**
     * 立即重置到隐藏状态
     */
    suspend fun reset() {
        offsetXAnim.snapTo(0f)
        scaleXAnim.snapTo(1f)
        scaleYAnim.snapTo(1f)
        blurAnim.snapTo(maxBlur)
    }
}
