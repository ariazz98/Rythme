package com.aria.rythme.ui.component

import android.os.Build
import android.view.Display
import android.view.Window
import android.content.pm.ActivityInfo
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.luminance
import com.aria.rythme.ui.theme.rythmeColors
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

// 来自用户确认的 HEIC：页面白色为 1，玻璃高光峰值约为 1.2。
internal const val GlassHdrHeadroom = 1.2f

internal fun glassHdrEligible(api: Int, darkTheme: Boolean, hdrDisplay: Boolean): Boolean =
    api >= 35 && !darkTheme && hdrDisplay

/** 合并显示系统的细碎亮度回调，不让浮点噪声反复重建绘制缓存。 */
internal fun glassHdrBucket(ratio: Float): Int =
    (normalizedHdrRatio(ratio) * 100f).roundToInt()

private fun normalizedHdrRatio(ratio: Float): Float =
    if (ratio.isFinite()) ratio.coerceIn(1f, 10_000f) else 1f

@Stable
internal class GlassHdrState {
    private val pressHeadrooms = mutableMapOf<Any, Float>()
    var pressAvailable by mutableStateOf(false)
        private set
    var darkTheme by mutableStateOf(false)
        private set
    val staticHeadroom: Float get() = if (darkTheme) 1f else GlassHdrHeadroom
    var requestedHeadroom by mutableFloatStateOf(GlassHdrHeadroom)
        private set

    /** 深色只开放顶部按压所需的 HDR，原静态玻璃 enabled 策略不变。 */
    fun configurePressEnvironment(available: Boolean, dark: Boolean) {
        pressAvailable = available
        darkTheme = dark
        if (!available) pressHeadrooms.clear()
        else pressHeadrooms.replaceAll { _, value -> value.coerceAtMost(if (dark) 7f else 2f) }
        updateHeadroom()
    }

    private fun updateHeadroom() {
        requestedHeadroom = maxOf(staticHeadroom, pressHeadrooms.values.maxOrNull() ?: staticHeadroom)
    }

    fun requestPressHeadroom(owner: Any, value: Float) {
        if (!value.isFinite() || value <= staticHeadroom) pressHeadrooms.remove(owner)
        else pressHeadrooms[owner] = value.coerceAtMost(if (darkTheme) 7f else 2f)
        updateHeadroom()
    }

    var enabled by mutableStateOf(false)
        private set
    var generation by mutableIntStateOf(0)
        private set
    private var ratioBucket = 100

    fun configure(enabled: Boolean, ratio: Float) {
        val bucket = glassHdrBucket(ratio)
        // 留一小段迟滞，避免自动亮度在两个 0.01 档位边界来回抖动。
        val ratioChanged = bucket != ratioBucket &&
            abs(normalizedHdrRatio(ratio) - ratioBucket / 100f) >= 0.006f
        if (this.enabled != enabled || ((enabled || pressAvailable) && ratioChanged)) {
            this.enabled = enabled
            ratioBucket = bucket
            generation++
        }
    }
}

internal val LocalGlassHdr = staticCompositionLocalOf { GlassHdrState() }

/** 只管理当前窗口的显示能力；页面和播放器状态不参与 HDR 缓存生命周期。 */
@Composable
internal fun ProvideGlassHdr(window: Window, display: Display?, content: @Composable () -> Unit) {
    val dark = MaterialTheme.rythmeColors.surface.luminance() < 0.5f
    val supported = Build.VERSION.SDK_INT >= 35 && display?.isHdr == true && display.isHdrSdrRatioAvailable
    val state = remember(window) { GlassHdrState().apply { configurePressEnvironment(supported, dark) } }
    val enabled = glassHdrEligible(Build.VERSION.SDK_INT, dark, supported)
    SideEffect { state.configurePressEnvironment(supported, dark) }

    DisposableEffect(window, display, supported, dark) {
        if (!supported || Build.VERSION.SDK_INT < 35) {
            state.configure(false, 1f)
            onDispose { }
        } else {
            val oldMode = window.colorMode
            val oldHeadroom = window.desiredHdrHeadroom
            val decor = window.decorView
            var disposed = false
            val refresh = Runnable {
                if (!disposed) state.configure(enabled, display.hdrSdrRatio)
            }
            val listener = Consumer<Display> {
                // 窗口先接收新的显示色彩空间，再在下一帧分配相关绘制缓存。
                decor.removeCallbacks(refresh)
                decor.postOnAnimation(refresh)
            }
            display.registerHdrSdrRatioChangedListener(decor.context.mainExecutor, listener)
            window.colorMode = ActivityInfo.COLOR_MODE_HDR
            window.desiredHdrHeadroom = state.requestedHeadroom
            decor.postOnAnimation(refresh)

            onDispose {
                disposed = true
                decor.removeCallbacks(refresh)
                display.unregisterHdrSdrRatioChangedListener(listener)
                window.desiredHdrHeadroom = oldHeadroom
                window.colorMode = oldMode
            }
        }
    }
    LaunchedEffect(window, supported) {
        if (supported && Build.VERSION.SDK_INT >= 35) {
            snapshotFlow { state.requestedHeadroom }.collect { window.desiredHdrHeadroom = it }
        }
    }
    CompositionLocalProvider(LocalGlassHdr provides state, content = content)
}

/**
 * 玻璃控件整组的透明度与模糊。只更新绘制缓存，不 key 内容或 Animatable。
 * 几何变换仍由原来的 graphicsLayer 处理，因此不改变触摸坐标和共享元素边界。
 */
@Composable
internal fun Modifier.glassHdrFadeAndBlur(
    alpha: () -> Float,
    blurDp: () -> Float = { 0f }
): Modifier {
    val hdr = LocalGlassHdr.current
    if (!hdr.enabled && !hdr.pressAvailable) return graphicsLayer { this.alpha = alpha() }.thenBlur(blurDp())

    return drawWithCache {
        // 此状态只在绘制缓存中读取，不在内容的组合/布局中读取。
        @Suppress("UNUSED_VARIABLE") val generation = hdr.generation
        val layer = obtainGraphicsLayer()
        var lastRadius = -1f
        onDrawWithContent {
            val opacity = alpha().coerceIn(0f, 1f)
            val radius = blurDp().let { if (it > 0.5f) it * density else 0f }
            if (opacity >= 1f && radius == 0f) {
                drawContent()
            } else if (opacity > 0f) {
                layer.alpha = opacity
                if (lastRadius != radius) {
                    layer.renderEffect = if (radius > 0f) BlurEffect(radius, radius, TileMode.Decal) else null
                    val outset = ceil(radius * 3f).toInt()
                    layer.setOutsets(outset, outset, outset, outset)
                    lastRadius = radius
                }
                layer.record { this@onDrawWithContent.drawContent() }
                drawLayer(layer)
            }
        }
    }
}
