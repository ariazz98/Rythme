package com.aria.rythme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.capsule.ContinuousCapsule

@Composable
fun CapsuleButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: Int? = null,
    iconSize: Dp = 20.dp,
    text: Int,
    textSize: TextUnit = 16.sp,
    tint: Color = MaterialTheme.rythmeColors.primary
) {
    val playInteraction = remember { MutableInteractionSource() }
    val playPressed by playInteraction.collectIsPressedAsState()
    Row(
        modifier = modifier
            .clip(ContinuousCapsule)
            .background(Color(0xFFEFEFF0))
            .clickable(
                interactionSource = playInteraction,
                indication = null
            ) {
                onClick()
            }
            .drawWithContent {
                drawContent()
                if (playPressed) {
                    drawRect(Color.White.copy(alpha = 0.2f))
                }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = "",
                tint = tint,
                modifier = Modifier.size(iconSize)
            )

            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = stringResource(text),
            fontSize = textSize,
            color = tint,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun CommonOperateButton(
    onPlayClick: () -> Unit,
    onRandomPlayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CapsuleButton(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            icon = R.drawable.ic_play,
            iconSize = 14.dp,
            text = R.string.music_play,
            onClick = onPlayClick
        )

        CapsuleButton(
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            icon = R.drawable.ic_shuffle,
            text = R.string.music_random_play,
            onClick = onRandomPlayClick
        )
    }
}