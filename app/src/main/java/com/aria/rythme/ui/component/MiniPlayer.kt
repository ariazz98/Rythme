package com.aria.rythme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aria.rythme.R
import com.aria.rythme.core.extensions.customMarquee
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun MiniPlayer(
    song: Song?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.rythmeColors.bottomBackground)
            .clickable {
                onClick()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE9E9E9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_music),
                contentDescription = "",
                tint = Color(0xFFC6C6C6),
                modifier = Modifier.size(16.dp)
            )

            if (song?.coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(song.coverUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

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
            modifier = Modifier
                .padding(end = 21.dp)
                .size(28.dp)
                .clickable(
                    interactionSource = null,
                    indication = null
                ) {
                    onNextClick()
                }
        )
    }
}

@Preview
@Composable
fun MiniPlayerPreview() {
    MiniPlayer(null, false, {}, {}, {})
}