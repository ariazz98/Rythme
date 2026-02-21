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
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun MenuItem(
    iconRes: Int,
    onClick: () -> Unit
) {
    Box(modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(Color.White)
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