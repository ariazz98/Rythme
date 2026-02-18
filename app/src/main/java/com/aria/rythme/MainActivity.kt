package com.aria.rythme

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.rythme.feature.player.presentation.PlayerScreen
import com.aria.rythme.feature.player.presentation.PlaylistScreen
import com.aria.rythme.feature.player.presentation.components.MiniPlayer
import com.aria.rythme.feature.player.presentation.components.MiniPlayerScaffold
import com.aria.rythme.ui.theme.RythmeTheme
import org.koin.androidx.compose.koinViewModel

/**
 * 主 Activity
 *
 * Rythme 应用的入口 Activity，使用 Jetpack Compose 构建 UI。
 * 项目采用 MVI（Model-View-Intent）架构模式。
 *
 * ## MVI 架构说明
 *
 * MVI 是一种单向数据流架构模式，强调状态的不可变性和可预测性。
 *
 * ### 核心组件
 * 1. **Intent（用户意图）**: 用户的所有交互行为
 *    - 位置：`core/mvi/UserIntent.kt`
 *    - 示例：OnButtonClick, OnTextChanged
 *
 * 2. **State（UI 状态）**: 界面的完整状态快照
 *    - 位置：`core/mvi/UiState.kt`
 *    - 示例：data class MyState(val isLoading: Boolean, val data: List<Item>)
 *
 * 3. **Action（内部动作）**: ViewModel 内部处理的中间动作
 *    - 位置：`core/mvi/InternalAction.kt`
 *    - 示例：LoadSuccess, LoadFailure
 *
 * 4. **Effect（副作用）**: 一次性事件
 *    - 位置：`core/mvi/SideEffect.kt`
 *    - 示例：ShowToast, NavigateToHome
 *
 * 5. **BaseViewModel**: ViewModel 基类
 *    - 位置：`core/mvi/BaseViewModel.kt`
 *    - 提供完整的单向数据流处理能力
 *
 * ### 数据流向
 * ```
 * User Action → Intent → ViewModel → Action → Reducer → New State → UI Update
 *                                        ↓
 *                                    Effect → UI Side Effect Handler
 * ```
 *
 * ### 快速开始：创建新功能模块
 *
 * #### 1. 在 feature 目录下创建功能模块
 * ```
 * feature/
 * └── myfeature/
 *     ├── presentation/
 *     │   ├── MyFeatureContract.kt     // 定义 Intent/State/Action/Effect
 *     │   ├── MyFeatureViewModel.kt    // 实现 ViewModel
 *     │   └── MyFeatureScreen.kt       // Compose UI
 *     ├── domain/                      // 业务逻辑层（可选）
 *     └── data/                        // 数据层（可选）
 * ```
 *
 * #### 2. 定义契约（Contract.kt）
 * ```kotlin
 * // 用户意图
 * sealed interface MyFeatureIntent : UserIntent {
 *     data object OnButtonClick : MyFeatureIntent
 *     data class OnTextChanged(val text: String) : MyFeatureIntent
 * }
 *
 * // UI 状态
 * data class MyFeatureState(
 *     val text: String = "",
 *     val isLoading: Boolean = false
 * ) : UiState
 *
 * // 内部动作
 * sealed interface MyFeatureAction : InternalAction {
 *     data object StartLoading : MyFeatureAction
 *     data class UpdateText(val text: String) : MyFeatureAction
 * }
 *
 * // 副作用
 * sealed interface MyFeatureEffect : SideEffect {
 *     data class ShowToast(val message: String) : MyFeatureEffect
 * }
 * ```
 *
 * #### 3. 实现 ViewModel
 * ```kotlin
 * class MyFeatureViewModel : BaseViewModel<
 *     MyFeatureIntent,
 *     MyFeatureState,
 *     MyFeatureAction,
 *     MyFeatureEffect
 * >() {
 *
 *     override fun createInitialState() = MyFeatureState()
 *
 *     override fun handleIntent(intent: MyFeatureIntent) {
 *         when (intent) {
 *             is MyFeatureIntent.OnButtonClick -> {
 *                 viewModelScope.launch {
 *                     reduceAndUpdate(MyFeatureAction.StartLoading)
 *                     // 执行业务逻辑
 *                     sendEffect(MyFeatureEffect.ShowToast("Success"))
 *                 }
 *             }
 *             is MyFeatureIntent.OnTextChanged -> {
 *                 reduceAndUpdate(MyFeatureAction.UpdateText(intent.text))
 *             }
 *         }
 *     }
 *
 *     override fun reduce(action: MyFeatureAction): MyFeatureState {
 *         return when (action) {
 *             is MyFeatureAction.StartLoading -> {
 *                 state.value.copy(isLoading = true)
 *             }
 *             is MyFeatureAction.UpdateText -> {
 *                 state.value.copy(text = action.text)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * #### 4. 创建 Compose Screen
 * ```kotlin
 * @Composable
 * fun MyFeatureScreen(viewModel: MyFeatureViewModel = koinViewModel()) {
 *     // 订阅状态
 *     val state by viewModel.state.collectAsState()
 *
 *     // 处理副作用
 *     val context = LocalContext.current
 *     LaunchedEffect(Unit) {
 *         viewModel.effect.collect { effect ->
 *             when (effect) {
 *                 is MyFeatureEffect.ShowToast -> {
 *                     Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
 *                 }
 *             }
 *         }
 *     }
 *
 *     // UI 内容
 *     Column {
 *         TextField(
 *             value = state.text,
 *             onValueChange = { viewModel.sendIntent(MyFeatureIntent.OnTextChanged(it)) }
 *         )
 *         Button(
 *             onClick = { viewModel.sendIntent(MyFeatureIntent.OnButtonClick) },
 *             enabled = !state.isLoading
 *         ) {
 *             Text(if (state.isLoading) "Loading..." else "Submit")
 *         }
 *     }
 * }
 * ```
 *
 * #### 5. 配置 Koin 依赖注入
 * ```kotlin
 * val myFeatureModule = module {
 *     viewModel { MyFeatureViewModel() }
 * }
 * ```
 *
 * ### 工具类和扩展
 *
 * #### Compose 扩展函数
 * - 位置：`core/extensions/MviComposeExt.kt`
 * - `collectAsUiState()`: 收集状态
 * - `collectAsEffect()`: 收集副作用
 * - `collectUi()`: 同时收集状态和副作用
 *
 * #### 日志工具
 * - 位置：`core/utils/RythmeLogger.kt`
 * - 自动记录 Intent、Action、State、Effect
 * - 支持性能分析和错误追踪
 *
 * #### 协程调度器
 * - 位置：`core/utils/DispatchersProvider.kt`
 * - 统一管理协程调度器
 * - 便于管理和替换调度器
 *
 * ### 最佳实践
 *
 * 1. **单向数据流**: 始终通过 Intent → Action → State 流程更新状态
 * 2. **状态不可变**: 使用 data class 和 copy() 创建新状态
 * 3. **纯函数 Reducer**: Reducer 不应该包含副作用
 * 4. **副作用隔离**: 使用 Effect 处理一次性事件
 * 5. **依赖注入**: 使用 Koin 注入依赖，便于测试
 * 6. **日志记录**: 在关键节点使用 RythmeLogger 记录日志
 * 7. **单元测试**: 为 ViewModel 和 Reducer 编写单元测试（可选）
 *
 * ### 参考资源
 *
 * - MVI 核心接口：`core/mvi/`
 * - BaseViewModel：`core/mvi/BaseViewModel.kt`
 * - Compose 扩展：`core/extensions/MviComposeExt.kt`
 * - 示例目录结构：`feature/.gitkeep`
 */
class MainActivity : ComponentActivity() {

    /**
     * 权限请求启动器
     */
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，刷新歌曲列表
            Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            // 权限被拒绝
            Toast.makeText(this, "需要音频权限才能播放音乐", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 检查并请求权限
        checkAndRequestPermission()

        setContent {
            RythmeTheme {
                MainScreen()
            }
        }
    }

    /**
     * 检查并请求音频权限
     */
    private fun checkAndRequestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) ==
                    PackageManager.PERMISSION_GRANTED -> {
                // 权限已授予
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // 显示权限说明
                Toast.makeText(this, "需要音频权限来扫描本地音乐", Toast.LENGTH_LONG).show()
                permissionLauncher.launch(permission)
            }
            else -> {
                // 直接请求权限
                permissionLauncher.launch(permission)
            }
        }
    }
}

/**
 * 主屏幕
 *
 * 包含播放列表和迷你播放器
 */
@Composable
fun MainScreen() {
    var showPlayer by remember { mutableStateOf(false) }

    if (showPlayer) {
        // 显示完整播放器
        PlayerScreenWithBack { showPlayer = false }
    } else {
        // 显示播放列表和迷你播放器
        PlaylistWithMiniPlayer { showPlayer = true }
    }
}

/**
 * 播放列表与迷你播放器组合
 */
@Composable
fun PlaylistWithMiniPlayer(
    onExpandPlayer: () -> Unit
) {
    MiniPlayerScaffold(
        modifier = Modifier.fillMaxSize(),
        content = {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = {
                    // 可以添加顶部标题栏
                }
            ) { innerPadding ->
                PlaylistScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }
        },
        miniPlayer = {
            val viewModel: com.aria.rythme.feature.player.presentation.PlayerViewModel = koinViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
        
            MiniPlayer(
                song = state.currentSong,
                isPlaying = state.isPlaying,
                progress = state.progress,
                onClick = onExpandPlayer,
                onPlayPauseClick = {
                    viewModel.sendIntent(
                        com.aria.rythme.feature.player.presentation.PlayerIntent.TogglePlayPause
                    )
                },
                onNextClick = {
                    viewModel.sendIntent(
                        com.aria.rythme.feature.player.presentation.PlayerIntent.Next
                    )
                }
            )
        }
    )
}

/**
 * 播放器页面（带返回）
 */
@Composable
fun PlayerScreenWithBack(
    onBack: () -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        PlayerScreen(
            modifier = Modifier.padding(innerPadding),
            onBack = onBack
        )
    }
}