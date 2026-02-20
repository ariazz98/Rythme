package com.aria.rythme.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

data class RythmeColors(
    val primary: Color,
    val bottomSelected: Color,
    val bottomUnselected: Color,
    val bottomBackground: Color
)

private val LightRythmeColors = RythmeColors(
    primary = PrimaryPinkDark,
    bottomSelected = BottomBarSelected,
    bottomUnselected = BottomBarUnSelected,
    bottomBackground = BottomBarBackground
)

private val DarkRythmeColors = RythmeColors(
    primary = PrimaryPink,
    bottomSelected = BottomBarSelected,
    bottomUnselected = BottomBarUnSelected,
    bottomBackground = BottomBarBackgroundDark
)

/**
 * 本地扩展颜色 CompositionLocal
 */
val LocalRythmeColors = staticCompositionLocalOf {
    LightRythmeColors
}

/**
 * 获取扩展颜色的便捷函数
 */
val MaterialTheme.rythmeColors: RythmeColors
    @Composable
    get() = LocalRythmeColors.current

/**
 * Rythme 主题
 *
 * Apple Music 风格的配色方案，支持深浅色模式
 *
 * @param darkTheme 是否使用深色主题
 * @param content 内容
 */
@Composable
fun RythmeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkRythmeColors else LightRythmeColors

    CompositionLocalProvider(
        LocalRythmeColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}