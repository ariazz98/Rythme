package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aria.rythme.ui.component.utils.DropletSlideAnimation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * AnimatedHeaderActions 的动画状态机，统一管理所有动画维度：
 *
 * - 整体进出场：overallBlur + overallAlpha
 * - 内容交叉淡入淡出：contentBlur
 * - 更多按钮水滴动画：moreDroplet
 *
 * 通过 Mutex 防止并发动画冲突。
 * 使用 mutableStateOf 确保 Compose 能观测状态变化并触发重组。
 */
class HeaderActionsAnimState(scope: CoroutineScope) {

    enum class Phase { Hidden, Visible }

    // ---- 动画值 ----
    val overallBlur = Animatable(0f)
    val overallAlpha = Animatable(1f)
    val contentBlur = Animatable(0f)
    val moreDroplet = DropletSlideAnimation(animationScope = scope)

    // ---- 可观测状态（驱动重组） ----
    var phase by mutableStateOf(Phase.Hidden)
        private set
    var displayActions by mutableStateOf<List<Action>>(emptyList())
        private set
    var showMore by mutableStateOf(false)
        private set

    private val mutex = Mutex()

    /** actions 内容指纹 */
    private fun List<Action>.contentKey(): String = joinToString(",") { action ->
        when (action) {
            is Action.Icon -> "I:${action.iconRes}"
            is Action.Avatar -> "A:${action.url}:${action.name}"
        }
    }

    /**
     * 初始化：首次组合时调用，跳过所有动画直接 snap 到正确状态
     */
    suspend fun initialize(actions: List<Action>, showMoreButton: Boolean) = mutex.withLock {
        if (actions.isNotEmpty()) {
            overallBlur.snapTo(0f)
            overallAlpha.snapTo(1f)
            contentBlur.snapTo(0f)
            if (showMoreButton) moreDroplet.snapToVisible()
            // 先设动画值，最后更新 phase 触发重组（确保首帧数据完整）
            displayActions = actions
            showMore = showMoreButton
            phase = Phase.Visible
        } else {
            phase = Phase.Hidden
        }
    }

    /**
     * 处理所有输入变化，内部决定执行哪种动画
     */
    suspend fun update(
        actions: List<Action>,
        showMoreButton: Boolean,
        skipAnimation: Boolean,
        moreSlideDistance: Float,
    ) = mutex.withLock {
        val hasContent = actions.isNotEmpty()
        val actionsKey = actions.contentKey()
        val displayKey = displayActions.contentKey()

        when {
            // 场景 A: 进场（无→有）
            hasContent && phase == Phase.Hidden -> {
                contentBlur.snapTo(0f)
                if (showMoreButton) moreDroplet.snapToVisible()
                if (skipAnimation) {
                    overallBlur.snapTo(0f)
                    overallAlpha.snapTo(1f)
                } else {
                    overallBlur.snapTo(10f)
                    overallAlpha.snapTo(0f)
                }
                // 设置数据并切换 phase，触发重组使 UI 出现
                displayActions = actions
                showMore = showMoreButton
                phase = Phase.Visible
                // 之后播放进场动画（UI 已挂载）
                if (!skipAnimation) {
                    coroutineScope {
                        launch { overallBlur.animateTo(0f, spring(dampingRatio = 1f, stiffness = 500f)) }
                        launch { overallAlpha.animateTo(1f, tween(ANIM_DURATION)) }
                    }
                }
            }

            // 场景 B: 退场（有→无）
            !hasContent && phase == Phase.Visible -> {
                if (skipAnimation) {
                    overallBlur.snapTo(10f)
                    overallAlpha.snapTo(0f)
                } else {
                    coroutineScope {
                        launch { overallBlur.animateTo(10f, spring(dampingRatio = 1f, stiffness = 500f)) }
                        launch { overallAlpha.animateTo(0f, tween(ANIM_DURATION)) }
                    }
                }
                displayActions = emptyList()
                showMore = false
                phase = Phase.Hidden
            }

            // 场景 C: 内容变更（有→有，内容不同）
            hasContent && phase == Phase.Visible && actionsKey != displayKey -> {
                // 修复：场景 B（退场）被取消时 overallBlur/overallAlpha 可能卡在中间值，
                // 需要先恢复到完全可见状态再做内容交叉过渡
                overallBlur.snapTo(0f)
                overallAlpha.snapTo(1f)
                if (skipAnimation) {
                    displayActions = actions
                    showMore = showMoreButton
                    if (showMoreButton) moreDroplet.snapToVisible()
                } else {
                    // 立即换数据（触发 animateContentSize 宽度过渡），
                    // 同时用一个短暂的模糊脉冲遮盖内容切换瞬间
                    contentBlur.snapTo(10f)
                    displayActions = actions
                    coroutineScope {
                        launch { contentBlur.animateTo(0f, spring(dampingRatio = 1f, stiffness = 500f)) }
                        launch { handleMoreChange(showMoreButton, skipAnimation, moreSlideDistance) }
                    }
                }
            }

            // 场景 D: 仅更多按钮变更
            hasContent && phase == Phase.Visible -> {
                overallBlur.snapTo(0f)
                overallAlpha.snapTo(1f)
                handleMoreChange(showMoreButton, skipAnimation, moreSlideDistance)
            }
        }
    }

    /**
     * 同步 lambda 引用（不触发动画）：当 actions 内容指纹未变但引用更新时调用
     */
    fun syncActionRefs(actions: List<Action>) {
        if (actions.contentKey() == displayActions.contentKey()) {
            displayActions = actions
        }
    }

    private suspend fun handleMoreChange(
        showMoreButton: Boolean,
        skipAnimation: Boolean,
        distance: Float,
    ) {
        if (showMoreButton == showMore) return

        if (showMoreButton) {
            showMore = true
            if (skipAnimation) moreDroplet.snapToVisible()
            else moreDroplet.awaitSlideIn(fromLeft = false, distance = distance)
        } else {
            if (!skipAnimation) moreDroplet.awaitSlideOut(toLeft = false, distance = distance)
            showMore = false
        }
    }
}

/** 内容交叉淡入淡出单程时长，与 animateContentSize 同步 */
private const val CONTENT_CROSSFADE = 150
