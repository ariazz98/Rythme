package com.aria.rythme.feature.pitch.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.withResumed
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.LocalInnerPadding
import com.aria.rythme.R
import com.aria.rythme.ui.component.*
import com.aria.rythme.ui.theme.rythmeColors
import org.koin.compose.viewmodel.koinViewModel

/** 根页面使用固定画布；纵向手势只移动音阶，不驱动全局底栏收起。 */
@Composable
fun PitchScreen(isActive: Boolean = true, onConfigureModels: () -> Unit, onOpenSongPractice: () -> Unit, viewModel: PitchViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val active by rememberUpdatedState(isActive)
    val scope = rememberCoroutineScope()
    var denied by rememberSaveable { mutableStateOf(false) }
    var discard by remember { mutableStateOf(false) }
    var settingsInfo by rememberSaveable { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(state.message) { message = state.message }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        denied = !granted
        if (granted) scope.launch {
            owner.lifecycle.withResumed { if (active) viewModel.start() }
        }
        else if (!granted) viewModel.permissionDenied()
    }
    fun beginRecording() {
        if (!active || !owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            denied = false
            viewModel.start()
        } else permission.launch(Manifest.permission.RECORD_AUDIO)
    }
    DisposableEffect(owner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) viewModel.onInactive()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); viewModel.onInactive() }
    }
    LaunchedEffect(isActive) { if (!isActive) viewModel.onInactive() }
    val title = stringResource(R.string.title_pitch)
    val entry = LocalTopBarEntry.current
    rememberPageHeader(
        title,
        TopBarConfig(
            auxiliaryActions = listOf(Action.Icon("song_practice", R.drawable.ic_song_practice, iconSize = 26.dp,
                contentDescription = "打开歌曲轨迹", onClick = onOpenSongPractice)),
            actions = listOf(Action.Avatar("avatar", name = "ARiA"))
        ),
        HeaderMode.HIDDEN, rememberPageSearchState(), hasSearch = false
    )
    SideEffect {
        entry.scroll.atTop = true
        entry.scroll.firstVisibleItemIndex = 0
        entry.scroll.firstVisibleItemScrollOffsetDp = 0f
    }
    val colors = MaterialTheme.rythmeColors
    Column(Modifier.fillMaxSize().background(colors.surface).padding(LocalInnerPadding.current)) {
        PitchCanvas(
            state = state,
            freeModeControls = true,
            enabled = isActive && !state.stopping && !state.preparing,
            onPrimary = {
                when (state.phase) {
                    PitchPhase.Idle, PitchPhase.Paused, PitchPhase.Finished -> beginRecording()
                    PitchPhase.Recording -> viewModel.pause()
                }
            },
            onStop = viewModel::stop,
            onReset = { if (state.frames.isEmpty()) viewModel.clearHistory() else { discard = true } },
            modifier = Modifier.weight(1f).fillMaxWidth()
        )
    }
    if (discard) GlassAlertDialog(
        onDismissRequest = { discard = false },
        title = { Text("清空轨迹？") },
        text = { Text("当前轨迹将被清空，监听会停止。") },
        confirmButton = { GlassDialogButton(onClick = {
            discard = false
            viewModel.clearHistory()
        }) { Text("清空") } },
        dismissButton = { GlassDialogButton(primary = false, onClick = { discard = false }) { Text("取消") } }
    )
    if (settingsInfo) AlertDialog(
        onDismissRequest = { settingsInfo = false },
        title = { Text("测量信息") },
        text = { Text("基准音：A4 = 440 Hz\n检测范围：55–1000 Hz\n音准偏差：相对最近音名\n本地单音检测，不保存录音") },
        confirmButton = { TextButton(onClick = { settingsInfo = false }) { Text("完成") } }
    )
    message?.let { text ->
        AlertDialog(
            onDismissRequest = { message = null; viewModel.dismissMessage() },
            text = { Text(text) },
            confirmButton = {
                TextButton(onClick = {
                    message = null
                    viewModel.dismissMessage()
                    if (denied) context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        "package:${context.packageName}".toUri()))
                }) { Text(if (denied) "权限设置" else "知道了") }
            },
            dismissButton = { if (denied) TextButton(onClick = { message = null; viewModel.dismissMessage() }) { Text("取消") } }
        )
    }
}
