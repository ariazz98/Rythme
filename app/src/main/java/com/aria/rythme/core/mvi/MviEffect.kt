package com.aria.rythme.core.mvi

/**
 * MVI 架构 - 副作用事件接口
 *
 * Effect 代表不属于 UI 状态的一次性事件或副作用。
 * 这些事件只会被消费一次，不应该成为持久的 UI 状态的一部分。
 *
 * ## 设计原则
 * 1. **一次性消费**：Effect 只应该被处理一次，处理后自动清除
 * 2. **非状态化**：Effect 不应该影响 UI 的持久状态
 * 3. **副作用处理**：用于处理导航、Toast、Dialog、权限请求等副作用操作
 * 4. **不可变性**：Effect 应该是不可变的（使用 data class 或 object）
 *
 * ## 使用场景
 * - **导航跳转**：跳转到其他页面
 * - **显示提示**：Toast、Snackbar 消息
 * - **弹窗显示**：Dialog、BottomSheet
 * - **系统交互**：请求权限、打开系统设置
 * - **外部调用**：分享、调用第三方应用
 * - **一次性动画**：震动、音效播放
 *
 * ## 使用示例
 * ```kotlin
 * // 定义具体功能的 Effect
 * sealed interface LoginEffect : MviEffect {
 *     data class ShowToast(val message: String) : LoginEffect
 *     data object NavigateToHome : LoginEffect
 *     data class ShowError(val error: Throwable) : LoginEffect
 *     data object RequestBiometricAuth : LoginEffect
 * }
 *
 * // 在 ViewModel 中发送 Effect
 * sendEffect(LoginEffect.ShowToast("登录成功"))
 * sendEffect(LoginEffect.NavigateToHome)
 *
 * // 在 Compose 中处理 Effect
 * LaunchedEffect(Unit) {
 *     viewModel.effect.collect { effect ->
 *         when (effect) {
 *             is LoginEffect.ShowToast -> {
 *                 // 显示 Toast
 *                 Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
 *             }
 *             is LoginEffect.NavigateToHome -> {
 *                 // 导航到首页
 *                 navController.navigate("home")
 *             }
 *             is LoginEffect.ShowError -> {
 *                 // 显示错误对话框
 *                 showErrorDialog(effect.error)
 *             }
 *             is LoginEffect.RequestBiometricAuth -> {
 *                 // 请求生物识别认证
 *                 biometricPrompt.authenticate()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * ## 数据流向
 * ```
 * Business Logic → Effect → UI Side Effect Handler
 * ```
 *
 * ## State vs Effect 对比
 * | 特性 | State | Effect |
 * |------|-------|--------|
 * | 持久性 | 持久的 UI 状态 | 一次性事件 |
 * | 订阅 | 始终可观察 | 触发后消失 |
 * | 重组 | 影响 UI 重组 | 不影响 UI 重组 |
 * | 示例 | 用户名、加载状态 | Toast、导航 |
 *
 * ## 最佳实践
 * - 不要在 Effect 中存储 UI 状态数据
 * - Effect 应该尽快处理，避免阻塞 UI
 * - 使用 sealed interface/class 定义所有可能的 Effect
 * - 在 LaunchedEffect 或 DisposableEffect 中收集 Effect
 * - 确保 Effect 只被处理一次（使用 Channel 而非 StateFlow）
 *
 * @see MviIntent 用户意图
 * @see MviState UI 状态
 * @see MviAction 内部处理动作
 */
interface MviEffect
