package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** TopBar action-group 的小型视觉状态机。 */
class HeaderActionsAnimState {
    enum class Phase { Hidden, Visible }

    val overallBlur = Animatable(0f)
    val overallAlpha = Animatable(1f)
    val contentBlur = Animatable(0f)

    var phase by mutableStateOf(Phase.Hidden)
        private set

    var displayActions by mutableStateOf<List<Action>>(emptyList())
        private set

    private val mutex = Mutex()

    suspend fun initialize(actions: List<Action>) = mutex.withLock {
        displayActions = actions
        if (actions.isEmpty()) {
            phase = Phase.Hidden
        } else {
            overallBlur.snapTo(0f)
            overallAlpha.snapTo(1f)
            contentBlur.snapTo(0f)
            phase = Phase.Visible
        }
    }

    suspend fun update(
        actions: List<Action>,
        skipAnimation: Boolean
    ) = mutex.withLock {
        val hasContent = actions.isNotEmpty()
        val visualChanged = actions.contentKey() != displayActions.contentKey()

        when {
            hasContent && phase == Phase.Hidden -> {
                displayActions = actions
                phase = Phase.Visible
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
            }

            !hasContent && phase == Phase.Visible -> {
                if (!skipAnimation) {
                    coroutineScope {
                        launch { overallBlur.animateTo(10f, spring(dampingRatio = 1f, stiffness = 500f)) }
                        launch { overallAlpha.animateTo(0f, tween(ANIM_DURATION)) }
                    }
                }
                displayActions = emptyList()
                phase = Phase.Hidden
            }

            hasContent && phase == Phase.Visible && visualChanged -> {
                overallBlur.snapTo(0f)
                overallAlpha.snapTo(1f)
                if (skipAnimation) {
                    displayActions = actions
                } else {
                    contentBlur.snapTo(8f)
                    displayActions = actions
                    contentBlur.animateTo(0f, spring(dampingRatio = 1f, stiffness = 500f))
                }
            }
        }
    }

    /** 回调更新不触发视觉过渡。 */
    fun syncActionRefs(actions: List<Action>) {
        if (actions.contentKey() == displayActions.contentKey()) {
            displayActions = actions
        }
    }
}
