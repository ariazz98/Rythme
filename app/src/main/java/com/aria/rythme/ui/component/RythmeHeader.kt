package com.aria.rythme.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun RythmeHeader(
    title: String,
    hasMoreMenu: Boolean,
    hasAvatar: Boolean,
    onMoreClick: (() -> Unit)? = null,
    onAvatarClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 21.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 标题
        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.rythmeColors.textColor,
            modifier = Modifier.weight(1f)
        )

        if (hasMoreMenu) {
            MenuItem(
                iconRes = R.drawable.ic_more,
                onClick = {
                    onMoreClick?.invoke()
                }
            )

            Spacer(modifier = Modifier.width(16.dp))
        }

        if (hasAvatar) {
            AvatarItem(
                url = "",
                name = "ARiA",
                onClick = {
                    onAvatarClick?.invoke()
                }
            )
        }
    }
}