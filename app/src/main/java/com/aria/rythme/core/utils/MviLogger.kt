package com.aria.rythme.core.utils

import android.util.Log
import com.aria.rythme.BuildConfig
import com.aria.rythme.core.mvi.MviAction
import com.aria.rythme.core.mvi.MviEffect
import com.aria.rythme.core.mvi.MviIntent
import com.aria.rythme.core.mvi.MviState

/**
 * MVI 架构日志工具
 *
 * 提供统一的 MVI 事件日志记录功能，帮助开发者追踪和调试 MVI 数据流。
 * 支持不同级别的日志输出，便于在开发和生产环境中灵活使用。
 *
 * ## 功能特性
 * 1. **Intent 日志**：记录用户发起的所有意图
 * 2. **Action 日志**：记录内部处理的所有动作
 * 3. **State 日志**：记录状态变化前后的对比
 * 4. **Effect 日志**：记录发送的所有副作用事件
 * 5. **性能分析**：记录状态更新耗时
 * 6. **调试模式**：仅在 Debug 模式下输出日志
 *
 * ## 日志级别
 * - **VERBOSE**: 最详细的日志，包含所有信息
 * - **DEBUG**: 调试信息，默认级别
 * - **INFO**: 一般信息
 * - **WARN**: 警告信息
 * - **ERROR**: 错误信息
 *
 * ## 使用示例
 * ```kotlin
 * class LoginViewModel : BaseViewModel<...>() {
 *
 *     override fun handleIntent(intent: LoginIntent) {
 *         // 记录 Intent
 *         MviLogger.logIntent("LoginViewModel", intent)
 *
 *         when (intent) {
 *             is LoginIntent.OnLoginClick -> {
 *                 viewModelScope.launch {
 *                     // 记录 Action
 *                     MviLogger.logAction("LoginViewModel", LoginAction.StartLoading)
 *                     reduce(LoginAction.StartLoading)
 *
 *                     try {
 *                         val user = loginUseCase()
 *                         MviLogger.logAction("LoginViewModel", LoginAction.LoginSuccess(user))
 *                         reduce(LoginAction.LoginSuccess(user))
 *
 *                         // 记录 Effect
 *                         MviLogger.logEffect("LoginViewModel", LoginEffect.NavigateToHome)
 *                         sendEffect(LoginEffect.NavigateToHome)
 *                     } catch (e: Exception) {
 *                         MviLogger.logError("LoginViewModel", "Login failed", e)
 *                     }
 *                 }
 *             }
 *         }
 *     }
 *
 *     override fun reduce(action: LoginAction): LoginState {
 *         val oldState = state.value
 *         val newState = when (action) {
 *             // ...
 *         }
 *         // 记录状态变化
 *         MviLogger.logStateChange("LoginViewModel", oldState, newState)
 *         return newState
 *     }
 * }
 * ```
 *
 * ## 自定义日志行为
 * ```kotlin
 * // 设置是否启用日志（默认在 Debug 模式下启用）
 * MviLogger.isEnabled = BuildConfig.DEBUG
 *
 * // 设置日志级别
 * MviLogger.logLevel = MviLogger.LogLevel.VERBOSE
 *
 * // 自定义日志处理器（如：写入文件、上报到服务器）
 * MviLogger.setCustomLogger { tag, message ->
 *     // 自定义日志处理
 * }
 * ```
 */
object MviLogger {

    /**
     * 日志级别枚举
     */
    enum class LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    /**
     * 是否启用日志
     * 默认在 Debug 构建时启用，Release 构建时禁用
     */
    var isEnabled: Boolean = BuildConfig.DEBUG

    /**
     * 当前日志级别
     * 默认为 DEBUG 级别
     */
    var logLevel: LogLevel = LogLevel.DEBUG

    /**
     * 自定义日志处理器
     * 可以设置自定义的日志处理逻辑，如写入文件或上报到服务器
     */
    private var customLogger: ((tag: String, message: String) -> Unit)? = null

    /**
     * MVI 日志标签前缀
     */
    private const val TAG_PREFIX = "MVI"

    /**
     * 设置自定义日志处理器
     *
     * @param logger 自定义日志处理函数
     */
    fun setCustomLogger(logger: (tag: String, message: String) -> Unit) {
        customLogger = logger
    }

    /**
     * 记录 Intent 日志
     *
     * 当 UI 层发送用户意图时调用，记录用户的交互行为。
     *
     * @param viewModelName ViewModel 的名称
     * @param intent 用户发起的意图
     */
    fun <I : MviIntent> logIntent(viewModelName: String, intent: I) {
        if (!isEnabled) return

        val tag = "$TAG_PREFIX-$viewModelName"
        val message = "📥 Intent: ${intent::class.simpleName} -> $intent"
        log(tag, message, LogLevel.DEBUG)
    }

    /**
     * 记录 Action 日志
     *
     * 当 ViewModel 内部生成 Action 时调用，记录业务逻辑的处理过程。
     *
     * @param viewModelName ViewModel 的名称
     * @param action 内部处理的动作
     */
    fun <A : MviAction> logAction(viewModelName: String, action: A) {
        if (!isEnabled) return

        val tag = "$TAG_PREFIX-$viewModelName"
        val message = "⚙️ Action: ${action::class.simpleName} -> $action"
        log(tag, message, LogLevel.DEBUG)
    }

    /**
     * 记录状态变化日志
     *
     * 当状态发生变化时调用，记录状态变化的前后对比。
     * 可以帮助追踪状态变化的原因和结果。
     *
     * @param viewModelName ViewModel 的名称
     * @param oldState 变化前的状态
     * @param newState 变化后的状态
     */
    fun <S : MviState> logStateChange(viewModelName: String, oldState: S, newState: S) {
        if (!isEnabled) return

        val tag = "$TAG_PREFIX-$viewModelName"
        val message = buildString {
            appendLine("📊 State Changed:")
            appendLine("  Old: $oldState")
            appendLine("  New: $newState")
            
            // 计算并显示变化的字段（如果状态是 data class）
            if (oldState != newState) {
                appendLine("  Changed fields: ${getChangedFields(oldState, newState)}")
            }
        }
        log(tag, message, LogLevel.DEBUG)
    }

    /**
     * 记录 Effect 日志
     *
     * 当发送副作用事件时调用，记录一次性事件的触发。
     *
     * @param viewModelName ViewModel 的名称
     * @param effect 副作用事件
     */
    fun <E : MviEffect> logEffect(viewModelName: String, effect: E) {
        if (!isEnabled) return

        val tag = "$TAG_PREFIX-$viewModelName"
        val message = "✨ Effect: ${effect::class.simpleName} -> $effect"
        log(tag, message, LogLevel.INFO)
    }

    /**
     * 记录错误日志
     *
     * 当发生错误时调用，记录错误信息和堆栈跟踪。
     *
     * @param viewModelName ViewModel 的名称
     * @param message 错误描述信息
     * @param throwable 异常对象（可选）
     */
    fun logError(viewModelName: String, message: String, throwable: Throwable? = null) {
        if (!isEnabled) return

        val tag = "$TAG_PREFIX-$viewModelName"
        val errorMessage = buildString {
            append("❌ Error: $message")
            throwable?.let {
                appendLine()
                appendLine("  Exception: ${it::class.simpleName}")
                appendLine("  Message: ${it.message}")
                appendLine("  StackTrace: ${it.stackTraceToString()}")
            }
        }
        log(tag, errorMessage, LogLevel.ERROR)
    }

    /**
     * 记录警告日志
     *
     * @param viewModelName ViewModel 的名称
     * @param message 警告信息
     */
    fun logWarning(viewModelName: String, message: String) {
        if (!isEnabled) return

        val tag = "$TAG_PREFIX-$viewModelName"
        log(tag, "⚠️ Warning: $message", LogLevel.WARN)
    }

    /**
     * 记录详细日志
     *
     * 用于记录详细的调试信息。
     *
     * @param viewModelName ViewModel 的名称
     * @param message 日志信息
     */
    fun logVerbose(viewModelName: String, message: String) {
        if (!isEnabled) return

        val tag = "$TAG_PREFIX-$viewModelName"
        log(tag, "💬 $message", LogLevel.VERBOSE)
    }

    /**
     * 记录性能指标
     *
     * 用于记录操作耗时等性能相关信息。
     *
     * @param viewModelName ViewModel 的名称
     * @param operation 操作名称
     * @param durationMs 耗时（毫秒）
     */
    fun logPerformance(viewModelName: String, operation: String, durationMs: Long) {
        if (!isEnabled) return

        val tag = "$TAG_PREFIX-$viewModelName"
        val message = "⏱️ Performance: $operation took ${durationMs}ms"
        log(tag, message, LogLevel.INFO)
    }

    /**
     * 统一的日志输出方法
     *
     * @param tag 日志标签
     * @param message 日志信息
     * @param level 日志级别
     */
    private fun log(tag: String, message: String, level: LogLevel) {
        // 检查日志级别
        if (level.ordinal < logLevel.ordinal) return

        // 使用自定义日志处理器
        customLogger?.invoke(tag, message)

        // 使用系统日志
        when (level) {
            LogLevel.VERBOSE -> Log.v(tag, message)
            LogLevel.DEBUG -> Log.d(tag, message)
            LogLevel.INFO -> Log.i(tag, message)
            LogLevel.WARN -> Log.w(tag, message)
            LogLevel.ERROR -> Log.e(tag, message)
        }
    }

    /**
     * 获取状态变化的字段
     *
     * 尝试通过反射获取变化的字段名称（仅用于调试）
     *
     * @param oldState 旧状态
     * @param newState 新状态
     * @return 变化的字段列表
     */
    private fun <S : MviState> getChangedFields(oldState: S, newState: S): String {
        return try {
            val changes = mutableListOf<String>()
            val oldClass = oldState::class.java
            val newClass = newState::class.java

            if (oldClass == newClass) {
                oldClass.declaredFields.forEach { field ->
                    field.isAccessible = true
                    val oldValue = field.get(oldState)
                    val newValue = field.get(newState)
                    if (oldValue != newValue) {
                        changes.add("${field.name}: $oldValue -> $newValue")
                    }
                }
            }

            if (changes.isEmpty()) "none" else changes.joinToString(", ")
        } catch (e: Exception) {
            "unable to detect (${e.message})"
        }
    }
}

/**
 * ViewModel 扩展函数：简化日志记录
 *
 * 为 BaseViewModel 提供便捷的日志记录方法，自动使用 ViewModel 的类名作为标签。
 */

/**
 * 记录 Intent 的便捷方法
 */
fun <I : MviIntent, S : MviState, A : MviAction, E : MviEffect> 
    com.aria.rythme.core.mvi.BaseViewModel<I, S, A, E>.logIntent(intent: I) {
    MviLogger.logIntent(this::class.simpleName ?: "Unknown", intent)
}

/**
 * 记录 Action 的便捷方法
 */
fun <I : MviIntent, S : MviState, A : MviAction, E : MviEffect> 
    com.aria.rythme.core.mvi.BaseViewModel<I, S, A, E>.logAction(action: A) {
    MviLogger.logAction(this::class.simpleName ?: "Unknown", action)
}

/**
 * 记录 Effect 的便捷方法
 */
fun <I : MviIntent, S : MviState, A : MviAction, E : MviEffect> 
    com.aria.rythme.core.mvi.BaseViewModel<I, S, A, E>.logEffect(effect: E) {
    MviLogger.logEffect(this::class.simpleName ?: "Unknown", effect)
}

/**
 * 记录错误的便捷方法
 */
fun <I : MviIntent, S : MviState, A : MviAction, E : MviEffect> 
    com.aria.rythme.core.mvi.BaseViewModel<I, S, A, E>.logError(message: String, throwable: Throwable? = null) {
    MviLogger.logError(this::class.simpleName ?: "Unknown", message, throwable)
}
