package com.aria.rythme.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.aria.rythme.ui.theme.rythmeColors

/**
 * 底部导航栏（Apple Music 风格）
 *
 * 四个主要标签页：
 * 1. 主页 - 专属推荐、最近播放
 * 2. 新发现 - 新专辑、排行榜
 * 3. 广播 - 电台、直播
 * 4. 资料库 - 本地音乐、歌单
 *
 * @param selectedKey 当前选中的标签索引
 * @param onSelectKey 标签选中回调
 */
@Composable
fun BottomNavigationBar(
    selectedKey: NavKey,
    onSelectKey: (NavKey) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(95.dp)
            .padding(horizontal = 21.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.rythmeColors.bottomBackground, RoundedCornerShape(30.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp)
            ) {
                TOP_LEVEL_DESTINATIONS.forEach { (route, item) ->
                    BottomNavigationItem(
                        modifier = Modifier
                            .weight(1f),
                        iconRes = item.icon,
                        titleRes = item.title,
                        isSelected = selectedKey == route,
                        onClick = { onSelectKey(route) }
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationItem(
    modifier: Modifier,
    iconRes: Int,
    titleRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (isSelected) MaterialTheme.rythmeColors.bottomSelected else MaterialTheme.rythmeColors.bottomUnselected,
                RoundedCornerShape(27.dp)
            )
            .clickable(
                interactionSource = null,
                indication = null
            ) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = "",
            tint = if (isSelected) MaterialTheme.rythmeColors.primary else MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .size(24.dp)
        )
        Text(
            text = stringResource(titleRes),
            color = if (isSelected) MaterialTheme.rythmeColors.primary else MaterialTheme.colorScheme.onBackground,
            fontSize = 10.sp,
            lineHeight = 10.sp
        )
    }
}

@Preview(showSystemUi = true)
@Composable
fun PreviewBottomBar() {
    BottomNavigationBar(
        selectedKey = RythmeRoute.Home,
        onSelectKey = {}
    )
}
