package com.aria.rythme.ui.component

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun RythmeListItem(
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