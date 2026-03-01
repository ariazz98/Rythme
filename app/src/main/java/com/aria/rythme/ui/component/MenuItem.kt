package com.aria.rythme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
fun MenuItem(
    backdrop: Backdrop = LocalBackdrop.current,
    iconRes: Int,
    onClick: () -> Unit
) {
    val containerColor = MaterialTheme.rythmeColors.bottomBackground
    Box(modifier = Modifier
        .drawBackdrop(
            backdrop = backdrop,
            shape = { CircleShape },
            effects = {
                vibrancy()
                blur(2f.dp.toPx())
                lens(24f.dp.toPx(), 32f.dp.toPx())
            },
            onDrawSurface = {
                drawRect(color = containerColor)
            }
        )
        .size(44.dp)
        .clickable(
            interactionSource = null,
            indication = null
        ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = "",
            tint = MaterialTheme.rythmeColors.textColor,
            modifier = Modifier.size(28.dp)
        )
    }
}