package com.aria.rythme.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.R
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch

/**
 * 搜索占位符 — 放在 content 列表中，外观类似搜索框但不可输入，点击触发搜索激活
 */
@Composable
fun SearchPlaceholder(
    onClick: () -> Unit,
    contentAlpha: Float = 1f,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    value: String = "",
    hint: String = stringResource(R.string.title_search),
    verticalPadding: Dp = HeaderSearchLayout.inlineVerticalPadding
) {
    Row(
        modifier = Modifier
            .padding(vertical = verticalPadding)
            .then(modifier)
            .clip(ContinuousCapsule)
            .background(MaterialTheme.rythmeColors.searchBg)
            .fillMaxWidth()
            .height(HeaderSearchLayout.surfaceHeight)
            .clickable(
                interactionSource = null,
                indication = null,
                enabled = enabled,
                onClick = onClick
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
            text = value.ifEmpty { hint },
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
    value: String,
    onValueChange: (String) -> Unit,
    progress: Float,
    onClose: () -> Unit,
    backdrop: Backdrop = LocalBackdrop.current,
    clearable: Boolean = false,
    hint: String = stringResource(R.string.title_search)
) {
    val backgroundColor = MaterialTheme.rythmeColors.bottomBackground
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val animationScope = rememberCoroutineScope()
    val pressAnimation = remember { Animatable(0f) }
    val pressSpec = spring(1f, 1000f, 0.001f)

    LaunchedEffect(active) {
        if (active) {
            awaitFrame()
            focusRequester.requestFocus()
        } else {
            focusManager.clearFocus()
        }
    }

    // 与页面占位、标题退场共用同一进度，避免多个 Transition 错拍。
    val bodyEndPadding = HeaderSearchLayout.bodyEndPadding(progress)
    val closeButtonOffsetX = HeaderSearchMotion.closeEnterTravel * (1f - progress)
    val closeButtonAlpha = progress

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderSearchLayout.toolbarHeight)
    ) {
        // 搜索框主体
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth()
                .padding(end = bodyEndPadding)
                .height(HeaderSearchLayout.surfaceHeight)
        ) {
            GlassBackdropSurface(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    glassLens(24f.dp.toPx(), 32f.dp.toPx())
                },
                pressProgress = { pressAnimation.value },
                onDrawSurface = { drawRect(backgroundColor) }
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(HeaderSearchLayout.surfaceHeight)
                    .clip(ContinuousCapsule)
                    .pointerInput(active) {
                        if (!active) return@pointerInput
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            try {
                                animationScope.launch { pressAnimation.animateTo(1f, pressSpec) }
                                // 只观察按压，不消费输入事件，不接管焦点和文字选择。
                                waitForUpOrCancellation()
                            } finally {
                                animationScope.launch { pressAnimation.animateTo(0f, pressSpec) }
                            }
                        }
                    }
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
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    keyboardOptions = if (clearable) KeyboardOptions(imeAction = ImeAction.Search) else KeyboardOptions.Default,
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    cursorBrush = SolidColor(MaterialTheme.rythmeColors.primary),
                    textStyle = TextStyle(
                        color = MaterialTheme.rythmeColors.subTitleColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = hint,
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

                if (clearable && value.isNotEmpty()) {
                    Box(Modifier.size(28.dp).clip(ContinuousCapsule)
                        .clickable(interactionSource = null, indication = null) { onValueChange("") },
                        contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_close), stringResource(R.string.search_clear),
                            tint = MaterialTheme.rythmeColors.textColor, modifier = Modifier.size(14.dp))
                    }
                } else Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = "mic",
                    tint = MaterialTheme.rythmeColors.textColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 关闭按钮，从右侧滑入
        Box(
            modifier = Modifier
                .padding(end = HeaderSearchLayout.closeTouchEndInset)
                .size(HeaderSearchLayout.closeTouchSize)
                .align(Alignment.CenterEnd)
                .offset(x = closeButtonOffsetX)
                .glassHdrFadeAndBlur(alpha = { closeButtonAlpha }),
            contentAlignment = Alignment.Center
        ) {
            CloseButton(
                onClick = onClose
            )
        }
    }
}
