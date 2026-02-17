package com.aria.rythme.core.mvi

/**
 * MVI 架构 - 内部动作接口
 *
 * Action 代表 ViewModel 内部处理的中间动作或业务逻辑结果。
 * 它是 Intent 和 State 之间的桥梁，将用户意图转化为具体的状态变化。
 *
 * ## 设计原则
 * 1. **内部使用**：Action 仅在 ViewModel 内部使用，不对外暴露
 * 2. **业务语义**：Action 应该表达业务逻辑的结果（如：LoadSuccess、LoadFailure）
 * 3. **携带数据**：Action 可以携带处理后的业务数据
 * 4. **不可变性**：Action 应该是不可变的（使用 data class 或 object）
 *
 * ## 工作流程
 * ```
 * 1. UI 发送 Intent（用户点击登录按钮）
 *    ↓
 * 2. ViewModel 将 Intent 转换为 Action（开始加载数据）
 *    ↓
 * 3. 执行业务逻辑（调用 API、访问数据库）
 *    ↓
 * 4. 根据结果生成新的 Action（成功/失败）
 *    ↓
 * 5. Reducer 根据 Action 生成新的 State
 *    ↓
 * 6. UI 观察到 State 变化并更新界面
 * ```
 *
 * ## Intent vs Action 区别
 * | 特性 | Intent | Action |
 * |------|--------|--------|
 * | 来源 | UI 层发起 | ViewModel 内部生成 |
 * | 语义 | 用户意图（点击、输入） | 业务结果（成功、失败） |
 * | 可见性 | 对外暴露 | 内部使用 |
 * | 示例 | OnLoginClick | LoginSuccess, LoginFailure |
 *
 * ## 使用示例
 * ```kotlin
 * // 定义具体功能的 Action
 * sealed interface LoginAction : MviAction {
 *     // 加载动作
 *     data object StartLoading : LoginAction
 *     
 *     // 成功动作（携带数据）
 *     data class LoginSuccess(val user: User, val token: String) : LoginAction
 *     
 *     // 失败动作（携带错误信息）
 *     data class LoginFailure(val error: Throwable) : LoginAction
 *     
 *     // 验证动作
 *     data class ValidateInput(val isValid: Boolean) : LoginAction
 * }
 *
 * // 在 ViewModel 中处理 Intent 并生成 Action
 * override fun handleIntent(intent: MviIntent) {
 *     when (intent) {
 *         is LoginIntent.OnLoginClick -> {
 *             viewModelScope.launch {
 *                 // 1. 发送开始加载的 Action
 *                 reduce(LoginAction.StartLoading)
 *                 
 *                 // 2. 执行业务逻辑
 *                 try {
 *                     val result = loginUseCase(username, password)
 *                     // 3. 成功后发送成功 Action
 *                     reduce(LoginAction.LoginSuccess(result.user, result.token))
 *                     sendEffect(LoginEffect.NavigateToHome)
 *                 } catch (e: Exception) {
 *                     // 4. 失败后发送失败 Action
 *                     reduce(LoginAction.LoginFailure(e))
 *                     sendEffect(LoginEffect.ShowToast("登录失败"))
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Reducer 处理 Action
 * ```kotlin
 * override fun reduce(action: MviAction) {
 *     val newState = when (action) {
 *         is LoginAction.StartLoading -> {
 *             currentState.copy(isLoading = true, errorMessage = null)
 *         }
 *         is LoginAction.LoginSuccess -> {
 *             currentState.copy(
 *                 isLoading = false,
 *                 user = action.user,
 *                 isLoggedIn = true
 *             )
 *         }
 *         is LoginAction.LoginFailure -> {
 *             currentState.copy(
 *                 isLoading = false,
 *                 errorMessage = action.error.message
 *             )
 *         }
 *         else -> currentState
 *     }
 *     _state.value = newState
 * }
 * ```
 *
 * ## 最佳实践
 * - Action 命名应该是完成时态（如：LoadSuccess 而非 LoadSuccessAction）
 * - 使用 sealed interface/class 定义所有可能的 Action
 * - Action 应该携带足够的信息以便 Reducer 更新 State
 * - 复杂业务可以拆分为多个 Action（如：Loading → Success/Failure）
 * - 不要在 Action 中存储可变对象或回调函数
 *
 * ## 数据流向
 * ```
 * Intent → [Intent Handler] → Action → [Reducer] → New State → UI
 *                                  ↓
 *                              [Effect Handler] → Effect → UI Side Effect
 * ```
 *
 * @see MviIntent 用户意图
 * @see MviState UI 状态
 * @see MviEffect 副作用事件
 * @see StateReducer 状态归约器
 */
interface MviAction
