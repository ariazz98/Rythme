package com.aria.rythme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * 小方形卡片
 */
@Composable
fun SmallSquareCard(
    cover: Brush,
    title: String?,
    subTitle: String?
) {
    Column(
        modifier = Modifier.width(160.dp)
            .wrapContentHeight()
            .clickable {
                // TODO: 点击播放
            }
    ) {
        // 封面
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(ContinuousRoundedRectangle(8.dp))
                .background(cover)
        ) {
            // Apple Music 标志
            Text(
                text = "Rythme",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 5.dp, end = 8.dp)
            )
        }

        // 标题信息
        Text(
            text = title ?: "",
            fontSize = 12.sp,
            color = MaterialTheme.rythmeColors.textColor,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 1
        )

        Text(
            text = subTitle ?: "",
            fontSize = 12.sp,
            color = MaterialTheme.rythmeColors.subTitleColor,
            maxLines = 1
        )
    }
}

@Composable
fun MiddleVerticalCard(
    title: String? = null,
    innerDesc: String? = null,
    cover: Brush
) {
    Column(
        modifier = Modifier
            .width(240.dp)
            .wrapContentHeight()
            .clickable {
                // TODO: 点击卡片
            }
    ) {
        Text(
            text = title ?: "",
            fontSize = 14.sp,
            color = MaterialTheme.rythmeColors.subTitleColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .width(240.dp)
                .height(320.dp)
                .clip(ContinuousRoundedRectangle(16.dp))
                .background(cover)
        ) {
            Text(
                text = "Rythme",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 12.dp)
            )

            if (!innerDesc.isNullOrEmpty()) {
                Text(
                    text = innerDesc,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun LargeVerticalCard(
    cover: Brush,
    innerDesc: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(450.dp)
            .padding(horizontal = 21.dp)
            .clip(ContinuousRoundedRectangle(24.dp))
            .background(cover)
    ) {
        if (!innerDesc.isNullOrEmpty()) {
            Text(
                text = innerDesc,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun MiddleHorizontalCard(
    cover: Brush,
    title: String? = null,
    desc: String? = null,
    subTitle: String? = null,
    innerDesc: String? = null
) {

    Column(
        modifier = Modifier
            .width(330.dp)
            .wrapContentHeight()
            .clickable {
                // TODO: 点击卡片
            }
    ) {

        Text(
            text = title ?: "",
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.rythmeColors.subTitleColor
        )

        Text(
            text = desc ?: "",
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.rythmeColors.textColor
        )

        Text(
            text = subTitle ?: "",
            fontSize = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.rythmeColors.subTitleColor,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .width(330.dp)
                .height(220.dp)
                .clip(ContinuousRoundedRectangle(21.dp))
                .background(cover)
        ) {
            if (!innerDesc.isNullOrEmpty()) {
                Text(
                    text = innerDesc,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun SmallCategoryCard(
    cover: Brush,
    title: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.8f)
            .clip(ContinuousRoundedRectangle(18.dp))
            .background(cover)
            .clickable {
                // TODO: 点击卡片
            }
    ) {
        if (!title.isNullOrEmpty()) {
            // 分类标题
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            )
        }
    }
}