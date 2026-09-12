package com.aria.rythme.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.Song
import com.kyant.capsule.ContinuousRoundedRectangle
import androidx.core.graphics.scale

@Composable
fun CoverItem(
    modifier: Modifier = Modifier,
    size: Dp,
    corner: Dp,
    song: Song?,
    defaultBgColor: Color,
    defaultIconColor: Color,
    onBitmapReady: ((Bitmap?) -> Unit)? = null,
    cachePlaceholderForTransition: Boolean = false
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
            // 缓存尺寸不跟随共享边界逐帧变化；显示尺寸仍严格为封面的一半。
            // 192dp 覆盖当前最大封面的图标尺寸，避免过渡时反复重建矢量位图。
            modifier = Modifier.fillMaxSize(.5f).then(if (cachePlaceholderForTransition) Modifier.layout { measurable, constraints ->
                val cacheSize = 192.dp.roundToPx()
                val icon = measurable.measure(Constraints.fixed(cacheSize, cacheSize))
                layout(constraints.maxWidth, constraints.maxHeight) {
                    icon.placeWithLayer(0, 0) {
                        transformOrigin = TransformOrigin(0f, 0f)
                        scaleX = constraints.maxWidth.toFloat() / cacheSize
                        scaleY = constraints.maxHeight.toFloat() / cacheSize
                    }
                }
            } else Modifier)
        )

        val context = LocalContext.current
        Crossfade(
            targetState = song?.coverUri,
            animationSpec = tween(500),
            label = "cover_crossfade"
        ) { coverUri ->
            if (coverUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(coverUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onSuccess = { state ->
                        if (onBitmapReady != null) {
                            val original = state.result.image
                                .asDrawable(context.resources)
                                .toBitmap()
                            val software = original.copy(Bitmap.Config.ARGB_8888, false)
                            val small = software.scale(100, 100)
                            software.recycle()
                            onBitmapReady(small)
                        }
                    },
                    onError = {
                        onBitmapReady?.invoke(null)
                    }
                )
            } else {
                onBitmapReady?.invoke(null)
            }
        }
    }

}
