package com.aria.rythme.feature.pitch.presentation

import com.aria.rythme.feature.pitch.data.SongMelodyState
import androidx.compose.ui.res.painterResource
import com.kyant.capsule.ContinuousRoundedRectangle
import coil3.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.aria.rythme.ui.component.Action
import com.aria.rythme.ui.component.AnimatedHeaderActions
import com.aria.rythme.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.core.music.controller.PlaybackController
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.feature.pitch.data.SongMelodyManager
import com.aria.rythme.feature.pitch.service.SongMelodyAnalysisService
import com.aria.rythme.ui.theme.rythmeColors
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun PitchWorkspace(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    recordingState: PitchState,
    onPrimary: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    boundReference: SongMelodyState?,
    onReferenceSelected: (SongMelodyState?) -> Unit,
    onSelectStart: (Long) -> Unit,
    onConfigureModels: () -> Unit
) {
    val playback = koinInject<PlaybackController>()
    val manager = koinInject<SongMelodyManager>()
    val model: MelodyModelViewModel = koinViewModel()
    val queue by playback.queue.collectAsStateWithLifecycle()
    val playing by playback.isPlaying.collectAsStateWithLifecycle()
    val positionEvent by playback.currentPosition.collectAsStateWithLifecycle()
    val analysis by manager.state.collectAsStateWithLifecycle()
    var selectedSong by remember { mutableStateOf<Song?>(null) }
    val song = boundReference?.song ?: selectedSong
    var menu by remember { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    var referenceVisible by rememberSaveable { mutableStateOf(true) }
    val editable = recordingState.phase == PitchPhase.Idle && !recordingState.preparing
    val discontinuity by playback.positionDiscontinuity.collectAsStateWithLifecycle()
    val clock = remember(song?.id) { ReferencePlaybackClock { previous, raw ->
        if (com.aria.rythme.BuildConfig.DEBUG) android.util.Log.d("ReferenceClock", "backward correction held: $previous -> $raw")
    } }
    fun samplePosition() = clock.sample(playback.getCurrentPosition(), playback.positionDiscontinuity.value, playback.isPlaying.value)
    var position by remember(song?.id) { mutableLongStateOf(samplePosition()) }

    var checking by remember { mutableStateOf(false) }
    val active by rememberUpdatedState(enabled)
    val matches = song != null && analysis.song?.let { it.id == song.id && it.uri == song.uri && it.dateModified == song.dateModified && it.size == song.size } == true
    val colors = MaterialTheme.rythmeColors
    val toolbarBackdrop = rememberLayerBackdrop()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<Song?>(null) }
    var pending by remember { mutableStateOf<Song?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pending?.let { SongMelodyAnalysisService.start(context, manager, it) }; pending = null
    }
    LaunchedEffect(song?.id) { confirm = null }
    LaunchedEffect(positionEvent, discontinuity, song?.id) { position = samplePosition() }
    // Controller 的位置流只报告播放器事件；页面自行低频采样，绘制层再逐帧读取真实进度。
    LaunchedEffect(song?.id, playing, enabled) {
        position = samplePosition()
        while (playing && enabled) { delay(100); position = samplePosition() }
    }
    LaunchedEffect(analysis, selectedSong?.id) {
        if (selectedSong != null && matches && analysis.curve != null && editable) {
            onReferenceSelected(analysis)
            selectedSong = null
            referenceVisible = true
        }
    }
    val previewing = recordingState.phase == PitchPhase.Idle && queue.currentEntry?.song?.id == song?.id && playing
    val reference = boundReference?.let { bound ->
        ReferencePlayback(
            positionMs = if (previewing) position else recordingState.durationMs,
            totalMs = bound.curve!!.endMs,
            playing = previewing,
            currentPosition = { if (previewing) samplePosition() else recordingState.durationMs },
            frames = bound.history, visible = referenceVisible)
    }
    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 20.dp, vertical = 5.dp)) {
            Box(Modifier.matchParentSize().layerBackdrop(toolbarBackdrop).background(colors.surface))
            PitchGlassSurface(toolbarBackdrop, ContinuousRoundedRectangle(18.dp))
            Row(Modifier.fillMaxSize().padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (song?.coverUri != null) AsyncImage(song.coverUri, null,
                    Modifier.size(34.dp).clip(ContinuousRoundedRectangle(9.dp)), contentScale = ContentScale.Crop,
                    error = painterResource(R.drawable.ic_music), placeholder = painterResource(R.drawable.ic_music))
                else Icon(painterResource(R.drawable.ic_music), null, Modifier.size(25.dp), tint = colors.primary)
                Text(song?.title ?: "歌曲参考", Modifier.weight(1f).padding(horizontal = 12.dp),
                    color = colors.textColor, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                Box {
                    AnimatedHeaderActions(sourceKey = "pitch-reference", backdrop = toolbarBackdrop,
                        skipAnimation = true, enabled = enabled && !checking && (song != null || editable),
                        actions = listOf(Action.Icon("reference", if (song == null) R.drawable.ic_add else R.drawable.ic_more,
                            contentDescription = if (song == null) "添加歌曲参考" else "歌曲参考菜单",
                            onClick = { if (song == null) choosing = true else menu = true })))
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        if (boundReference != null) DropdownMenuItem(text = { Text(if (referenceVisible) "隐藏参考曲线" else "显示参考曲线") },
                            onClick = { referenceVisible = !referenceVisible; menu = false })
                        DropdownMenuItem(text = { Text("更换参考歌曲") }, enabled = editable,
                            onClick = { menu = false; choosing = true })
                        DropdownMenuItem(text = { Text("移除歌曲参考") }, enabled = editable,
                            onClick = { menu = false; selectedSong = null; onReferenceSelected(null) })
                    }
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            PitchCanvas(state = recordingState, enabled = enabled && (song == null || reference != null),
                onPrimary = onPrimary, onStop = onStop, onReset = onReset,
                onSelectStart = onSelectStart, modifier = Modifier.fillMaxSize(), reference = reference)
            if (song != null && reference == null) Column(Modifier.align(Alignment.Center).fillMaxWidth().padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (analysis.busy) {
                    if (!matches) Text("正在分析：${analysis.song?.title.orEmpty()}", color = colors.textColor)
                    LinearProgressIndicator(progress = { analysis.progress }, modifier = Modifier.fillMaxWidth(), color = colors.primary)
                    Text(analysis.stage, color = colors.subTitleColor)
                    TextButton(onClick = { SongMelodyAnalysisService.cancel(context) }) { Text("取消分析", color = colors.primary) }
                } else {
                    if (matches) analysis.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { scope.launch {
                        if (model.isReady()) { if (active) confirm = song }
                        else if (active) onConfigureModels()
                    } }, enabled = enabled) {
                        Text(if (matches && analysis.error != null) "重新分析参考旋律" else "准备参考旋律", color = colors.primary)
                    }
                }
            }
        }
    }
    if (choosing) AlertDialog(onDismissRequest = { choosing = false },
        title = { Text("添加歌曲参考") },
        text = { Text(queue.currentEntry?.song?.let { "使用播放器当前歌曲：${it.title}。参考将固定到本次录制。" }
            ?: "请先在播放器中选择一首歌曲。") },
        confirmButton = { TextButton(enabled = queue.currentEntry != null && !checking && editable, onClick = {
            val target = queue.currentEntry?.song ?: return@TextButton
            checking = true
            scope.launch {
                try {
                    if (model.isReady()) {
                        if (active) { onReferenceSelected(null); selectedSong = target; choosing = false }
                    } else if (active) { choosing = false; onConfigureModels() }
                } finally { checking = false }
            }
        }) { Text("使用当前歌曲") } },
        dismissButton = { TextButton(onClick = { choosing = false }) { Text("取消") } })
    confirm?.let { target ->
        val estimate = manager.estimateSeconds(target.duration)
        AlertDialog(onDismissRequest = { confirm = null }, containerColor = colors.surface,
            title = { Text("准备整首参考旋律", color = colors.textColor) },
            text = { Text("${target.title}\n整首 · ${target.durationText}\n\n" +
                (estimate?.let { "根据本机上次分析，预计约 ${maxOf(1, it / 2)}–${it * 2} 秒，实际可能不同。" } ?: "首次分析暂无可靠耗时估计，开始后显示进度与剩余时间。") +
                "\n\n在本机处理，不上传歌曲。分析会占用临时存储、内存和电量，可能发热；支持后台继续和取消。自动估计可能有误，不用于评分。已有结果会复用。", color = colors.textColor) },
            confirmButton = { TextButton(onClick = {
                confirm = null; pending = target
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    SongMelodyAnalysisService.start(context, manager, target); pending = null
                } else notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text("开始分析", color = colors.primary) } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("取消", color = colors.textColor) } })
    }
}
