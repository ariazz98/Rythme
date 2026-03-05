package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.launch
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.LocalPlayerVisible
import com.aria.rythme.LocalSharedTransitionScope
import com.aria.rythme.R
import com.aria.rythme.core.extensions.customMarquee
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule

/**
 * 迷你播放器条。
 *
 * 展示当前播放歌曲的封面、标题、歌手，以及暂停/下一首操作按钮。
 * 点击整体区域可进入完整播放器页面。
 *
 * ## 按压放大效果
 *
 * 使用 [drawBackdrop] 的 `layerBlock` 在按下时对胶囊背景整体轻微放大（约 +16dp/width ≈ 5%），
 * 与 BottomNavigationBar 的按压动效保持视觉一致。
 *
 * **手势检测**：通过独立的 `pointerInput` 监听按下/抬起，驱动 [pressAnimation]。
 * 使用 `requireUnconsumed = false` 是因为 `clickable`（链末 = 内层 modifier）在
 * Compose 指针事件 Main pass 中先于外层 `pointerInput` 接收事件并消费 DOWN，
 * 若不设置此参数，`awaitFirstDown` 将永远等不到 DOWN 事件，动画不会触发。
 *
 * @param song            当前播放歌曲，null 表示未播放
 * @param isPlaying       是否正在播放，控制暂停/播放图标切换
 * @param canPlayNext     是否可以播放下一首，控制下一首按钮是否可点击
 * @param onClick         点击整体区域的回调（进入完整播放器）
 * @param onPlayPauseClick 点击播放/暂停按钮的回调
 * @param onNextClick     点击下一首按钮的回调
 */
@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    canPlayNext: Boolean,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    val backdrop = LocalBackdrop.current
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    val scope = rememberCoroutineScope()

    /** 按压进度 [0, 1]，驱动 layerBlock 中的缩放系数 */
    val pressAnimation = remember { Animatable(0f) }

    /**
     * 临界阻尼 + 高刚度弹簧：手感快脆，无回弹过冲。
     * 与 TabDragState 的 pressSpec 参数相同，保持整个 BottomBar 区域动效一致。
     */
    val pressSpec = spring(1f, 1000f, 0.001f)

    val sharedTransitionScope = LocalSharedTransitionScope.current
    val playerVisible = LocalPlayerVisible.current

    Row(
        modifier = Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()
                    blur(4f.dp.toPx())
                    lens(24f.dp.toPx(), 32f.dp.toPx())
                },
                layerBlock = {
                    // 按压时胶囊背景轻微膨胀，scale 最大约 1.05（16dp / width）
                    val progress = pressAnimation.value
                    val scale = lerp(1f, 1f + 16f.dp.toPx() / size.width, progress)
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = { drawRect(containerColor) }
            )
            .pointerInput(scope) {
                awaitEachGesture {
                    // requireUnconsumed = false：接受已被 clickable 消费的 DOWN 事件，
                    // 确保动画在手指按下时立即触发，而非被 clickable 拦截后丢失
                    awaitFirstDown(requireUnconsumed = false)
                    scope.launch { pressAnimation.animateTo(1f, pressSpec) }

                    // 等待手指抬起或手势取消，无论哪种情况都恢复到 0
                    waitForUpOrCancellation()
                    scope.launch { pressAnimation.animateTo(0f, pressSpec) }
                }
            }
            .fillMaxWidth()
            .height(50.dp)
            // indication = null：禁用默认水波纹，视觉反馈完全由 layerBlock 动效承担
            .clickable(interactionSource = null, indication = null) { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {

        with(sharedTransitionScope) {
            CoverItem(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .sharedElementWithCallerManagedVisibility(
                        sharedContentState = rememberSharedContentState(key = "cover"),
                        visible = !playerVisible
                    ),
                size = 32.dp,
                corner = 6.dp,
                song = song,
                defaultBgColor = Color(0x99D6D6D5),
                defaultIconColor = Color(0xFF4A4A49)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song?.title ?: stringResource(R.string.not_play),
                color = MaterialTheme.rythmeColors.textColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    // Offscreen 使 DstIn BlendMode 正确裁剪文字，而非裁剪整个 Row
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        // 左侧渐隐：从左边缘 0 到 8dp 由透明过渡到不透明，消除边缘截断感
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                1f to Color.Black,
                                startX = 0f,
                                endX = 8.dp.toPx()
                            ),
                            blendMode = BlendMode.DstIn
                        )
                        // 右侧渐隐：最后 15% 宽度淡出，为跑马灯文字提供柔和出口
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0.85f to Color.Black,
                                1f to Color.Transparent
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .customMarquee()
                    .padding(start = 8.dp)
            )
            if (!song?.artist.isNullOrEmpty()) {
                Text(
                    text = song.artist,
                    color = MaterialTheme.rythmeColors.textColor,
                    maxLines = 1,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            painter = painterResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
            contentDescription = "",
            modifier = Modifier
                .padding(end = 21.dp)
                .size(18.dp)
                .clickable(interactionSource = null, indication = null) {
                    onPlayPauseClick()
                }
        )

        Icon(
            painter = painterResource(R.drawable.ic_next),
            contentDescription = "",
            tint = if (canPlayNext) Color.Black else Color(0xFFBFBFBE),
            modifier = Modifier
                .padding(end = 21.dp)
                .size(28.dp)
                // canPlayNext = false 时不挂载 clickable，完全禁用点击而非仅视觉置灰
                .then(
                    if (canPlayNext) {
                        Modifier.clickable(interactionSource = null, indication = null) {
                            onNextClick()
                        }
                    } else {
                        Modifier
                    }
                )
        )
    }
}
