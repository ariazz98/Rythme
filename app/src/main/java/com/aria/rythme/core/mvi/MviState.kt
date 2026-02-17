package com.aria.rythme.core.mvi

/**
 * MVI 架构 - UI 状态接口
 *
 * State 代表界面在某一时刻的完整状态快照。
 * 这是 MVI 架构中的 Model，包含了渲染 UI 所需的所有数据。
 *
 * ## 设计原则
 * 1. **完整性**：State 应该包含渲染 UI 所需的所有数据
 * 2. **不可变性**：State 必须是不可变的（使用 data class，所有属性为 val）
 * 3. **单一数据源**：UI 只从一个 State 对象获取数据
 * 4. **可预测性**：相同的 State 总是产生相同的 UI
 * 5. **可序列化**：建议 State 可以被序列化，便于状态恢复和调试
 *
 * ## 最佳实践
 * - 使用 data class 定义具体的 State
 * - 所有属性使用 val（不可变）
 * - 为属性提供合理的默认值
 * - 使用 copy() 方法创建新状态
 * - 避免在 State 中存储 UI 相关的对象（如 Context、View）
 *
 * ## 使用示例
 * ```kotlin
 * // 定义具体功能的 State
 * data class LoginState(
 *     val username: String = "",
 *     val password: String = "",
 *     val isLoading: Boolean = false,
 *     val isLoginEnabled: Boolean = false,
 *     val errorMessage: String? = null
 * ) : MviState
 *
 * // 在 ViewModel 中更新 State
 * _state.value = currentState.copy(isLoading = true)
 *
 * // 在 Compose 中使用 State
 * val state by viewModel.state.collectAsState()
 * if (state.isLoading) {
 *     CircularProgressIndicator()
 * }
 * ```
 *
 * ## 数据流向
 * ```
 * Intent → Action → Reducer → New State → UI Update
 * ```
 *
 * ## State vs Effect
 * - **State**: 持久的 UI 状态（如：用户名、加载状态、列表数据）
 * - **Effect**: 一次性事件（如：显示 Toast、导航跳转、显示 Dialog）
 *
 * @see MviIntent 用户意图
 * @see MviEffect 副作用事件
 * @see MviAction 内部处理动作
 */
interface MviState
