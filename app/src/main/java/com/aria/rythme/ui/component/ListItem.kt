package com.aria.rythme.ui.component

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.Artist
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun LibraryListItem(
    icon: Int,
    title: Int,
    iconColor: Color,
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp, horizontal = 21.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 图标
            Icon(
                painter = painterResource(icon),
                contentDescription = stringResource(title),
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 标题
            Text(
                text = stringResource(title),
                fontSize = 17.sp,
                color = MaterialTheme.rythmeColors.textColor,
                modifier = Modifier.weight(1f)
            )

            // 右箭头
            Icon(
                painter = painterResource(R.drawable.ic_forward),
                contentDescription = null,
                tint = MaterialTheme.rythmeColors.weakColor,
                modifier = Modifier.size(12.dp)
            )
        }

        // 分割线
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 65.dp, end = 18.dp),
                thickness = DividerDefaults.Thickness,
                color = MaterialTheme.rythmeColors.weakColor
            )
        }
    }
}

@Composable
fun SongListItem(
    song: Song,
    showDivider: Boolean = true,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp, horizontal = 21.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CoverItem(
                size = 48.dp,
                corner = 6.dp,
                song = song,
                defaultBgColor = Color(0x99D6D6D5),
                defaultIconColor = Color(0xFF4A4A49)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.rythmeColors.textColor
                )
                Text(
                    text = song.artist,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.rythmeColors.subTitleColor
                )
            }

            Icon(
                painter = painterResource(R.drawable.ic_more),
                contentDescription = "",
                tint = MaterialTheme.rythmeColors.textColor,
                modifier = Modifier.size(18.dp).clickable(interactionSource = null, indication = null) { onMoreClick() }
            )
        }

        // 分割线
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp, end = 18.dp),
                thickness = DividerDefaults.Thickness,
                color = MaterialTheme.rythmeColors.weakColor
            )
        }
    }
}

@Composable
fun ArtistListItem(
    artist: Artist,
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp, horizontal = 21.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE9E9EA)),
                contentAlignment = Alignment.Center
            ) {
                // 图标
                Icon(
                    painter = painterResource(R.drawable.ic_artist),
                    contentDescription = "",
                    tint = Color(0xFFB5B5B8),
                    modifier = Modifier.size(24.dp)
                )

                if (artist.coverUri != null) {
                    AsyncImage(
                        model = artist.coverUri,
                        contentDescription = "cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 标题
            Text(
                text = artist.name,
                fontSize = 17.sp,
                color = MaterialTheme.rythmeColors.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // 右箭头
            Icon(
                painter = painterResource(R.drawable.ic_forward),
                contentDescription = null,
                tint = MaterialTheme.rythmeColors.weakColor,
                modifier = Modifier.size(12.dp)
            )
        }

        // 分割线
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 81.dp, end = 18.dp),
                thickness = DividerDefaults.Thickness,
                color = MaterialTheme.rythmeColors.weakColor
            )
        }
    }
}

@Composable
fun CommonListItem(
    title: String,
    showDivider: Boolean = true,
    onClick: () -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp, horizontal = 21.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 标题
            Text(
                text = title,
                fontSize = 17.sp,
                color = MaterialTheme.rythmeColors.textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // 右箭头
            Icon(
                painter = painterResource(R.drawable.ic_forward),
                contentDescription = null,
                tint = MaterialTheme.rythmeColors.weakColor,
                modifier = Modifier.size(12.dp)
            )
        }

        // 分割线
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 81.dp, end = 18.dp),
                thickness = DividerDefaults.Thickness,
                color = MaterialTheme.rythmeColors.weakColor
            )
        }
    }
}