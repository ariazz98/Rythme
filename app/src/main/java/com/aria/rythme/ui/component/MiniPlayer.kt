package com.aria.rythme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
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
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.R
import com.aria.rythme.core.extensions.customMarquee
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.capsule.ContinuousCapsule

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
    Row(
        modifier = Modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { ContinuousCapsule },
                effects = {
                    vibrancy()
                    blur(2f.dp.toPx())
                    lens(24f.dp.toPx(), 32f.dp.toPx())
                },
                onDrawSurface = {
                    drawRect(containerColor)
                }
            )
            .fillMaxWidth()
            .height(50.dp)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {

        CoverItem(
            modifier = Modifier.padding(start = 16.dp),
            size = 32.dp,
            corner = 6.dp,
            song = song,
            defaultBgColor = Color(0x99D6D6D5),
            defaultIconColor = Color(0xFF4A4A49)
        )

        Column(modifier = Modifier
            .weight(1f)
        ) {
            Text(
                text = song?.title ?: stringResource(R.string.not_play),
                color = MaterialTheme.rythmeColors.textColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                1f to Color.Black,
                                startX = 0f,
                                endX = 8.dp.toPx()
                            ),
                            blendMode = BlendMode.DstIn
                        )
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
                .clickable(
                    interactionSource = null,
                    indication = null
                ) {
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
                .then(
                    if (canPlayNext) {
                        Modifier.clickable(
                            interactionSource = null,
                            indication = null
                        ) {
                            onNextClick()
                        }
                    } else {
                        Modifier
                    }
                )
        )
    }
}