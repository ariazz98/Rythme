package com.aria.rythme.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.R
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun SectionItem(
    withContentPadding: Boolean = false,
    title: String? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
    ) {
        // 标题行
        if (!title.isNullOrEmpty()) {
            Row(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(horizontal = 21.dp)
                    .clickable(
                        interactionSource = null,
                        indication = null
                    ) { onClick?.invoke() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.rythmeColors.textColor
                )

                if (onClick != null) {
                    Icon(
                        painter = painterResource(R.drawable.ic_forward),
                        contentDescription = "查看更多",
                        tint = MaterialTheme.rythmeColors.subTitleColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (withContentPadding) {
            Spacer(modifier = Modifier.height(18.dp))
        }
        content()
    }
}