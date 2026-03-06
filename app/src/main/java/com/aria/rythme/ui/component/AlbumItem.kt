package com.aria.rythme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.capsule.ContinuousRoundedRectangle


@Composable
fun AlbumItem(
    album: Album,
    onClick: () -> Unit
) {
    val coverUri = album.coverUri
    val albumTitle = album.title
    val albumArtist = album.artist
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxWidth().clickable(
            interactionSource = null,
            indication = null
        ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(ContinuousRoundedRectangle(8.dp))
                .background(Color(0xFFE9E9EA)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_album),
                contentDescription = "",
                tint = Color(0xFFB5B5B8),
                modifier = Modifier.fillMaxSize(0.5f)
            )

            if (coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(coverUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = albumTitle,
            fontSize = 12.sp,
            color = MaterialTheme.rythmeColors.textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = albumArtist,
            fontSize = 12.sp,
            color = MaterialTheme.rythmeColors.subTitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}