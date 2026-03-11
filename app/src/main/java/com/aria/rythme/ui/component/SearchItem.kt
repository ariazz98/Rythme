package com.aria.rythme.ui.component

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.R
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.shadow.Shadow
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.delay

/**
 * 搜索占位符 — 放在 content 列表中，外观类似搜索框但不可输入，点击触发搜索激活
 */
@Composable
fun SearchPlaceholder(
    onClick: () -> Unit,
    contentAlpha: Float = 1f
) {
    Row(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .clip(ContinuousCapsule)
            .background(Color(0xFFEBEBEB))
            .fillMaxWidth()
            .height(44.dp)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = { if (contentAlpha > 0.9f) onClick() }
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = "Search",
            tint = MaterialTheme.rythmeColors.textColor,
            modifier = Modifier.size(18.dp).alpha(contentAlpha)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = stringResource(R.string.title_search),
            color = MaterialTheme.rythmeColors.subTitleColor.copy(alpha = contentAlpha),
            fontSize = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            painter = painterResource(R.drawable.ic_mic),
            contentDescription = "mic",
            tint = MaterialTheme.rythmeColors.textColor,
            modifier = Modifier.size(18.dp).alpha(contentAlpha)
        )
    }
}

/**
 * Header 中的搜索栏 — 带玻璃效果，激活时宽度收窄并滑入 CloseButton
 */
@Composable
fun HeaderSearchBar(
    active: Boolean,
    onClose: () -> Unit,
    backdrop: Backdrop = LocalBackdrop.current
) {
    var searchText by remember { mutableStateOf("") }
    val backgroundColor = MaterialTheme.rythmeColors.bottomBackground
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(active) {
        if (active) {
            delay(150)
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    // 关闭按钮区域宽度：8dp 间距 + 44dp 按钮
    val closeButtonAreaWidth = 52.dp

    // 用 MutableTransitionState 确保首次组合也有动画
    val transitionState = remember { MutableTransitionState(false) }
    transitionState.targetState = active
    val transition = rememberTransition(transitionState, label = "searchBar")

    // 搜索框主体右侧 padding，为关闭按钮腾出空间
    val bodyEndPadding by transition.animateDp(
        transitionSpec = { tween(ANIM_DURATION) },
        label = "bodyEndPadding"
    ) { if (it) 77.dp else 21.dp }

    // 关闭按钮水平偏移
    val closeButtonOffsetX by transition.animateDp(
        transitionSpec = { tween(ANIM_DURATION) },
        label = "closeButtonOffsetX"
    ) { if (it) 0.dp else closeButtonAreaWidth }

    // 关闭按钮透明度
    val closeButtonAlpha by transition.animateFloat(
        transitionSpec = { tween(ANIM_DURATION) },
        label = "closeButtonAlpha"
    ) { if (it) 1f else 0f }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
    ) {
        // 搜索框主体
        Row(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(end = bodyEndPadding)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { ContinuousCapsule },
                    shadow = { Shadow.Default.copy(radius = 12.dp, offset = DpOffset(0.dp, 0.dp)) },
                    effects = {
                        vibrancy()
                        blur(2f.dp.toPx())
                        lens(24f.dp.toPx(), 32f.dp.toPx())
                    },
                    onDrawSurface = { drawRect(backgroundColor) }
                )
                .height(44.dp)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Search",
                tint = MaterialTheme.rythmeColors.textColor,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = searchText,
                onValueChange = { searchText = it },
                singleLine = true,
                cursorBrush = SolidColor(MaterialTheme.rythmeColors.primary),
                textStyle = TextStyle(
                    color = MaterialTheme.rythmeColors.subTitleColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (searchText.isEmpty()) {
                            Text(
                                text = stringResource(R.string.title_search),
                                color = MaterialTheme.rythmeColors.subTitleColor,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = "mic",
                tint = MaterialTheme.rythmeColors.textColor,
                modifier = Modifier.size(18.dp)
            )
        }

        // 关闭按钮，从右侧滑入
        Box(
            modifier = Modifier
                .padding(end = 9.dp)
                .size(68.dp)
                .align(Alignment.CenterEnd)
                .offset(x = closeButtonOffsetX)
                .alpha(closeButtonAlpha),
            contentAlignment = Alignment.Center
        ) {
            CloseButton(
                onClick = {
                    searchText = ""
                    onClose()
                }
            )
        }
    }
}
