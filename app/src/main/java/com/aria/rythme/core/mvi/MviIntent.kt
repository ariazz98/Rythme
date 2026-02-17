package com.aria.rythme.core.mvi

/**
 * MVI 架构 - 用户意图接口
 *
 * Intent 代表用户在界面上的所有交互行为和操作意图。
 * 这是 MVI 单向数据流的起点，所有用户行为都应该被封装为一个 Intent。
 *
 * ## 设计原则
 * 1. **语义化命名**：Intent 名称应该清晰表达用户意图（如：OnLoginClick、OnTextChanged）
 * 2. **携带数据**：Intent 可以携带操作所需的数据（如：OnTextChanged(text: String)）
 * 3. **不可变性**：Intent 应该是不可变的（使用 data class 或 object）
 * 4. **单一职责**：每个 Intent 只代表一个用户行为
 *
 * ## 使用示例
 * ```kotlin
 * // 定义具体功能的 Intent
 * sealed interface LoginIntent : MviIntent {
 *     data class OnUsernameChanged(val username: String) : LoginIntent
 *     data class OnPasswordChanged(val password: String) : LoginIntent
 *     data object OnLoginClick : LoginIntent
 *     data object OnForgotPasswordClick : LoginIntent
 * }
 *
 * // 在 Compose 中发送 Intent
 * viewModel.sendIntent(LoginIntent.OnLoginClick)
 * ```
 *
 * ## 数据流向
 * ```
 * User Action → Intent → ViewModel → Action → State → UI Update
 * ```
 *
 * @see MviState UI 状态的表示
 * @see MviAction 内部处理动作
 * @see MviEffect 副作用事件
 */
interface MviIntent
