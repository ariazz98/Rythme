package com.aria.rythme.feature.pitch.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import com.aria.rythme.LocalInnerPadding
import com.aria.rythme.R
import com.aria.rythme.ui.component.Action
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.component.TopBarConfig
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.capsule.ContinuousRoundedRectangle

/** 当前阶段仅呈现布局，不申请录音权限、不启用麦克风，也不伪造检测数据。 */
@Composable
fun PitchScreen() {
    val colors = MaterialTheme.rythmeColors
    val density = LocalDensity.current
    val insets = LocalInnerPadding.current
    val availableHeight = with(density) { LocalWindowInfo.current.containerSize.height.toDp() } -
        insets.calculateTopPadding() - insets.calculateBottomPadding()
    // 优先让监听区域位于首屏，较矮窗口仍可滚动，不把控制区压在 MiniPlayer 后面。
    val chartHeight = (availableHeight - 390.dp).coerceIn(150.dp, 260.dp)
    MainListPage(
        title = stringResource(R.string.title_pitch),
        topBar = TopBarConfig(
            auxiliaryActions = listOf(Action.Icon("pitch_settings", R.drawable.ic_filter, contentDescription = "测量设置（待接入）")),
            actions = listOf(Action.Avatar("avatar", name = "ARiA"))
        )
    ) {
        item {
            Column(Modifier.padding(horizontal = 21.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("观察声音的轨迹", fontSize = 15.sp, color = colors.subTitleColor)
                    Text("布局预览", fontSize = 11.sp, color = colors.primary,
                        modifier = Modifier.clip(ContinuousRoundedRectangle(8.dp)).background(colors.primary.copy(alpha = 0.08f)).padding(horizontal = 9.dp, vertical = 5.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PitchReading("当前音名", "—", "音名 / 八度")
                    PitchReading("基频", "—", "Hz")
                    PitchReading("音准偏差", "—", "cents")
                }
                Row(Modifier.fillMaxWidth().clip(ContinuousRoundedRectangle(12.dp)).background(colors.coverBg).padding(4.dp)) {
                    Box(Modifier.weight(1f).clip(ContinuousRoundedRectangle(9.dp)).background(colors.surface).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text("自由观察", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textColor)
                    }
                    Box(Modifier.weight(1f).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                        Text("目标音对照", fontSize = 14.sp, color = colors.subTitleColor)
                    }
                }
                EmptyPitchChart(chartHeight)
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(disabledContainerColor = colors.primary.copy(alpha = 0.10f), disabledContentColor = colors.primary.copy(alpha = 0.55f))) {
                    Icon(painterResource(R.drawable.ic_mic), null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("开始监听", fontSize = 16.sp)
                }
                Text("麦克风尚未接入。本轮只预览页面布局，不采集或保存声音。", fontSize = 12.sp, color = colors.subTitleColor)
            }
        }
    }
}

@Composable
private fun PitchReading(label: String, value: String, unit: String) {
    val colors = MaterialTheme.rythmeColors
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, fontSize = 12.sp, color = colors.subTitleColor)
        Text(value, fontSize = 36.sp, fontWeight = FontWeight.Light, color = colors.textColor)
        Text(unit, fontSize = 11.sp, color = colors.subTitleColor)
    }
}

@Composable
private fun EmptyPitchChart(height: Dp) {
    val colors = MaterialTheme.rythmeColors
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("音高轨迹", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textColor)
            Text("C3 — C5", fontSize = 12.sp, color = colors.subTitleColor)
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(height).clip(ContinuousRoundedRectangle(18.dp)).background(colors.coverBg.copy(alpha = 0.42f))) {
            Canvas(Modifier.fillMaxSize().padding(start = 36.dp, end = 12.dp, top = 20.dp, bottom = 20.dp)) {
                for (index in 0..24) {
                    val y = size.height * index / 24f
                    drawLine(colors.subTitleColor.copy(alpha = if (index % 12 == 0) 0.20f else 0.07f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
                for (index in 0..6) {
                    val x = size.width * index / 6f
                    drawLine(colors.subTitleColor.copy(alpha = 0.07f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                }
            }
            Column(Modifier.fillMaxHeight().padding(start = 9.dp, top = 14.dp, bottom = 14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                listOf("C5", "C4", "C3").forEach { Text(it, fontSize = 10.sp, color = colors.subTitleColor) }
            }
            Column(Modifier.align(Alignment.Center).background(colors.surface.copy(alpha = 0.92f), ContinuousRoundedRectangle(12.dp)).padding(horizontal = 20.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("尚无音高输入", fontSize = 14.sp, color = colors.textColor)
                Text("监听接入后，在这里显示实时曲线", fontSize = 11.sp, color = colors.subTitleColor)
            }
        }
        Text("时间 →", modifier = Modifier.align(Alignment.End).padding(top = 5.dp), fontSize = 11.sp, color = colors.subTitleColor)
    }
}
