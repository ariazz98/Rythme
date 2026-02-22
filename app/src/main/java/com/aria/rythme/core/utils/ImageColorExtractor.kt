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
object ImageColorExtractor {
    
    /**
     * 从 Bitmap 提取主题色并生成纵向渐变 Brush
     * 
     * 渐变效果：
     * - 顶部：主题色的亮色版本（RGB 值增加）
     * - 中间：提取的主题色
     * - 底部：主题色的暗色版本（RGB 值减小）
     * 
     * @param bitmap 图片 Bitmap
     * @return 纵向渐变 Brush
     */
    suspend fun extractGradientBrush(
        bitmap: Bitmap,
    ): Brush = withContext(Dispatchers.Default) {
        // 从图片中提取调色板
        val palette = Palette.from(bitmap).generate()
        
        // 获取主题色（优先级：Vibrant > Muted > DarkVibrant > LightVibrant）
        val vibrantColor = palette.vibrantSwatch?.rgb
        val darkVibrantColor = palette.darkVibrantSwatch?.rgb
        val lightVibrantColor = palette.lightVibrantSwatch?.rgb

        val centerColor: Color
        val topColor: Color
        val bottomColor: Color
        if (vibrantColor != null && darkVibrantColor != null && lightVibrantColor != null) {
            topColor = Color(lightVibrantColor)
            centerColor = Color(vibrantColor)
            bottomColor = Color(darkVibrantColor)
        } else {
            topColor = Color(0xFF6B6B6E)
            centerColor = Color(0xFF6A6A6D)
            bottomColor = Color(0xFF404042)
        }
        
        // 创建纵向渐变
        Brush.verticalGradient(
            colors = listOf(topColor, centerColor, bottomColor),
            startY = 0f,
            endY = Float.POSITIVE_INFINITY
        )
    }
}
