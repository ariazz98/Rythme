package com.aria.rythme.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * MVI 架构 - 基础 ViewModel 抽象类
 *
 * BaseViewModel 是所有 MVI 架构 ViewModel 的基类，提供了完整的 MVI 数据流处理能力。
 * 它实现了 Intent → Action → State → Effect 的单向数据流，并提供了线程安全的状态管理。
 *
 * ## 核心职责
 * 1. **接收 Intent**：通过 [sendIntent] 方法接收来自 UI 的用户意图
 * 2. **处理 Intent**：将 Intent 转换为内部 Action 并执行业务逻辑
 * 3. **更新 State**：通过 Reducer 将 Action 转换为新的 State
 * 4. **发送 Effect**：处理一次性的副作用事件
 * 5. **状态暴露**：向 UI 层暴露可观察的 State 和 Effect
 *
 * ## 泛型参数
 * @param I [MviIntent] 该功能模块的用户意图类型
 * @param S [MviState] 该功能模块的 UI 状态类型
 * @param A [MviAction] 该功能模块的内部动作类型
 * @param E [MviEffect] 该功能模块的副作用事件类型
 *
 * ## 使用示例
 * ```kotlin
 * // 1. 定义契约（Contract）
 * sealed interface LoginIntent : MviIntent {
 *     data class OnUsernameChanged(val username: String) : LoginIntent
 *     data object OnLoginClick : LoginIntent
 * }
 *
 * data class LoginState(
 *     val username: String = "",
 *     val isLoading: Boolean = false
 * ) : MviState
 *
 * sealed interface LoginAction : MviAction {
 *     data object StartLoading : LoginAction
 *     data class LoginSuccess(val user: User) : LoginAction
 * }
 *
 * sealed interface LoginEffect : MviEffect {
 *     data class ShowToast(val message: String) : LoginEffect
 *     data object NavigateToHome : LoginEffect
 * }
 *
 * // 2. 实现 ViewModel
 * class LoginViewModel(
 *     private val loginUseCase: LoginUseCase
 * ) : BaseViewModel<LoginIntent, LoginState, LoginAction, LoginEffect>() {
 *
 *     override fun createInitialState(): LoginState {
 *         return LoginState()
 *     }
 *
 *     override fun handleIntent(intent: LoginIntent) {
 *         when (intent) {
 *             is LoginIntent.OnUsernameChanged -> {
 *                 reduce(LoginAction.UpdateUsername(intent.username))
 *             }
 *             is LoginIntent.OnLoginClick -> {
 *                 performLogin()
 *             }
 *         }
 *     }
 *
 *     private fun performLogin() {
 *         viewModelScope.launch {
 *             reduce(LoginAction.StartLoading)
 *             try {
 *                 val user = loginUseCase(state.value.username)
 *                 reduce(LoginAction.LoginSuccess(user))
 *                 sendEffect(LoginEffect.NavigateToHome)
 *             } catch (e: Exception) {
 *                 sendEffect(LoginEffect.ShowToast("登录失败"))
 *             }
 *         }
 *     }
 *
 *     override fun reduce(action: LoginAction): LoginState {
 *         return when (action) {
 *             is LoginAction.StartLoading -> {
 *                 state.value.copy(isLoading = true)
 *             }
 *             is LoginAction.LoginSuccess -> {
 *                 state.value.copy(isLoading = false)
 *             }
 *         }
 *     }
 * }
 *
 * // 3. 在 Compose 中使用
 * @Composable
 * fun LoginScreen(viewModel: LoginViewModel = koinViewModel()) {
 *     val state by viewModel.state.collectAsState()
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.effect.collect { effect ->
 *             when (effect) {
 *                 is LoginEffect.ShowToast -> {
 *                     // 显示 Toast
 *                 }
 *                 is LoginEffect.NavigateToHome -> {
 *                     // 导航到首页
 *                 }
 *             }
 *         }
 *     }
 *
 *     TextField(
 *         value = state.username,
 *         onValueChange = { viewModel.sendIntent(LoginIntent.OnUsernameChanged(it)) }
 *     )
 *
 *     Button(onClick = { viewModel.sendIntent(LoginIntent.OnLoginClick) }) {
 *         Text("登录")
 *     }
 * }
 * ```
 *
 * ## 数据流向图
 * ```
 *     ┌─────────────┐
 *     │     UI      │
 *     └─────────────┘
 *           │ sendIntent()
 *           ▼
 *     ┌─────────────┐
 *     │   Intent    │
 *     └─────────────┘
 *           │ handleIntent()
 *           ▼
 *     ┌─────────────┐
 *     │   Action    │────────┐
 *     └─────────────┘        │
 *           │ reduce()       │ sendEffect()
 *           ▼                ▼
 *     ┌─────────────┐  ┌─────────────┐
 *     │    State    │  │   Effect    │
 *     └─────────────┘  └─────────────┘
 *           │                │
 *           └────────┬───────┘
 *                    ▼
 *              ┌─────────────┐
 *              │  UI Update  │
 *              └─────────────┘
 * ```
 *
 * ## 线程安全
 * - State 使用 [MutableStateFlow]，确保状态更新的线程安全
 * - Effect 使用 [Channel]，确保每个副作用只被消费一次
 * - 所有协程操作都在 [viewModelScope] 中执行，生命周期安全
 *
 * ## 最佳实践
 * 1. **不要在 UI 层直接修改 State**：所有状态变化都应该通过 Intent → Action → State 流程
 * 2. **Reducer 应该是纯函数**：不应该包含副作用，只负责根据 Action 计算新 State
 * 3. **Effect 用于副作用**：导航、Toast 等一次性事件应该使用 Effect 而非 State
 * 4. **业务逻辑在 handleIntent 中**：复杂的业务逻辑应该提取到 UseCase 中
 * 5. **初始状态合理**：[createInitialState] 应该返回合理的默认状态
 *
 * @see MviIntent 用户意图接口
 * @see MviState UI 状态接口
 * @see MviAction 内部动作接口
 * @see MviEffect 副作用事件接口
 */
abstract class BaseViewModel<I : MviIntent, S : MviState, A : MviAction, E : MviEffect> : ViewModel() {

    /**
     * 内部可变的状态流
     * 使用 MutableStateFlow 保证状态的线程安全性和可观察性
     */
    private val _state: MutableStateFlow<S> by lazy {
        MutableStateFlow(createInitialState())
    }

    /**
     * 对外暴露的只读状态流
     * UI 层通过订阅此 Flow 来获取最新的状态并触发重组
     *
     * 使用示例：
     * ```kotlin
     * val state by viewModel.state.collectAsState()
     * ```
     */
    val state: StateFlow<S> = _state.asStateFlow()

    /**
     * 内部的副作用通道
     * 使用 Channel 而非 StateFlow，确保每个 Effect 只被消费一次
     * Channel.BUFFERED 允许缓冲多个事件，避免事件丢失
     */
    private val _effect: Channel<E> = Channel(Channel.BUFFERED)

    /**
     * 对外暴露的副作用流
     * UI 层通过收集此 Flow 来处理一次性事件（如导航、Toast）
     *
     * 使用示例：
     * ```kotlin
     * LaunchedEffect(Unit) {
     *     viewModel.effect.collect { effect ->
     *         // 处理副作用
     *     }
     * }
     * ```
     */
    val effect = _effect.receiveAsFlow()

    /**
     * 创建初始状态
     *
     * 子类必须实现此方法，返回该功能模块的初始 UI 状态。
     * 这个方法只会在 ViewModel 创建时调用一次。
     *
     * ## 实现要点
     * - 返回合理的默认值，确保 UI 能够正常渲染
     * - 不应该包含复杂的业务逻辑
     * - 初始状态应该是不可变的
     *
     * @return 初始的 UI 状态
     *
     * 示例：
     * ```kotlin
     * override fun createInitialState(): LoginState {
     *     return LoginState(
     *         username = "",
     *         password = "",
     *         isLoading = false,
     *         isLoginEnabled = false
     *     )
     * }
     * ```
     */
    protected abstract fun createInitialState(): S

    /**
     * 处理用户意图
     *
     * 这是 MVI 数据流的入口，所有来自 UI 的用户交互都会转化为 Intent 并通过此方法处理。
     * 子类需要实现此方法，将 Intent 转换为具体的业务逻辑和 Action。
     *
     * ## 实现要点
     * - 使用 when 表达式匹配不同的 Intent
     * - 可以在此方法中调用业务逻辑（UseCase）
     * - 通过 [reduce] 方法更新状态
     * - 通过 [sendEffect] 方法发送副作用
     * - 异步操作应该在协程中执行（使用 viewModelScope.launch）
     *
     * @param intent 用户发起的意图
     *
     * 示例：
     * ```kotlin
     * override fun handleIntent(intent: LoginIntent) {
     *     when (intent) {
     *         is LoginIntent.OnUsernameChanged -> {
     *             // 同步更新状态
     *             reduce(LoginAction.UpdateUsername(intent.username))
     *         }
     *         is LoginIntent.OnLoginClick -> {
     *             // 异步执行业务逻辑
     *             viewModelScope.launch {
     *                 reduce(LoginAction.StartLoading)
     *                 try {
     *                     val user = loginUseCase(username, password)
     *                     reduce(LoginAction.LoginSuccess(user))
     *                     sendEffect(LoginEffect.NavigateToHome)
     *                 } catch (e: Exception) {
     *                     reduce(LoginAction.LoginFailure(e))
     *                     sendEffect(LoginEffect.ShowError(e))
     *                 }
     *             }
     *         }
     *     }
     * }
     * ```
     */
    protected abstract fun handleIntent(intent: I)

    /**
     * 状态归约器
     *
     * 这是一个纯函数，负责根据 Action 计算新的 State。
     * Reducer 不应该包含任何副作用，只负责状态转换。
     *
     * ## 实现要点
     * - 必须是纯函数（相同输入产生相同输出）
     * - 不应该修改原 State，使用 copy() 创建新 State
     * - 不应该包含异步操作或副作用
     * - 不应该访问外部状态或依赖
     * - 使用 when 表达式匹配不同的 Action
     *
     * ## 为什么要分离 Reducer？
     * - **可测试性**：纯函数易于单元测试
     * - **可预测性**：相同的 Action 总是产生相同的 State
     * - **可维护性**：状态变化逻辑集中在一处
     * - **时间旅行调试**：可以记录和回放状态变化
     *
     * @param action 内部动作
     * @return 新的 UI 状态
     *
     * 示例：
     * ```kotlin
     * override fun reduce(action: LoginAction): LoginState {
     *     return when (action) {
     *         is LoginAction.UpdateUsername -> {
     *             state.value.copy(
     *                 username = action.username,
     *                 isLoginEnabled = action.username.isNotEmpty()
     *             )
     *         }
     *         is LoginAction.StartLoading -> {
     *             state.value.copy(isLoading = true, errorMessage = null)
     *         }
     *         is LoginAction.LoginSuccess -> {
     *             state.value.copy(isLoading = false, user = action.user)
     *         }
     *         is LoginAction.LoginFailure -> {
     *             state.value.copy(
     *                 isLoading = false,
     *                 errorMessage = action.error.message
     *             )
     *         }
     *     }
     * }
     * ```
     */
    protected abstract fun reduce(action: A): S

    /**
     * 发送用户意图
     *
     * UI 层调用此方法来发送用户的交互意图。
     * 这是 UI 与 ViewModel 交互的唯一入口。
     *
     * ## 线程安全
     * 此方法在 viewModelScope 中执行，确保线程安全和生命周期安全。
     *
     * @param intent 用户发起的意图
     *
     * 使用示例：
     * ```kotlin
     * // 在 Compose 中
     * Button(onClick = { viewModel.sendIntent(LoginIntent.OnLoginClick) }) {
     *     Text("登录")
     * }
     *
     * TextField(
     *     value = username,
     *     onValueChange = { viewModel.sendIntent(LoginIntent.OnUsernameChanged(it)) }
     * )
     * ```
     */
    fun sendIntent(intent: I) {
        viewModelScope.launch {
            handleIntent(intent)
        }
    }

    /**
     * 归约并更新状态
     *
     * 内部方法，用于通过 Reducer 更新状态。
     * 调用 [reduce] 方法计算新状态，然后更新 StateFlow。
     *
     * ## 线程安全
     * StateFlow 的 value 赋值是线程安全的，可以从任何协程中调用。
     *
     * @param action 触发状态变化的动作
     *
     * 使用示例：
     * ```kotlin
     * // 在 handleIntent 中调用
     * reduce(LoginAction.StartLoading)
     * reduce(LoginAction.LoginSuccess(user))
     * ```
     */
    protected fun reduce(action: A) {
        val newState = reduce(action)
        _state.value = newState
    }

    /**
     * 发送副作用事件
     *
     * 用于发送一次性的副作用事件（如导航、Toast、Dialog）。
     * 事件会被发送到 Channel 中，UI 层通过收集 [effect] Flow 来处理。
     *
     * ## 特点
     * - **一次性消费**：每个 Effect 只会被消费一次
     * - **缓冲机制**：使用 BUFFERED Channel，多个事件会被缓冲
     * - **非阻塞**：使用 trySend，不会阻塞当前协程
     *
     * ## 使用场景
     * - 导航跳转
     * - 显示 Toast/Snackbar
     * - 显示 Dialog
     * - 请求权限
     * - 调用系统功能
     *
     * @param effect 副作用事件
     *
     * 使用示例：
     * ```kotlin
     * // 在 handleIntent 或业务逻辑中
     * sendEffect(LoginEffect.ShowToast("登录成功"))
     * sendEffect(LoginEffect.NavigateToHome)
     * sendEffect(LoginEffect.ShowError(exception))
     * ```
     */
    protected fun sendEffect(effect: E) {
        viewModelScope.launch {
            _effect.trySend(effect)
        }
    }

    /**
     * ViewModel 清理时关闭 Effect Channel
     *
     * 确保不会有内存泄漏，当 ViewModel 被销毁时，Channel 会被正确关闭。
     */
    override fun onCleared() {
        super.onCleared()
        _effect.close()
    }
}
