package com.aria.rythme.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.aria.rythme.ui.theme.rythmeColors

/** 参考轮廓按手机宽度归一；视觉间距与连续点击槽分开。 */
internal object TopBarComponentMetrics {
    const val ReferenceWidth = 393f
    const val SurfaceHeight = 45f
    const val AvatarInset = 2.5f
    const val AvatarPressedScale = 1.36f
    const val GroupGap = 12f
    const val TrailingInset = 15.5f
    const val TitleInset = 19.5f
    const val ItemGap = 8f
    val rowPadding: Float get() = TrailingInset - GroupGap / 2f
    val titlePadding: Float get() = TitleInset - rowPadding

    // 同态对照专辑页 Filter + More：48 + 8 + 48，而非资料库复合图标的 110pt。
    fun itemWidth(count: Int): Float = if (count > 1) 48f else SurfaceHeight
    fun surfaceWidth(count: Int): Float = count.coerceAtLeast(0) * itemWidth(count) +
        (count - 1).coerceAtLeast(0) * ItemGap
    fun touchWidth(count: Int): Float = if (count > 0) surfaceWidth(count) / count else 0f
    fun iconOffset(count: Int, index: Int): Float = if (count > 1) {
        (index - (count - 1) / 2f) * ItemGap / count + 0.65f
    } else 0f
    fun referenceScale(widthDp: Float): Float =
        if (widthDp > 0f && widthDp.isFinite()) (widthDp / ReferenceWidth).coerceAtMost(1f) else 1f
    val avatarSize: Float get() = SurfaceHeight - AvatarInset * 2f
}

/** 固定标题行独立于 Android 字体的额外上下 padding，仍响应系统字号。 */
@Composable
internal fun TopBarTitle(text: String, referenceScale: Float = 1f, modifier: Modifier = Modifier) {
    Box(
        modifier.heightIn(min = (TopBarComponentMetrics.SurfaceHeight * referenceScale).dp).semantics { heading() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = (32f * referenceScale).sp,
                lineHeight = (38f * referenceScale).sp,
                // 第二轮 800 字重已获用户视觉确认，保留该档位。
                fontWeight = FontWeight.ExtraBold,
                textGeometricTransform = TextGeometricTransform(scaleX = 1.018f),
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            maxLines = 1,
            modifier = Modifier.offset(y = 0.75.dp),
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.rythmeColors.textColor
        )
    }
}

/** 只绘制头像前景；外壳及默认渐变由同一玻璃表面绘制，避免遮住高光。 */
@Composable
internal fun HeaderAvatarContent(action: Action.Avatar, referenceScale: Float, modifier: Modifier = Modifier) {
    Box(
        modifier.size((TopBarComponentMetrics.avatarSize * referenceScale).dp).clip(CircleShape)
            .semantics { contentDescription = action.contentDescription.ifEmpty { "头像" } },
        contentAlignment = Alignment.Center
    ) {
        if (!action.url.isNullOrEmpty()) {
            AsyncImage(
                model = action.url,
                contentDescription = action.contentDescription.ifEmpty { "头像" },
                modifier = Modifier.size((TopBarComponentMetrics.avatarSize * referenceScale).dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = action.name?.takeIf { it.isNotEmpty() && it != "未设置昵称" }?.take(2) ?: "R",
                color = Color.White,
                fontSize = (17.5f * referenceScale).sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.offset(y = (-0.5).dp),
                maxLines = 1
            )
        }
    }
}
