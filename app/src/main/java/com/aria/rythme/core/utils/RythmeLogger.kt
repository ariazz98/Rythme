package com.aria.rythme.core.utils

import android.util.Log

/** 应用级轻量日志门面。 */
object RythmeLogger {
    enum class LogLevel {
        VERBOSE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }

    var isEnabled: Boolean = true
    var logLevel: LogLevel = LogLevel.DEBUG

    private var customLogger: ((tag: String, message: String) -> Unit)? = null

    fun setCustomLogger(logger: ((tag: String, message: String) -> Unit)?) {
        customLogger = logger
    }

    fun d(tag: String, message: String) = log(tag, message, LogLevel.DEBUG)

    fun i(tag: String, message: String) = log(tag, message, LogLevel.INFO)

    fun w(tag: String, message: String) = log(tag, message, LogLevel.WARN)

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMessage = if (throwable == null) {
            message
        } else {
            "$message\n${throwable.stackTraceToString()}"
        }
        log(tag, fullMessage, LogLevel.ERROR)
    }

    private fun log(tag: String, message: String, level: LogLevel) {
        if (!isEnabled || level.ordinal < logLevel.ordinal) return

        val fullTag = "Rythme-$tag"
        customLogger?.invoke(fullTag, message)
        when (level) {
            LogLevel.VERBOSE -> Log.v(fullTag, message)
            LogLevel.DEBUG -> Log.d(fullTag, message)
            LogLevel.INFO -> Log.i(fullTag, message)
            LogLevel.WARN -> Log.w(fullTag, message)
            LogLevel.ERROR -> Log.e(fullTag, message)
        }
    }
}
