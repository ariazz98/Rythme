package com.aria.rythme.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun RythmeHeader(
    title: String,
    hasMoreMenu: Boolean,
    hasAvatar: Boolean,
    isShow: Boolean,
    onMoreClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null
) {

    val headerAlpha by animateFloatAsState(
        targetValue = if (isShow) 1f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "headerAlpha"
    )

    val gradientAlpha by animateFloatAsState(
        targetValue = if (isShow) 0f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "gradientAlpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 21.dp, vertical = 8.dp)
            .alpha(headerAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 标题
        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.rythmeColors.textColor,
            modifier = Modifier.weight(1f)
        )

        if (hasMoreMenu) {
            MenuItem(
                iconRes = R.drawable.ic_more,
                onClick = {
                    onMoreClick?.invoke()
                }
            )

            Spacer(modifier = Modifier.width(16.dp))
        }

        if (hasAvatar) {
            AvatarItem(
                url = "",
                name = "ARiA",
                onClick = {
                    onAvatarClick?.invoke()
                }
            )
        }
    }

    if (gradientAlpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .alpha(gradientAlpha)
                .background(
                    Brush.verticalGradient(listOf(
                        MaterialTheme.rythmeColors.surface.copy(0.1f), Color.Transparent
                    ))
                )
        )
    }
}