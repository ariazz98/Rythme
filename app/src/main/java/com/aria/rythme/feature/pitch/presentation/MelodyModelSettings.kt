package com.aria.rythme.feature.pitch.presentation

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.feature.pitch.data.ModelPhase
import com.aria.rythme.feature.pitch.service.MelodyModelDownloadService
import com.aria.rythme.ui.theme.rythmeColors
import org.koin.compose.viewmodel.koinViewModel
import java.util.Locale
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import com.aria.rythme.feature.pitch.data.SongMelodyManager

@Composable
internal fun MelodyModelSettings(modifier: Modifier = Modifier, configurationRequired: Boolean = false, viewModel: MelodyModelViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val analysisManager = koinInject<SongMelodyManager>()
    val analysis by analysisManager.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var cacheBytes by remember { mutableLongStateOf(0L) }
    LaunchedEffect(analysis.busy, analysis.curve) { cacheBytes = analysisManager.cacheBytes() }
    val context = LocalContext.current
    val colors = MaterialTheme.rythmeColors
    var allowMetered by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val metered by rememberUpdatedState(allowMetered)
    var pendingImport by rememberSaveable { mutableStateOf<String?>(null) }
    fun startPending() {
        val uri = pendingImport
        pendingImport = null
        if (uri == null) MelodyModelDownloadService.start(context, viewModel.manager, metered)
        else MelodyModelDownloadService.importFile(context, viewModel.manager, uri.toUri())
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        // 通知拒绝不剥夺准备模型的能力，进度仍可在页面查看。
        startPending()
    }
    fun requestStart(uri: Uri? = null) {
        pendingImport = uri?.toString()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else startPending()
    }
    val pickModel = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) requestStart(uri)
    }
    CompositionLocalProvider(LocalContentColor provides colors.textColor) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("歌曲对照模型", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textColor)
        if (configurationRequired && state.phase != ModelPhase.Ready) {
            Text("歌曲对照需要先准备兼容模型，请在这里完成配置。", color = colors.primary)
        }
        Text(when (state.phase) {
            ModelPhase.Ready -> "模型已就绪"
            ModelPhase.Downloading -> "正在下载模型"
            ModelPhase.Importing -> "正在导入模型"
            ModelPhase.Verifying -> "正在校验模型"
            ModelPhase.Paused -> "模型下载已暂停"
            ModelPhase.Failed -> "模型尚未就绪"
            else -> "启用歌曲对照"
        }, color = colors.textColor, fontWeight = FontWeight.Medium)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (state.phase) {
                    ModelPhase.Checking -> { LinearProgressIndicator(Modifier.fillMaxWidth(), color = colors.primary); Text("正在检查本地模型…") }
                    ModelPhase.Ready -> {
                        Text("RMVPE · 345 MiB\n已安装并通过完整性校验。")
                        Text(if (analysis.busy) "正在分析歌曲，完成或取消后可删除模型。" else "可在歌曲对照中分析当前歌曲，已生成的参考轨迹会缓存复用。")
                        TextButton(onClick = { confirmDelete = true }, enabled = !analysis.busy) { Text("删除模型", color = colors.primary) }
                    }
                    else -> {
                        if (!state.transferring && state.bytes == 0L) {
                            Text("歌曲对照使用模型在本机提取参考旋律。此功能可选，自由录制不受影响。")
                            Text("需下载约 345 MiB，下载完成后占用约 345 MiB 存储。下载时间取决于网络速度。")
                            Text("后续整首分析会增加内存、耗电及发热，耗时取决于歌曲长度与设备，目前尚无可靠估计。歌曲不上传，结果可能有误。")
                        }
                        if (state.transferring || state.bytes > 0 || state.phase == ModelPhase.Failed) {
                            LinearProgressIndicator(progress = { (state.bytes.toFloat() / viewModel.manager.totalBytes).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth(), color = colors.primary)
                            Text("${mib(state.bytes)} / ${mib(viewModel.manager.totalBytes)} MiB")
                            if (state.phase == ModelPhase.Verifying) Text("正在检查文件完整性，校验通过后才会标记为就绪。")
                        }
                        state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        Text(if (state.transferring) "关闭窗口或切到后台会继续处理，可通过通知停止。" else "支持后台下载，暂停后保留进度。模型来自 Hugging Face，网络不可达时可稍后重试。")
                        if (!state.transferring) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Checkbox(checked = allowMetered, onCheckedChange = { allowMetered = it }, colors = CheckboxDefaults.colors(checkedColor = colors.primary))
                                Text("允许使用移动数据等计费网络")
                            }
                            Text("也可导入 RMVPE（RVC v1）的 rmvpe.onnx 文件，系统会验证是否为兼容版本。")
                            TextButton(onClick = { pickModel.launch(arrayOf("*/*")) }) { Text("导入已下载模型", color = colors.primary) }
                            TextButton(onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW,
                                    com.aria.rythme.feature.pitch.data.MelodyModelSpec.Recommended.url.toUri()))
                            }) { Text("打开模型官方下载地址", color = colors.primary) }
                            if (state.bytes > 0) TextButton(onClick = { confirmDelete = true }) { Text("清除已下载文件", color = colors.primary) }
                        }
                    }
                }
            }
            if (cacheBytes > 0) {
                Text("参考轨迹缓存 · ${mib(cacheBytes)} MiB")
                TextButton(enabled = !analysis.busy, onClick = { scope.launch {
                    analysisManager.clearCache(); cacheBytes = analysisManager.cacheBytes()
                } }) { Text("清理参考轨迹缓存", color = colors.primary) }
            }
            when {
                state.phase == ModelPhase.Ready -> Unit
                state.transferring -> TextButton(onClick = { MelodyModelDownloadService.pause(context) }) { Text(if (state.importing) "取消导入" else "暂停", color = colors.primary) }
                state.phase != ModelPhase.Checking -> TextButton(onClick = { requestStart() }) {
                    Text(if (state.bytes > 0) "继续下载" else if (state.phase == ModelPhase.Failed) "重试下载" else "下载模型", color = colors.primary)
                }
            }
    }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false }, containerColor = colors.surface,
        title = { Text(if (state.phase == ModelPhase.Ready) "删除模型？" else "清除下载进度？", color = colors.textColor) },
        text = { Text("将释放 ${mib(state.bytes)} MiB 存储，再次使用需要重新下载。自由录制不受影响。", color = colors.textColor) },
        confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete() }) { Text("删除", color = colors.primary) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消", color = colors.textColor) } }
    )
}
private fun mib(bytes: Long) = String.format(Locale.ROOT, "%.1f", bytes / 1048576.0)
