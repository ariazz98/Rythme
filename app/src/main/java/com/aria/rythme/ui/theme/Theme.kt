package com.aria.rythme.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle

data class RythmeColors(
    val primary: Color,
    val bottomSelected: Color,
    val bottomUnselected: Color,
    val bottomBackground: Color,
    val textColor: Color,
    val weakColor: Color,
    val subTitleColor: Color,
    val surface: Color,
    val capsuleIconBg: Color,
    val miniCoverBg: Color,
    val coverBg: Color,
    val miniCoverIcon: Color,
    val coverIcon: Color,
    val searchBg: Color,
    val artistCoverBg: Color,
    val miniNextWeak: Color,
)

private val LightRythmeColors = RythmeColors(
    primary = PrimaryPinkDark,
    bottomSelected = BottomBarSelected,
    bottomUnselected = BottomBarUnSelected,
    bottomBackground = BottomBarBackground,
    textColor = Color.Black,
    weakColor = Gray4,
    subTitleColor = SubTitleColor,
    surface = SurfaceBackground,
    capsuleIconBg = SurfaceWeakBgColor,
    miniCoverBg = CoverMiniBgColor,
    coverBg = AlbumCoverBg,
    miniCoverIcon = CoverMiniIconColor,
    coverIcon = Color(0xFFB5B5B8),
    searchBg = Color(0xFFEBEBEB),
    artistCoverBg = Color(0xFFE9E9EA),
    miniNextWeak = Color(0xFFBFBFBE)
)

private val DarkRythmeColors = RythmeColors(
    primary = PrimaryPink,
    bottomSelected = BottomBarSelectedDark,
    bottomUnselected = BottomBarUnSelected,
    bottomBackground = BottomBarBackgroundDark,
    textColor = Color.White,
    weakColor = Gray4Dark,
    subTitleColor = SubTitleColor,
    surface = SurfaceBackgroundDark,
    capsuleIconBg = SurfaceWeakBgColorDark,
    miniCoverBg = CoverMiniBgColorDark,
    coverBg = AlbumCoverBgDark,
    miniCoverIcon = CoverMiniIconColorDark,
    coverIcon = Color(0xFF616165),
    searchBg = Color(0xFF1E1E1E),
    artistCoverBg = Color(0xFFE9E9EA),
    miniNextWeak = Color(0xFF4F4F4F)
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
 * 创建紧凑的文本样式
 * 移除字体默认内边距，使文本高度更精确地匹配字号大小
 */
private fun createCompactTextStyle(baseStyle: TextStyle): TextStyle {
    return baseStyle.copy(
        platformStyle = PlatformTextStyle(
            includeFontPadding = false
        ),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.Both
        )
    )
}

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
    val extendedColors = if (darkTheme) DarkRythmeColors else LightRythmeColors

    // 创建紧凑的字体配置
    val compactTypography = Typography(
        displayLarge = createCompactTextStyle(Typography.displayLarge),
        displayMedium = createCompactTextStyle(Typography.displayMedium),
        displaySmall = createCompactTextStyle(Typography.displaySmall),
        headlineLarge = createCompactTextStyle(Typography.headlineLarge),
        headlineMedium = createCompactTextStyle(Typography.headlineMedium),
        headlineSmall = createCompactTextStyle(Typography.headlineSmall),
        titleLarge = createCompactTextStyle(Typography.titleLarge),
        titleMedium = createCompactTextStyle(Typography.titleMedium),
        titleSmall = createCompactTextStyle(Typography.titleSmall),
        bodyLarge = createCompactTextStyle(Typography.bodyLarge),
        bodyMedium = createCompactTextStyle(Typography.bodyMedium),
        bodySmall = createCompactTextStyle(Typography.bodySmall),
        labelLarge = createCompactTextStyle(Typography.labelLarge),
        labelMedium = createCompactTextStyle(Typography.labelMedium),
        labelSmall = createCompactTextStyle(Typography.labelSmall)
    )

    CompositionLocalProvider(
        LocalRythmeColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColorScheme(
                surface = SurfaceBackgroundDark,
                background = SurfaceBackgroundDark
            ) else lightColorScheme(
                surface = SurfaceBackground,
                background = SurfaceBackground
            ),
            typography = compactTypography,
            content = content
        )
    }
}