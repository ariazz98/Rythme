package com.aria.rythme.feature.pitch.presentation

import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.semantics.Role
import com.aria.rythme.feature.player.presentation.PlayerPressFeedback
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.aria.rythme.core.navigation.*
import com.aria.rythme.feature.navigationbar.domain.model.SongPracticeRoute
import com.aria.rythme.LocalBackdrop
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import com.kyant.capsule.ContinuousCapsule
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.withResumed
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.aria.rythme.R
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.feature.pitch.data.ParsedMelodyEntry
import com.aria.rythme.feature.pitch.service.SongMelodyAnalysisService
import com.aria.rythme.feature.player.presentation.PlayerOverlayMotion
import com.aria.rythme.ui.component.*
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** 独立页面栈，复用主应用标题栏和 Navigation3 进退动画。 */
@Composable
fun SongPracticeOverlay(
    onDismiss: () -> Unit,
    onConfigureModels: () -> Unit,
    configurationVisible: Boolean,
    viewModel: SongPracticeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val entries by viewModel.library.entries.collectAsStateWithLifecycle()
    val analysis by viewModel.analysis.state.collectAsStateWithLifecycle()
    val initializing by viewModel.initializing.collectAsStateWithLifecycle()
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val model: MelodyModelViewModel = koinViewModel()
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val view = LocalView.current
    val latestConfigurationVisible by rememberUpdatedState(configurationVisible)
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    val height = with(LocalDensity.current) { LocalWindowInfo.current.containerDpSize.height.toPx() }
    val latestDismiss by rememberUpdatedState(onDismiss)
    val navigation = rememberNavigationState(SongPracticeRoute.Library, setOf(SongPracticeRoute.Library))
    val navigator = remember(navigation) { Navigator(navigation) }
    val topBar = rememberTopBarState()
    var query by remember { mutableStateOf("") }
    val overlayMenu = LocalOverlayMenu.current
    var deleting by remember { mutableStateOf<ParsedMelodyEntry?>(null) }
    var confirm by remember { mutableStateOf<Song?>(null) }
    var pendingModel by remember { mutableStateOf<Song?>(null) }
    var pendingNotification by remember { mutableStateOf<Song?>(null) }
    var checkingModel by remember { mutableStateOf(false) }
    fun close() { if (!closing) { viewModel.setForeground(false); closing = true } }
    fun back() {
        if (overlayMenu.isVisible) { overlayMenu.dismiss(); return }
        if (navigation.currentRoute == SongPracticeRoute.Detail) viewModel.pause()
        if (!navigator.goBack()) close()
    }
    BackHandler(enabled = !configurationVisible, onBack = ::back)
    LaunchedEffect(Unit) { viewModel.refreshLibrary() }
    LaunchedEffect(closing) {
        progress.animateTo(if (closing) 0f else 1f,
            tween(if (closing) PlayerOverlayMotion.CollapseMs else PlayerOverlayMotion.ExpandMs,
                easing = if (closing) PlayerOverlayMotion.CollapseEasing else PlayerOverlayMotion.ExpandEasing))
        if (closing) { viewModel.closeSong(); latestDismiss() }
    }
    DisposableEffect(owner, viewModel) {
        viewModel.setForeground(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && !latestConfigurationVisible)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) viewModel.setForeground(false)
            else if (event == Lifecycle.Event.ON_RESUME) viewModel.setForeground(!latestConfigurationVisible)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); viewModel.closeSong() }
    }
    DisposableEffect(view, state.entry?.key) {
        val previous = view.keepScreenOn
        if (state.entry != null) view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
    LaunchedEffect(configurationVisible) {
        viewModel.setForeground(!configurationVisible && owner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
        if (!configurationVisible) pendingModel?.let { target ->
            pendingModel = null
            if (model.isReady()) confirm = target
        }
    }
    val microphone = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scope.launch {
            owner.lifecycle.withResumed { if (!closing && !latestConfigurationVisible) {
                viewModel.setForeground(true); viewModel.toggle()
            } }
        }
        else if (!granted) viewModel.message("需要麦克风权限才能实时跟唱。可在系统应用权限中开启。")
    }
    val notification = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingNotification?.let { SongMelodyAnalysisService.start(context, viewModel.analysis, it) }
        pendingNotification = null
    }
    fun select(song: Song) {
        if (checkingModel || analysis.busy) return
        checkingModel = true
        scope.launch {
            try {
                if (model.isReady()) { if (navigation.currentRoute == SongPracticeRoute.Picker) navigator.goBack(); confirm = song }
                else { if (navigation.currentRoute == SongPracticeRoute.Picker) navigator.goBack(); pendingModel = song; onConfigureModels() }
            } finally { checkingModel = false }
        }
    }
    fun entryMenu(entry: ParsedMelodyEntry): List<MenuConfig> = buildList {
        if (!analysis.busy) add(MenuConfig.Item(iconRes = R.drawable.ic_pitch_reset, titleRes = R.string.pitch_reanalyze, onClick = {
            overlayMenu.dismiss()
            songs.firstOrNull { it.id == entry.songId }?.let(::select) ?: viewModel.message("原歌曲已不可用，请添加新的歌曲")
        }))
        add(MenuConfig.Item(iconRes = R.drawable.ic_pitch_clear, titleRes = R.string.pitch_remove_analysis, onClick = {
            overlayMenu.dismiss(); deleting = entry
        }))
    }
    run {
        val colors = MaterialTheme.rythmeColors
        val backdrop = rememberLayerBackdrop()
        val targetHeader = topBar.find(SongPracticeRoute.Library, navigation.currentRoute)
        var previousHeader by remember { mutableStateOf<TopBarEntry?>(null) }
        SideEffect { if (targetHeader != null) previousHeader = targetHeader }
        val header = targetHeader ?: previousHeader
        Box(Modifier.fillMaxSize().then(if (configurationVisible) Modifier.clearAndSetSemantics { } else Modifier)
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent(PointerEventPass.Final) } }
            .graphicsLayer { translationY = (1f - progress.value) * height }
            .background(colors.surface).semantics { paneTitle = "歌曲轨迹" }) {
            CompositionLocalProvider(LocalBackdrop provides backdrop) {
                Scaffold(containerColor = colors.surface, topBar = {
                    if (header != null) RythmeHeader(header, profileName = "", enabled = !closing && !configurationVisible,
                        isRoot = header.route == SongPracticeRoute.Library,
                        showDivider = false, onBackClick = ::back)
                }) { padding ->
                    NavDisplay(modifier = Modifier.fillMaxSize().layerBackdrop(backdrop), onBack = ::back,
                        transitionSpec = {
                            slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { it } togetherWith
                                slideOutHorizontally(tween(400, easing = FastOutSlowInEasing)) { -it / 2 }
                        }, popTransitionSpec = {
                            slideInHorizontally(tween(400, easing = FastOutSlowInEasing)) { -it / 2 } togetherWith
                                slideOutHorizontally(tween(400, easing = FastOutSlowInEasing)) { it }
                        }, entries = navigation.toEntries(entryProvider {
                            entry<SongPracticeRoute> { route ->
                                val binding = LocalTopBarEntry.current
                                val choosing = route == SongPracticeRoute.Picker
                                val detail = route == SongPracticeRoute.Detail
                                SideEffect { binding.update(TopBarConfig(
                                    title = if (choosing) "添加歌曲" else if (detail) state.entry?.title ?: "歌曲轨迹" else "歌曲轨迹",
                                    showBackButton = route != SongPracticeRoute.Library,
                                    auxiliaryActions = if (route == SongPracticeRoute.Library) listOf(Action.Icon("add", R.drawable.ic_add, iconSize = 18.dp,
                                        contentDescription = "添加解析歌曲", onClick = { if (!analysis.busy) { query = ""; navigator.navigate(SongPracticeRoute.Picker) } })) else emptyList(),
                                    actions = listOf(Action.Icon("close", R.drawable.ic_close, iconSize = 16.dp,
                                        contentDescription = "关闭歌曲轨迹", onClick = ::close))), null) }
                                LaunchedEffect(binding) { binding.scroll.chrome.snapTo(if (detail || choosing) 1f else 0f) }
                                if (detail) {
                                    LaunchedEffect(state.entry) {
                                        if (state.entry == null && navigation.currentRoute == route) navigator.goBack()
                                    }
                                    DisposableEffect(Unit) {
                                        onDispose {
                                            // 出场动画结束后释放；快速再次进入时不能释放新会话。
                                            if (navigation.currentRoute != SongPracticeRoute.Detail) viewModel.closeSong()
                                        }
                                    }
                                }
                                Column(Modifier.fillMaxSize().background(colors.surface).padding(padding)) {
                                    when {

                                        detail && state.loading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = colors.primary) }
                                        detail && state.entry != null -> {
                                            val entry = state.entry!!
                                            PitchCanvas(
                                                state = PitchState(phase = if (state.playing) PitchPhase.Recording else PitchPhase.Paused,
                                                    frames = state.takes.current, reading = state.reading, durationMs = state.positionMs,
                                                    sessionId = entry.addedAt),
                                                enabled = !closing && !configurationVisible,
                                                onPrimary = {}, onStop = {}, onReset = {}, modifier = Modifier.weight(1f).fillMaxWidth(),
                                                reference = ReferencePlayback(state.positionMs, entry.durationMs, state.playing,
                                                    currentPosition = viewModel::displayPosition, frames = state.reference),
                                                olderTakes = if (state.showOlder) state.takes.older else emptyList(),
                                                timelineGestures = PracticeTimelineGestures(viewModel::beginScrub, viewModel::previewSeek, viewModel::finishScrub),
                                                bottomControls = { canvasBackdrop ->
                                                    PracticeControls(state.positionMs, entry.durationMs, state.playing, !closing && !configurationVisible,
                                                        canvasBackdrop, onBegin = viewModel::beginScrub, onPreview = viewModel::previewSeek,
                                                        onFinish = { viewModel.finishScrub() }, onCancel = { viewModel.finishScrub(true) }, onSeek = viewModel::seek,
                                                        onToggle = {
                                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) viewModel.toggle()
                                                            else microphone.launch(Manifest.permission.RECORD_AUDIO)
                                                        })
                                                })
                                        }
                                        choosing -> {
                                            PracticeSongSearch(query, { query = it })
                                            val filtered = remember(songs, query) { songs.filter { it.title.contains(query, true) || it.artist.contains(query, true) } }
                                            LazyColumn(Modifier.weight(1f).fillMaxWidth().imePadding(), contentPadding = PaddingValues(vertical = 12.dp)) {
                                                itemsIndexed(filtered, key = { _, it -> it.id }) { index, song ->
                                                    SongRow(song.title, song.artist, song.coverUri?.toString(), formatPracticeTime(song.duration),
                                                        showDivider = index != filtered.lastIndex, enabled = !checkingModel, onClick = { select(song) })
                                                }
                                                if (filtered.isEmpty()) item { Text("没有找到歌曲", Modifier.padding(20.dp), color = colors.subTitleColor) }
                                            }
                                        }
                                        else -> LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp)) {
                                            val taskSong = analysis.song?.takeIf { analysis.busy || analysis.error != null }
                                            val visibleEntries = entries.filter { it.songId != taskSong?.id }
                                            if (taskSong != null) item(key = "analysis-task") {
                                                val previousEntry = entries.firstOrNull { it.songId == taskSong.id }
                                                SongRow(taskSong.title, taskSong.artist, taskSong.coverUri?.toString(),
                                                    if (analysis.busy) "${(analysis.progress.coerceIn(0f, 1f) * 100).toInt()}%" else "失败",
                                                    status = if (analysis.busy) analysis.stage else analysis.error,
                                                    progress = analysis.progress.takeIf { analysis.busy },
                                                    menu = { listOf(MenuConfig.Item(iconRes = if (analysis.busy) R.drawable.ic_close else R.drawable.ic_pitch_reset,
                                                        iconSize = if (analysis.busy) 14.dp else 18.dp,
                                                        titleRes = if (analysis.busy) R.string.pitch_cancel_analysis else R.string.pitch_retry_analysis,
                                                        onClick = { overlayMenu.dismiss()
                                                            if (analysis.busy) SongMelodyAnalysisService.cancel(context) else select(taskSong)
                                                        })) }, onClick = previousEntry?.let { entry -> ({
                                                        viewModel.open(entry); navigator.navigate(SongPracticeRoute.Detail)
                                                    }) })
                                            }
                                            if (initializing) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 16.dp), color = colors.primary) }
                                            itemsIndexed(visibleEntries, key = { _, it -> it.key }) { index, entry ->
                                                SongRow(entry.title, entry.artist, entry.coverUri, formatPracticeTime(entry.durationMs),
                                                    showDivider = index != visibleEntries.lastIndex, onClick = { viewModel.open(entry); navigator.navigate(SongPracticeRoute.Detail) }, menu = { entryMenu(entry) })
                                            }
                                            if (entries.isEmpty() && !initializing && taskSong == null) item {
                                                Box(Modifier.fillMaxWidth().height(240.dp), contentAlignment = Alignment.Center) { Text("暂无已解析歌曲", color = colors.subTitleColor) }
                                            }
                                        }
                                    }
                                }
                            }
                        }, extraDecorators = { tab -> listOf(rememberTopBarEntryDecorator(tab, topBar, false)) }))
                }
            }
        }
        deleting?.let { entry -> GlassAlertDialog(backdrop = backdrop, onDismissRequest = { deleting = null }, title = { Text("移除解析记录？") },
            text = { Text("只移除「${entry.title}」的解析结果，原歌曲保留。") },
            confirmButton = { GlassDialogButton(onClick = { deleting = null; viewModel.remove(entry) }) { Text("移除") } },
            dismissButton = { GlassDialogButton(primary = false, onClick = { deleting = null }) { Text("取消") } }) }
        confirm?.let { song -> GlassAlertDialog(backdrop = backdrop, onDismissRequest = { confirm = null }, title = { Text("解析整首歌曲") },
            text = { Text("${song.title} · ${song.durationText}\n\n" +
                (viewModel.analysis.estimateSeconds(song.duration)?.let { "预计约 ${maxOf(1, it / 2)}–${it * 2} 秒，实际可能不同。" }
                    ?: "首次分析暂无可靠耗时估计，开始后显示进度。") +
                "\n\n本机处理，不上传歌曲。会占用存储、内存和电量，可能发热；支持后台处理和取消，已有结果会复用。完成后保留在已解析列表。") },
            confirmButton = { GlassDialogButton(onClick = {
                confirm = null; pendingNotification = song
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                    SongMelodyAnalysisService.start(context, viewModel.analysis, song); pendingNotification = null
                } else notification.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text("开始解析") } }, dismissButton = { GlassDialogButton(primary = false, onClick = { confirm = null }) { Text("取消") } }) }
        state.message?.let { message -> GlassAlertDialog(backdrop = backdrop, onDismissRequest = { viewModel.message(null) },
            text = { Text(message) }, confirmButton = { GlassDialogButton(onClick = { viewModel.message(null) }) { Text("知道了") } }) }
    }
}

@Composable
private fun SongRow(title: String, artist: String, cover: String?, duration: String,
    showDivider: Boolean = true, enabled: Boolean = true,
    status: String? = null, progress: Float? = null,
    menu: (() -> List<MenuConfig>)? = null, onClick: (() -> Unit)?) {
    val colors = MaterialTheme.rythmeColors
    val overlay = LocalOverlayMenu.current
    val backdrop = LocalBackdrop.current
    var bounds by remember { mutableStateOf(Rect.Zero) }
    fun showMenu() { menu?.let { overlay.show(OverlayMenu.ActionMenu(
        sourceKey = "practice-$title-$cover", anchorBounds = bounds, configs = it(), backdrop = backdrop)) } }
    Column {
        SongListRow(title = title, subtitle = status ?: artist, showDivider = showDivider && progress == null,
            enabled = enabled, onClick = { if (onClick != null) onClick() else showMenu() },
            onLongClick = if (menu != null) ({ showMenu() }) else null,
            cover = {
                Box(Modifier.size(48.dp).clip(ContinuousRoundedRectangle(6.dp)).background(colors.coverBg),
                    contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_music), null, Modifier.size(24.dp), tint = colors.coverIcon)
                    if (!cover.isNullOrBlank()) AsyncImage(cover, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
            }, trailing = {
                Text(duration, Modifier.padding(start = 12.dp, end = 4.dp),
                    color = if (progress != null) colors.primary else colors.subTitleColor, fontSize = 12.sp)
                if (menu != null) IconButton(onClick = ::showMenu,
                    modifier = Modifier.size(40.dp).onGloballyPositioned { bounds = it.boundsInWindow() }) {
                    Icon(painterResource(R.drawable.ic_more), "${title}的更多操作", Modifier.size(18.dp), tint = colors.textColor)
                } else Icon(painterResource(R.drawable.ic_forward), null, Modifier.padding(start = 6.dp).size(12.dp), tint = colors.weakColor)
            })
        if (progress != null) {
            val displayedProgress by animateFloatAsState(progress.coerceIn(0f, 1f),
                animationSpec = tween(200), label = "analysis-progress")
            Box(Modifier.fillMaxWidth().padding(start = 81.dp, end = 21.dp, bottom = 8.dp)
                .height(3.dp)
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress.coerceIn(0f, 1f), 0f..1f) }
                .drawBehind {
                    val radius = CornerRadius(size.height / 2f)
                    drawRoundRect(colors.textColor.copy(alpha = .10f), cornerRadius = radius)
                    if (displayedProgress > 0f) drawRoundRect(colors.primary,
                        size = Size(size.width * displayedProgress, size.height), cornerRadius = radius)
                })
        }
    }
}

@Composable
private fun PracticeSongSearch(value: String, onValueChange: (String) -> Unit) {
    val colors = MaterialTheme.rythmeColors
    val focus = LocalFocusManager.current
    BasicTextField(value, onValueChange, singleLine = true,
        textStyle = TextStyle(color = colors.textColor, fontSize = 16.sp),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focus.clearFocus() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 21.dp, vertical = 6.dp)
            .height(HeaderSearchLayout.surfaceHeight).clip(ContinuousCapsule).background(colors.searchBg)
            .semantics { contentDescription = "搜索本地歌曲" },
        decorationBox = { field ->
            Row(Modifier.fillMaxSize().padding(start = 14.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_search), null, Modifier.size(18.dp), tint = colors.subTitleColor)
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text("搜索歌曲或歌手", color = colors.subTitleColor, fontSize = 16.sp)
                    field()
                }
                if (value.isNotEmpty()) IconButton(onClick = { onValueChange("") }, Modifier.size(40.dp)) {
                    Icon(painterResource(R.drawable.ic_close), "清空搜索", Modifier.size(14.dp), tint = colors.subTitleColor)
                } else Spacer(Modifier.width(10.dp))
            }
        })
}

@Composable
private fun PracticeControls(position: Long, duration: Long, playing: Boolean, enabled: Boolean, backdrop: Backdrop,
    onBegin: () -> Unit, onPreview: (Long) -> Unit, onFinish: () -> Unit, onCancel: () -> Unit, onSeek: (Long) -> Unit,
    onToggle: () -> Unit) {
    val colors = MaterialTheme.rythmeColors
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        PitchGlassSurface(backdrop, ContinuousRoundedRectangle(26.dp))
        Column(Modifier.fillMaxWidth().padding(top = 13.dp, bottom = 9.dp)) {
            PlayerSliderTrack(progress = (position.toFloat() / duration.coerceAtLeast(1)).coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "歌曲进度" }, enabled = enabled,
                horizontalPadding = 20.dp, pressedPadding = 16.dp, trackColor = colors.textColor,
                onProgressChange = { onPreview((it * duration).toLong()) },
                onProgressChangeFinished = { onPreview((it * duration).toLong()); onFinish() },
                onDragStateChange = { if (it) onBegin() else onCancel() })
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatPracticeTime(position), fontSize = 11.sp, color = colors.subTitleColor)
                Text("−${formatPracticeTime((duration - position).coerceAtLeast(0))}", fontSize = 11.sp, color = colors.subTitleColor)
            }
            Row(Modifier.fillMaxWidth().height(62.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                PlayerPressFeedback(size = 48.dp, haloColor = colors.textColor) { interactions ->
                    PreviousIcon(enable = enabled, height = 15.dp, tint = colors.textColor,
                        interactionSource = interactions,
                        modifier = Modifier.size(48.dp).semantics { contentDescription = "回到歌曲开头" },
                        onClick = { onSeek(0) })
                }
                SkipFive(false, enabled) { onSeek(position - 5000) }
                PlayerPressFeedback(size = 60.dp, haloColor = colors.textColor) { interactions ->
                    PlayPauseIcon(isPlaying = playing, size = 27.dp, tint = colors.textColor,
                        interactionSource = interactions, enabled = enabled, animationSpec = tween(180),
                        modifier = Modifier.size(60.dp).semantics { contentDescription = if (playing) "暂停跟唱" else "播放并跟唱" },
                        onClick = onToggle)
                }
                SkipFive(true, enabled) { onSeek(position + 5000) }
                // 暂留入口位置，待新的菜单交互确定后接入。
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_more), null, Modifier.size(22.dp), tint = colors.textColor)
                }
            }
        }
    }
}
@Composable private fun SkipFive(forward: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.rythmeColors
    var turns by remember { mutableIntStateOf(0) }
    val angle by animateFloatAsState(turns * if (forward) 360f else -360f,
        animationSpec = tween(450), label = "skip-five-ring")
    // SVG 的环心不是画布中心；数字另绘一层，不参与旋转。
    PlayerPressFeedback(size = 48.dp, haloColor = colors.textColor) { interactions ->
        Box(Modifier.size(48.dp).clickable(enabled = enabled, interactionSource = interactions,
            indication = null, role = Role.Button, onClick = { onClick(); turns++ })
            .semantics { contentDescription = if (forward) "前进五秒" else "后退五秒" },
            contentAlignment = Alignment.Center) {
            Icon(painterResource(if (forward) R.drawable.ic_pitch_forward_five_ring else R.drawable.ic_pitch_back_five_ring),
                null, Modifier.size(25.dp).graphicsLayer {
                    rotationZ = angle
                    transformOrigin = TransformOrigin(24.4097f / 48f, 25.9924f / 48f)
                }, tint = colors.textColor)
            Icon(painterResource(R.drawable.ic_pitch_five_number), null, Modifier.size(25.dp), tint = colors.textColor)
        }
    }
}
private fun formatPracticeTime(ms: Long) = "%02d:%02d".format(java.util.Locale.ROOT, ms / 60_000, ms / 1000 % 60)
