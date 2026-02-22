package com.aria.rythme.feature.home.presentation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aria.rythme.R
import com.aria.rythme.core.navigation.Navigator
import com.aria.rythme.ui.component.LargeVerticalCard
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.component.MiddleVerticalCard
import com.aria.rythme.ui.component.SectionItem
import com.aria.rythme.ui.component.SmallSquareCard

/**
 * 主页页面
 * 包含专属精选推荐、最近播放等板块
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {

    MainListPage(
        titleRes = R.string.title_home,
        hasMoreMenu = false,
        hasAvatar = true,
        onAvatarClick = {
            // TODO: 打开个人资料
        }
    ) {
        // 专属精选推荐区块
        item {
            SectionItem(
                title = "专属精选推荐"
            ) {
                HorizontalPager(
                    state = rememberPagerState { 4 },
                    contentPadding = PaddingValues(horizontal = 21.dp),
                    pageSize = PageSize.Fixed(240.dp),
                    pageSpacing = 12.dp
                ) { page ->
                    when (page) {
                        0 -> {
                            MiddleVerticalCard(
                                title = "专属推荐",
                                innerDesc = "探索电台",
                                cover = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF52A99E),
                                        Color(0xFF2A4A77)
                                    )
                                )
                            )
                        }
                        1 -> {
                            MiddleVerticalCard(
                                title = "最新发行",
                                innerDesc = "爱是徜徉于浪漫理想和现实之间的过程",
                                cover = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFE8635E),
                                        Color(0xFF943B6E)
                                    )
                                )
                            )
                        }
                        2 -> {
                            MiddleVerticalCard(
                                title = "专属推荐",
                                innerDesc = "探索电台",
                                cover = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF52A99E),
                                        Color(0xFF2A4A77)
                                    )
                                )
                            )
                        }
                        3 -> {
                            MiddleVerticalCard(
                                title = "最新发行",
                                innerDesc = "爱是徜徉于浪漫理想和现实之间的过程",
                                cover = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFE8635E),
                                        Color(0xFF943B6E)
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }

        // 最近播放区块
        item {
            SectionItem(
                withContentPadding = true,
                title = "最近播放",
                onClick = {}
            ) {
                HorizontalPager(
                    state = rememberPagerState { 4 },
                    contentPadding = PaddingValues(horizontal = 21.dp),
                    pageSize = PageSize.Fixed(160.dp),
                    pageSpacing = 12.dp
                ) { page ->
                    when (page) {
                        0 -> {
                            SmallSquareCard(
                                title = "What a Day",
                                subTitle = "蔡徐坤",
                                cover = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF4A9FE8),
                                        Color(0xFF1E4A7A)
                                    )
                                )
                            )
                        }
                        1 -> {
                            SmallSquareCard(
                                title = "热门歌曲",
                                subTitle = "",
                                cover = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF6B6B),
                                        Color(0xFFE84A5F)
                                    )
                                )
                            )
                        }
                        2 -> {
                            SmallSquareCard(
                                title = "What a Day",
                                subTitle = "蔡徐坤",
                                cover = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF4A9FE8),
                                        Color(0xFF1E4A7A)
                                    )
                                )
                            )
                        }
                        3 -> {
                            SmallSquareCard(
                                title = "热门歌曲",
                                subTitle = "",
                                cover = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFF6B6B),
                                        Color(0xFFE84A5F)
                                    )
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionItem(
                title = "推荐歌单",
                withContentPadding = true
            ) {
                LargeVerticalCard(
                    cover = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4A9FE8),
                            Color(0xFF1E4A7A)
                        )
                    ),
                    innerDesc = "汲取百老汇的明亮热情"
                )
            }
        }
    }
}