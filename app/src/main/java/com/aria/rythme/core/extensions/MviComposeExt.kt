package com.aria.rythme.core.extensions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.aria.rythme.core.mvi.BaseViewModel
import com.aria.rythme.core.mvi.MviEffect
import com.aria.rythme.core.mvi.MviIntent
import com.aria.rythme.core.mvi.MviState
import com.aria.rythme.core.mvi.MviAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * MVI Compose 扩展函数集
 *
 * 提供一组便捷的扩展函数，简化 MVI 架构在 Jetpack Compose 中的使用。
 * 这些扩展函数封装了常见的状态订阅和副作用处理模式，提升开发效率。
 */

/**
 * 在 Compose 中收集 StateFlow 并转换为 State
 *
 * 这是 collectAsState() 的类型安全版本，专门用于 MVI 的 State。
 * 当 StateFlow 发出新值时，会触发 Compose 重组。
 *
 * ## 特点
 * - 自动管理生命周期，在 Composable 离开组合树时自动取消订阅
 * - 线程安全，StateFlow 的更新会自动调度到主线程
 * - 初始值为 StateFlow 的当前值
 *
 * ## 使用场景
 * 在 Composable 函数中订阅 ViewModel 的状态
 *
 * @receiver StateFlow<S> MVI State 的 StateFlow
 * @return State<S> Compose 的 State 对象，可直接在 UI 中使用
 *
 * 使用示例：
 * ```kotlin
 * @Composable
 * fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
 *     // 订阅状态
 *     val state = viewModel.state.collectAsMviState()
 *
 *     // 使用状态渲染 UI
 *     TextField(
 *         value = state.username,
 *         onValueChange = { viewModel.sendIntent(LoginIntent.OnUsernameChanged(it)) }
 *     )
 *
 *     if (state.isLoading) {
 *         CircularProgressIndicator()
 *     }
 * }
 * ```
 *
 * @see collectAsState Compose 原生的 collectAsState 函数
 */
@Composable
fun <S : MviState> StateFlow<S>.collectAsMviState(): State<S> {
    return collectAsState()
}

/**
 * 在 Compose 中收集并处理 MVI Effect
 *
 * 用于处理一次性的副作用事件（如导航、Toast、Dialog）。
 * 这个扩展函数确保 Effect 只在合适的生命周期状态下被处理，避免在后台状态下执行副作用。
 *
 * ## 特点
 * - **生命周期感知**：只在 Lifecycle.State.STARTED 或更高状态时收集 Effect
 * - **一次性消费**：每个 Effect 只会被处理一次
 * - **自动清理**：当 Composable 离开组合树时自动取消订阅
 * - **防止重复**：通过 LaunchedEffect(Unit) 确保只创建一次收集器
 *
 * ## 为什么需要生命周期感知？
 * - 防止在后台状态下显示 Toast 或 Dialog
 * - 避免在配置更改时重复执行副作用
 * - 确保导航操作在正确的时机执行
 *
 * ## 使用场景
 * 处理所有一次性事件，如：
 * - 导航跳转
 * - 显示 Toast/Snackbar
 * - 显示 Dialog/BottomSheet
 * - 请求权限
 * - 调用系统功能
 *
 * @receiver Flow<E> MVI Effect 的 Flow
 * @param lifecycleState 收集 Effect 的最低生命周期状态，默认为 STARTED
 * @param onEffect Effect 处理回调函数
 *
 * 使用示例：
 * ```kotlin
 * @Composable
 * fun LoginScreen(
 *     viewModel: LoginViewModel = koinViewModel(),
 *     navController: NavController
 * ) {
 *     val context = LocalContext.current
 *
 *     // 处理副作用
 *     viewModel.effect.collectAsMviEffect { effect ->
 *         when (effect) {
 *             is LoginEffect.ShowToast -> {
 *                 Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
 *             }
 *             is LoginEffect.NavigateToHome -> {
 *                 navController.navigate("home") {
 *                     popUpTo("login") { inclusive = true }
 *                 }
 *             }
 *             is LoginEffect.ShowError -> {
 *                 // 显示错误对话框
 *                 AlertDialog.Builder(context)
 *                     .setMessage(effect.error.message)
 *                     .show()
 *             }
 *             is LoginEffect.RequestBiometric -> {
 *                 // 请求生物识别
 *                 biometricPrompt.authenticate(promptInfo)
 *             }
 *         }
 *     }
 *
 *     // UI 内容
 *     // ...
 * }
 * ```
 *
 * ## 高级用法：指定生命周期状态
 * ```kotlin
 * // 只在 RESUMED 状态收集（适用于需要前台才能执行的操作）
 * viewModel.effect.collectAsMviEffect(
 *     lifecycleState = Lifecycle.State.RESUMED
 * ) { effect ->
 *     // 处理 effect
 * }
 * ```
 *
 * @see LaunchedEffect Compose 的副作用 API
 * @see repeatOnLifecycle 生命周期感知的 Flow 收集
 */
@Composable
fun <E : MviEffect> Flow<E>.collectAsMviEffect(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    onEffect: (E) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            collect { effect ->
                onEffect(effect)
            }
        }
    }
}

/**
 * 便捷函数：从 BaseViewModel 收集 State
 *
 * 直接从 ViewModel 收集状态，简化代码书写。
 *
 * @receiver BaseViewModel MVI ViewModel
 * @return State<S> Compose 的 State 对象
 *
 * 使用示例：
 * ```kotlin
 * @Composable
 * fun MyScreen(viewModel: MyViewModel = koinViewModel()) {
 *     val state = viewModel.collectState()
 *     // 使用 state
 * }
 * ```
 */
@Composable
fun <I : MviIntent, S : MviState, A : MviAction, E : MviEffect> BaseViewModel<I, S, A, E>.collectState(): State<S> {
    return state.collectAsMviState()
}

/**
 * 便捷函数：从 BaseViewModel 收集 Effect
 *
 * 直接从 ViewModel 收集副作用，简化代码书写。
 *
 * @receiver BaseViewModel MVI ViewModel
 * @param lifecycleState 收集 Effect 的最低生命周期状态
 * @param onEffect Effect 处理回调函数
 *
 * 使用示例：
 * ```kotlin
 * @Composable
 * fun MyScreen(viewModel: MyViewModel = koinViewModel()) {
 *     viewModel.collectEffect { effect ->
 *         when (effect) {
 *             // 处理 effect
 *         }
 *     }
 * }
 * ```
 */
@Composable
fun <I : MviIntent, S : MviState, A : MviAction, E : MviEffect> BaseViewModel<I, S, A, E>.collectEffect(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    onEffect: (E) -> Unit
) {
    effect.collectAsMviEffect(lifecycleState, onEffect)
}

/**
 * 完整的 MVI 组件收集函数
 *
 * 同时收集 State 和 Effect，提供一站式的 MVI 订阅方案。
 * 适用于需要同时处理状态和副作用的场景。
 *
 * @receiver BaseViewModel MVI ViewModel
 * @param lifecycleState 收集 Effect 的最低生命周期状态
 * @param onEffect Effect 处理回调函数
 * @return State<S> Compose 的 State 对象
 *
 * 使用示例：
 * ```kotlin
 * @Composable
 * fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
 *     val state = viewModel.collectMvi { effect ->
 *         when (effect) {
 *             is LoginEffect.ShowToast -> {
 *                 Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
 *             }
 *             is LoginEffect.NavigateToHome -> {
 *                 navController.navigate("home")
 *             }
 *         }
 *     }
 *
 *     // 使用 state 渲染 UI
 *     LoginContent(
 *         state = state,
 *         onIntent = viewModel::sendIntent
 *     )
 * }
 * ```
 */
@Composable
fun <I : MviIntent, S : MviState, A : MviAction, E : MviEffect> BaseViewModel<I, S, A, E>.collectMvi(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    onEffect: (E) -> Unit
): State<S> {
    // 收集副作用
    collectEffect(lifecycleState, onEffect)
    // 返回状态
    return collectState()
}
