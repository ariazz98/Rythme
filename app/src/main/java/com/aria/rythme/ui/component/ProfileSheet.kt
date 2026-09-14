package com.aria.rythme.ui.component

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aria.rythme.R
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

/** 参考 09-12 19:28 录屏：背景保持原位，近全高圆角面板从底部进出。 */
@Composable
internal fun ProfileSheet(displayName: String, avatarPath: String?, onSaveProfile: suspend (String, String?) -> Unit, configurationRequired: Boolean = false, onDismiss: () -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    var dragDismissMotion by remember { mutableStateOf<ProfileSheetDismissMotion?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val dismissed by rememberUpdatedState(onDismiss)
    LaunchedEffect(closing) {
        if (closing) {
            val motion = dragDismissMotion
            progress.animateTo(0f, if (motion != null) tween(motion.durationMs, easing = motion.easing)
                else tween(320, easing = CubicBezierEasing(.4f, 0f, .8f, .2f)))
            dismissed()
        } else progress.animateTo(1f, tween(440, easing = CubicBezierEasing(.18f, .84f, .22f, 1f)))
    }
    fun close() { settleJob?.cancel(); closing = true }
    BackHandler(onBack = ::close)
    val colors = MaterialTheme.rythmeColors
    val color = if (LocalGlassHdr.current.darkTheme) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val itemColor = if (LocalGlassHdr.current.darkTheme) Color(0xFF2C2C2E) else Color.White
    val backdrop = rememberLayerBackdrop()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val referenceScale = TopBarComponentMetrics.referenceScale(maxWidth.value)
        val topGap = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp
        val sheetHeight = (maxHeight - topGap).coerceAtLeast(0.dp)
        val heightPx = with(LocalDensity.current) { sheetHeight.toPx() }.coerceAtLeast(1f)
        fun finishDrag(velocity: Float) {
            if (closing) return
            if (dragOffset > heightPx * .35f || (dragOffset > 0f && velocity > 2000f)) {
                dragDismissMotion = ProfileSheetDismissMotion(velocity, heightPx - dragOffset, heightPx)
                close()
            } else {
                settleJob?.cancel()
                settleJob = scope.launch { animate(dragOffset, 0f) { value, _ -> dragOffset = value.coerceAtLeast(0f) } }
            }
        }
        val contentScroll = rememberScrollState()
        val nestedDrag = remember(heightPx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (closing || source != NestedScrollSource.UserInput || dragOffset <= 0f) return Offset.Zero
                    settleJob?.cancel()
                    val before = dragOffset
                    dragOffset = (before + available.y).coerceIn(0f, heightPx)
                    return Offset(0f, dragOffset - before)
                }
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (closing || progress.value < .999f || source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                    settleJob?.cancel()
                    val before = dragOffset
                    dragOffset = (before + available.y).coerceAtMost(heightPx)
                    return Offset(0f, dragOffset - before)
                }
                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (closing || dragOffset <= 0f) return Velocity.Zero
                    finishDrag(available.y)
                    return available
                }
                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (closing || dragOffset <= 0f) return Velocity.Zero
                    finishDrag(available.y)
                    return available
                }
            }
        }
        Box(Modifier.fillMaxSize()
            .graphicsLayer { alpha = progress.value * (1f - dragOffset / heightPx) * .18f }
            .background(Color.Black)
            .clickable(interactionSource = null, indication = null, onClickLabel = "关闭个人面板", onClick = ::close))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(sheetHeight)
            // 手势节点位于移动图层外，避免面板移动改变触点坐标而造成抖动。
            .draggable(
                state = rememberDraggableState { delta ->
                    dragOffset = (dragOffset + delta).coerceIn(0f, heightPx)
                },
                orientation = Orientation.Vertical,
                enabled = !closing && progress.value >= .999f,
                onDragStarted = { settleJob?.cancel() },
                onDragStopped = { velocity -> finishDrag(velocity) }
            )
            .nestedScroll(nestedDrag)
            .graphicsLayer { translationY = size.height * (1f - progress.value) + dragOffset * progress.value }
            .clip(RoundedCornerShape(40.dp))
            .semantics { paneTitle = "个人面板" }) {
            // 关闭按钮只采样面板底色，避免把自身录入采样层。
            Box(Modifier.matchParentSize().layerBackdrop(backdrop).background(color))
            Column(
                Modifier.fillMaxSize().padding(top = 68.dp).verticalScroll(contentScroll)
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(itemColor)
                    .clickable(onClickLabel = "编辑个人资料", onClick = { editing = true })
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeaderAvatarContent(
                    action = Action.Avatar("profile-identity", url = avatarPath, name = displayName, contentDescription = "头像"),
                    referenceScale = 1.1f,
                    modifier = Modifier.background(Brush.verticalGradient(listOf(HeaderAvatarTop, HeaderAvatarBottom)), CircleShape)
                )
                Text(displayName, color = colors.textColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                androidx.compose.material3.Icon(androidx.compose.ui.res.painterResource(R.drawable.ic_forward), null,
                    Modifier.size(12.dp), tint = colors.subTitleColor)
            }
                com.aria.rythme.feature.pitch.presentation.MelodyModelSettings(
                    modifier = Modifier.fillMaxWidth()
                        .background(itemColor, RoundedCornerShape(22.dp))
                        .padding(20.dp),
                    configurationRequired = configurationRequired
                )
                Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
            Box(Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 12.dp)) {
                AnimatedHeaderActions(
                    sourceKey = "profile-sheet-close",
                    referenceScale = referenceScale,
                    actions = listOf(Action.Icon("close", R.drawable.ic_close, iconSize = 16.dp,
                        contentDescription = "关闭个人面板", onClick = ::close)),
                    backdrop = backdrop,
                    enabled = !closing,
                    skipAnimation = true
                )
            }
        }
    }
    if (editing) ProfileEditor(displayName, avatarPath, backdrop, onSaveProfile, onDismiss = { editing = false })
}
