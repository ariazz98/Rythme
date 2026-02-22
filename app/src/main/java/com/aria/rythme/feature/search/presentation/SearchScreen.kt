package com.aria.rythme.feature.search.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aria.rythme.R
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.ui.component.MainGridPage
import com.aria.rythme.ui.component.SmallCategoryCard

/**
 * 搜索页面
 * 包含搜索框和分类浏览卡片
 */
@Composable
fun SearchScreen(
    viewModel: SearchViewModel
) {

    MainGridPage(
        titleRes = R.string.title_search,
        hasMoreMenu = false,
        hasAvatar = true,
        onAvatarClick = {
            // TODO: 打开个人资料
        }
    ) {
        // 农历新年
        item {
            SmallCategoryCard(
                title = "农历新年",
                cover = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF87CEEB),
                        Color(0xFFFFB6C1)
                    )
                )
            )
        }

        // C-Pop
        item {
            SmallCategoryCard(
                title = "C-Pop",
                cover = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE85D75),
                        Color(0xFFFF8FA3)
                    )
                )
            )
        }

        // 爱
        item {
            SmallCategoryCard(
                title = "爱",
                cover = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE8D5C4),
                        Color(0xFFF5EBE0)
                    )
                )
            )
        }

        // 空间音频
        item {
            SmallCategoryCard(
                title = "空间音频",
                cover = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE85D75),
                        Color(0xFFFF6B6B)
                    )
                )
            )
        }

        // 国语流行
        item {
            SmallCategoryCard(
                title = "国语流行",
                cover = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFD4729B),
                        Color(0xFFFF9EC5)
                    )
                )
            )
        }

        // DJ 混音精选
        item {
            SmallCategoryCard(
                title = "DJ 混音精选",
                cover = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFB71C1C),
                        Color(0xFFE53935)
                    )
                )
            )
        }

        // 月度音乐回忆
        item {
            SmallCategoryCard(
                title = "月度音乐回忆",
                cover = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFB347),
                        Color(0xFF64B5F6)
                    )
                )
            )
        }

        // 排行榜
        item {
            SmallCategoryCard(
                title = "排行榜",
                cover = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6B7C3D),
                        Color(0xFF8FA456)
                    )
                )
            )
        }

        // 爵士乐
        item {
            SmallCategoryCard(
                title = "爵士乐",
                cover = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4A9FD8),
                        Color(0xFF64B5F6)
                    )
                )
            )
        }

        // 创作与制作
        item {
            SmallCategoryCard(
                title = "创作与制作",
                cover = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF7C7C3D),
                        Color(0xFF9E9E5A)
                    )
                )
            )
        }

        // 嘻哈 / 说唱
        item {
            SmallCategoryCard(
                title = "嘻哈 / 说唱",
                cover = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF5C6BC0),
                        Color(0xFF7986CB)
                    )
                )
            )
        }

        // 古典音乐
        item {
            SmallCategoryCard(
                title = "古典音乐",
                cover = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF7B1FA2),
                        Color(0xFF9C27B0)
                    )
                )
            )
        }
    }
}