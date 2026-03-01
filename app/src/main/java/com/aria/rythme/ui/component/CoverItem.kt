package com.aria.rythme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.Song
import com.kyant.capsule.ContinuousRoundedRectangle

@Composable
fun CoverItem(
    modifier: Modifier = Modifier,
    size: Dp,
    corner: Dp,
    song: Song?,
    defaultBgColor: Color,
    defaultIconColor: Color
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(ContinuousRoundedRectangle(corner))
            .background(defaultBgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music),
            contentDescription = "",
            tint = defaultIconColor,
            modifier = Modifier.size(size / 2)
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

}