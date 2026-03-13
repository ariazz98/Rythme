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
import com.aria.rythme.LocalSharedAlbumId
import com.aria.rythme.LocalContentSharedTransitionScope
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.capsule.ContinuousRoundedRectangle


@Composable
fun AlbumItem(
    album: Album,
    showArtist: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val coverUri = album.coverUri
    val albumTitle = album.title
    val albumArtist = album.artist
    val albumYear = album.year
    val context = LocalContext.current
    val sharedTransitionScope = LocalContentSharedTransitionScope.current
    val sharedAlbumId = LocalSharedAlbumId.current

    Column(
        modifier = modifier.fillMaxWidth().clickable(
            interactionSource = null,
            indication = null
        ) { onClick() }
    ) {
        with(sharedTransitionScope) {
            Box(
                modifier = Modifier
                    .sharedElementWithCallerManagedVisibility(
                        sharedContentState = rememberSharedContentState(key = "albumCover_${album.id}"),
                        visible = sharedAlbumId != album.id.toString()
                    )
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(ContinuousRoundedRectangle(8.dp))
                    .background(MaterialTheme.rythmeColors.coverBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_album),
                    contentDescription = "",
                    tint = MaterialTheme.rythmeColors.coverIcon,
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
            text = if (showArtist) albumArtist else (if (albumYear != 0) "${albumYear}年" else "未知年份"),
            fontSize = 12.sp,
            color = MaterialTheme.rythmeColors.subTitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
