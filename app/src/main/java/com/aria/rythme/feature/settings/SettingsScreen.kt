package com.aria.rythme.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.core.music.data.model.ScanProgress
import com.aria.rythme.ui.component.HeaderMode
import com.aria.rythme.ui.component.MainListPage
import com.aria.rythme.ui.theme.rythmeColors
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var editName by rememberSaveable { mutableStateOf(false) }
    var editScan by rememberSaveable { mutableStateOf(false) }
    val scanning = busy || state.progress is ScanProgress.Discovering || state.progress is ScanProgress.Syncing || state.progress is ScanProgress.PostProcessing
    MainListPage(title = "个人设置", headerMode = HeaderMode.HIDDEN) {
        item {
            Column(Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(state.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.rythmeColors.textColor)
                Text("仅保存在本机，不需要登录账号。", color = MaterialTheme.rythmeColors.subTitleColor)
                TextButton(onClick = { editName = true }, enabled = !busy) { Text("修改名称") }
                HorizontalDivider()
                Text("本地音乐", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.rythmeColors.textColor)
                Text("最短时长 ${state.scan.minDurationSeconds} 秒 · 最小文件 ${state.scan.minSizeBytes / 1024} KB", color = MaterialTheme.rythmeColors.subTitleColor)
                Text(if (state.scan.excludeSystemDirs) "排除铃声、闹钟及录音目录" else "包含系统音频目录", color = MaterialTheme.rythmeColors.subTitleColor)
                TextButton(onClick = { editScan = true }, enabled = !scanning) { Text("修改扫描规则") }
                Button(onClick = viewModel::rescan, enabled = !scanning) { Text(if (scanning) "正在扫描…" else "重新扫描音乐") }
                Text(when (val progress = state.progress) {
                    ScanProgress.Idle -> "扫描只更新曲库索引，不删除手机上的音乐文件。"
                    is ScanProgress.Discovering -> "正在查找音乐：${progress.foundCount} 首"
                    is ScanProgress.Syncing -> "正在更新：${progress.current} / ${progress.total}"
                    is ScanProgress.PostProcessing -> progress.step
                    is ScanProgress.Completed -> "扫描完成，共 ${progress.stats.totalSongs} 首"
                    is ScanProgress.Failed -> "扫描失败：${progress.error}"
                }, color = MaterialTheme.rythmeColors.subTitleColor)
                message?.let { Text(it, color = MaterialTheme.rythmeColors.textColor) }
                HorizontalDivider()
                Text("Rythme", fontSize = 21.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.rythmeColors.textColor)
                Text("属于你的本地音乐空间", color = MaterialTheme.rythmeColors.subTitleColor)
            }
        }
    }
    if (editName) {
        var name by rememberSaveable { mutableStateOf(state.name) }
        AlertDialog(onDismissRequest = { editName = false }, title = { Text("本地名称") }, text = {
            OutlinedTextField(name, { name = it }, label = { Text("名称（最多 30 字）") }, singleLine = true)
        }, confirmButton = { TextButton(onClick = { viewModel.saveName(name); editName = false }, enabled = name.trim().length in 1..30) { Text("保存") } },
            dismissButton = { TextButton(onClick = { editName = false }) { Text("取消") } })
    }
    if (editScan) {
        var seconds by rememberSaveable { mutableStateOf(state.scan.minDurationSeconds.toString()) }
        var kilobytes by rememberSaveable { mutableStateOf((state.scan.minSizeBytes / 1024).toString()) }
        var exclude by rememberSaveable { mutableStateOf(state.scan.excludeSystemDirs) }
        val duration = seconds.toLongOrNull()?.takeIf { it in 0..86400 }
        val size = kilobytes.toLongOrNull()?.takeIf { it in 0..1_048_576 }
        AlertDialog(onDismissRequest = { editScan = false }, title = { Text("扫描规则") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(seconds, { seconds = it }, label = { Text("最短时长（秒）") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(kilobytes, { kilobytes = it }, label = { Text("最小文件（KB）") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Row { Checkbox(exclude, { exclude = it }); Text("排除铃声、闹钟及录音目录", modifier = Modifier.padding(top = 12.dp)) }
                Text("保存后重新扫描。不符合规则的音乐会从曲库移除，原文件不会删除。")
            }
        }, confirmButton = { TextButton(onClick = {
            if (duration != null && size != null) viewModel.saveScan(state.scan.copy(minDurationMs = duration * 1000, minSizeBytes = size * 1024, excludeSystemDirs = exclude))
            editScan = false
        }, enabled = duration != null && size != null) { Text("保存并扫描") } },
            dismissButton = { TextButton(onClick = { editScan = false }) { Text("取消") } })
    }
}
