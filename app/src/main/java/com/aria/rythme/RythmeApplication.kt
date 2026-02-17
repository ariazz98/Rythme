package com.aria.rythme

import android.app.Application
import com.aria.rythme.core.utils.RythmeLogger
import com.aria.rythme.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Rythme 应用入口
 *
 * 负责初始化应用级别的组件：
 * - Koin 依赖注入框架
 * - 日志工具
 *
 * ## 配置说明
 * 在 AndroidManifest.xml 中声明：
 * ```xml
 * <application
 *     android:name=".RythmeApplication"
 *     ... >
 * </application>
 * ```
 */
class RythmeApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化日志工具
        initializeLogger()

        // 初始化 Koin 依赖注入
        initializeKoin()
    }

    /**
     * 初始化日志工具
     *
     * 配置日志级别和输出行为
     */
    private fun initializeLogger() {
        RythmeLogger.apply {
            isEnabled = BuildConfig.DEBUG
            logLevel = if (BuildConfig.DEBUG) {
                RythmeLogger.LogLevel.DEBUG
            } else {
                RythmeLogger.LogLevel.ERROR
            }
        }
    }

    /**
     * 初始化 Koin 依赖注入
     *
     * 加载所有模块并启动 Koin
     */
    private fun initializeKoin() {
        startKoin {
            // 使用 Android 日志
            androidLogger(Level.ERROR)

            // 注入 Android 上下文
            androidContext(this@RythmeApplication)

            // 加载所有模块
            modules(appModules)
        }
    }
}
