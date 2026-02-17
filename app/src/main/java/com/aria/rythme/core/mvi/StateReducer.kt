package com.aria.rythme.core.mvi

/**
 * MVI 架构 - 状态归约器接口
 *
 * StateReducer 是一个函数式接口，负责将 Action 转换为新的 State。
 * 这是 MVI 架构中状态管理的核心，实现了可预测的状态转换逻辑。
 *
 * ## 设计理念
 * StateReducer 受到 Redux Reducer 的启发，强调：
 * - **纯函数**：相同的输入总是产生相同的输出
 * - **不可变性**：不修改原状态，总是返回新状态
 * - **可预测性**：状态变化逻辑清晰可追踪
 * - **可测试性**：纯函数易于单元测试
 *
 * ## 泛型参数
 * @param S [MviState] UI 状态类型
 * @param A [MviAction] 内部动作类型
 *
 * ## 核心原则
 * 1. **纯函数**：
 *    - 不应该有副作用（如网络请求、数据库操作）
 *    - 不应该修改外部状态或依赖
 *    - 不应该产生随机值或依赖时间
 *
 * 2. **不可变性**：
 *    - 不能修改传入的 currentState
 *    - 使用 copy() 方法创建新状态
 *    - 所有状态属性都应该是 val
 *
 * 3. **完备性**：
 *    - 应该处理所有可能的 Action
 *    - 使用 sealed interface/class 确保类型安全
 *    - 对于未知 Action 返回原状态
 *
 * ## 使用示例
 * ```kotlin
 * // 定义 Action
 * sealed interface CounterAction : MviAction {
 *     data object Increment : CounterAction
 *     data object Decrement : CounterAction
 *     data class SetValue(val value: Int) : CounterAction
 *     data object Reset : CounterAction
 * }
 *
 * // 定义 State
 * data class CounterState(
 *     val count: Int = 0,
 *     val isAtMax: Boolean = false
 * ) : MviState
 *
 * // 实现 Reducer
 * class CounterReducer : StateReducer<CounterState, CounterAction> {
 *     override fun reduce(currentState: CounterState, action: CounterAction): CounterState {
 *         return when (action) {
 *             is CounterAction.Increment -> {
 *                 val newCount = currentState.count + 1
 *                 currentState.copy(
 *                     count = newCount,
 *                     isAtMax = newCount >= 100
 *                 )
 *             }
 *             is CounterAction.Decrement -> {
 *                 val newCount = (currentState.count - 1).coerceAtLeast(0)
 *                 currentState.copy(
 *                     count = newCount,
 *                     isAtMax = false
 *                 )
 *             }
 *             is CounterAction.SetValue -> {
 *                 currentState.copy(
 *                     count = action.value,
 *                     isAtMax = action.value >= 100
 *                 )
 *             }
 *             is CounterAction.Reset -> {
 *                 CounterState() // 返回初始状态
 *             }
 *         }
 *     }
 * }
 *
 * // 在 ViewModel 中使用
 * class CounterViewModel(
 *     private val reducer: CounterReducer
 * ) : BaseViewModel<CounterIntent, CounterState, CounterAction, CounterEffect>() {
 *
 *     override fun reduce(action: CounterAction): CounterState {
 *         return reducer.reduce(state.value, action)
 *     }
 * }
 * ```
 *
 * ## 复杂业务场景示例
 * ```kotlin
 * // 登录场景的 Reducer
 * class LoginReducer : StateReducer<LoginState, LoginAction> {
 *     override fun reduce(currentState: LoginState, action: LoginAction): LoginState {
 *         return when (action) {
 *             is LoginAction.UpdateUsername -> {
 *                 currentState.copy(
 *                     username = action.username,
 *                     // 根据用户名和密码自动计算登录按钮是否可用
 *                     isLoginEnabled = action.username.isNotEmpty() && 
 *                                    currentState.password.isNotEmpty(),
 *                     // 清除之前的错误信息
 *                     errorMessage = null
 *                 )
 *             }
 *             is LoginAction.UpdatePassword -> {
 *                 currentState.copy(
 *                     password = action.password,
 *                     isLoginEnabled = currentState.username.isNotEmpty() && 
 *                                    action.password.isNotEmpty(),
 *                     errorMessage = null
 *                 )
 *             }
 *             is LoginAction.StartLoading -> {
 *                 currentState.copy(
 *                     isLoading = true,
 *                     isLoginEnabled = false, // 加载时禁用按钮
 *                     errorMessage = null
 *                 )
 *             }
 *             is LoginAction.LoginSuccess -> {
 *                 currentState.copy(
 *                     isLoading = false,
 *                     user = action.user,
 *                     isLoggedIn = true
 *                 )
 *             }
 *             is LoginAction.LoginFailure -> {
 *                 currentState.copy(
 *                     isLoading = false,
 *                     isLoginEnabled = true, // 失败后重新启用按钮
 *                     errorMessage = action.error.message,
 *                     loginAttempts = currentState.loginAttempts + 1
 *                 )
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## Reducer 组合模式
 * 对于复杂的状态，可以将 Reducer 拆分为多个小的 Reducer：
 * ```kotlin
 * // 拆分为多个子 Reducer
 * class UserInfoReducer : StateReducer<UserInfoState, UserAction> { ... }
 * class LoadingReducer : StateReducer<LoadingState, LoadingAction> { ... }
 *
 * // 组合 Reducer
 * class AppReducer(
 *     private val userInfoReducer: UserInfoReducer,
 *     private val loadingReducer: LoadingReducer
 * ) : StateReducer<AppState, AppAction> {
 *     override fun reduce(currentState: AppState, action: AppAction): AppState {
 *         return when (action) {
 *             is AppAction.UserAction -> {
 *                 currentState.copy(
 *                     userInfo = userInfoReducer.reduce(currentState.userInfo, action)
 *                 )
 *             }
 *             is AppAction.LoadingAction -> {
 *                 currentState.copy(
 *                     loading = loadingReducer.reduce(currentState.loading, action)
 *                 )
 *             }
 *             else -> currentState
 *         }
 *     }
 * }
 * ```
 *
 * ## 单元测试示例
 * ```kotlin
 * class CounterReducerTest {
 *     private val reducer = CounterReducer()
 *
 *     @Test
 *     fun `when increment action, count should increase by 1`() {
 *         // Given
 *         val initialState = CounterState(count = 5)
 *         val action = CounterAction.Increment
 *
 *         // When
 *         val newState = reducer.reduce(initialState, action)
 *
 *         // Then
 *         assertEquals(6, newState.count)
 *         assertFalse(newState.isAtMax)
 *     }
 *
 *     @Test
 *     fun `when increment to 100, isAtMax should be true`() {
 *         // Given
 *         val initialState = CounterState(count = 99)
 *         val action = CounterAction.Increment
 *
 *         // When
 *         val newState = reducer.reduce(initialState, action)
 *
 *         // Then
 *         assertEquals(100, newState.count)
 *         assertTrue(newState.isAtMax)
 *     }
 * }
 * ```
 *
 * ## 最佳实践
 * 1. **保持纯函数**：不要在 Reducer 中执行副作用操作
 * 2. **使用 sealed 类型**：确保所有 Action 都被处理
 * 3. **提取复杂逻辑**：将复杂的计算逻辑提取为私有方法
 * 4. **状态验证**：确保新状态的合法性（如：计数器不能为负数）
 * 5. **文档注释**：为每个 Action 分支添加注释说明状态变化逻辑
 * 6. **拆分 Reducer**：当 Reducer 过于复杂时，考虑拆分为多个子 Reducer
 * 7. **单元测试**：为每个 Action 编写单元测试，确保状态转换正确
 *
 * ## Reducer vs ViewModel
 * | 特性 | Reducer | ViewModel |
 * |------|---------|-----------|
 * | 职责 | 纯状态转换 | 业务逻辑协调 |
 * | 副作用 | 不允许 | 允许 |
 * | 测试 | 简单（纯函数） | 复杂（需要 mock） |
 * | 可预测性 | 完全可预测 | 依赖外部状态 |
 *
 * @see MviState UI 状态接口
 * @see MviAction 内部动作接口
 * @see BaseViewModel MVI ViewModel 基类
 */
fun interface StateReducer<S : MviState, A : MviAction> {
    /**
     * 归约状态
     *
     * 根据当前状态和动作，计算并返回新的状态。
     * 这必须是一个纯函数，不能有任何副作用。
     *
     * @param currentState 当前的 UI 状态
     * @param action 触发状态变化的动作
     * @return 新的 UI 状态
     */
    fun reduce(currentState: S, action: A): S
}
