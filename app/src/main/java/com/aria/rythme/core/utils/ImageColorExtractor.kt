package com.aria.rythme.core.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 图片颜色提取工具
 * 
 * 提供从图片中提取颜色并生成渐变背景的功能。
 */
data class GradientColors(
    val top: Color = Color(0xFF6B6B6E),
    val center: Color = Color(0xFF6A6A6D),
    val bottom: Color = Color(0xFF404042)
)

val defaultGradientBrush = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF6B6B6E),
        Color(0xFF6A6A6D),
        Color(0xFF404042)
    )
)

object ImageColorExtractor {

    /**
     * 从 Bitmap 提取主题色，返回三段渐变颜色
     *
     * @param bitmap 图片 Bitmap
     * @return [GradientColors]，提取失败时返回默认灰色
     */
    suspend fun extractGradientColors(
        bitmap: Bitmap,
    ): GradientColors = withContext(Dispatchers.Default) {
        val palette = Palette.from(bitmap).generate()

        val vibrantColor = palette.vibrantSwatch?.rgb
        val darkVibrantColor = palette.darkVibrantSwatch?.rgb
        val lightVibrantColor = palette.lightVibrantSwatch?.rgb

        if (vibrantColor != null && darkVibrantColor != null && lightVibrantColor != null) {
            GradientColors(
                top = Color(lightVibrantColor),
                center = Color(vibrantColor),
                bottom = Color(darkVibrantColor)
            )
        } else {
            GradientColors()
        }
    }
}
