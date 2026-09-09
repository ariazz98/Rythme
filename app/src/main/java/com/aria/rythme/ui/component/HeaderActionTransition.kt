package com.aria.rythme.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs

internal data class HeaderActionGlyph(
    val action: Action,
    val x: Float,
    val width: Float,
    val shell: Int,
    val alpha: Float = 1f,
    val blur: Float = 0f,
    val scale: Float = 1f
) {
    // 状态色、文案和回调不参与换图；同一图案可跨分组保留。
    val identity: String get() = when (action) {
        is Action.Icon -> "icon:${action.key}:${action.iconRes}:${action.iconSize.value}"
        is Action.Avatar -> "avatar:${action.key}:${action.url}:${action.name}"
    }
}

internal data class HeaderActionShell(val bounds: Rect, val keys: Set<String>)
internal data class HeaderActionFrame(
    val width: Float,
    val shells: List<HeaderActionShell>,
    val glyphs: List<HeaderActionGlyph>,
    val shellAlpha: Float = 1f,
    val shellBlur: Float = 0f,
    val connection: Float = 0f
)

/** 坐标以整个操作区的右边为 0，沿用稳定态的槽宽、光学偏移与组间距。 */
internal fun headerActionLayout(auxiliary: List<Action>, actions: List<Action>): HeaderActionFrame {
    val groups = listOf(auxiliary, actions).filter { it.isNotEmpty() }
    val width = groups.sumOf { (TopBarComponentMetrics.surfaceWidth(it.size) + TopBarComponentMetrics.GroupGap).toDouble() }.toFloat()
    var x = -width
    val shells = mutableListOf<HeaderActionShell>()
    val glyphs = mutableListOf<HeaderActionGlyph>()
    groups.forEachIndexed { groupIndex, group ->
        val surfaceWidth = TopBarComponentMetrics.surfaceWidth(group.size)
        val left = x + TopBarComponentMetrics.GroupGap / 2f
        shells += HeaderActionShell(Rect(left, 11.5f, left + surfaceWidth, 56.5f), group.map { it.key }.toSet())
        val slotWidth = TopBarComponentMetrics.touchWidth(group.size)
        group.forEachIndexed { index, action ->
            glyphs += HeaderActionGlyph(action, left + slotWidth * (index + 0.5f) + TopBarComponentMetrics.iconOffset(group.size, index), slotWidth, groupIndex)
        }
        x += surfaceWidth + TopBarComponentMetrics.GroupGap
    }
    return HeaderActionFrame(width, shells, glyphs)
}

private fun mix(a: Float, b: Float, p: Float) = a + (b - a) * p
private fun phase(p: Float, start: Float, end: Float) = ((p - start) / (end - start)).coerceIn(0f, 1f)
private fun mix(a: Rect, b: Rect, p: Float) = Rect(mix(a.left, b.left, p), mix(a.top, b.top, p), mix(a.right, b.right, p), mix(a.bottom, b.bottom, p))

/** 09-09 PQ 原片：约 100ms 达峰、240ms 过零、330ms 轻微回缩、约半秒回正。 */
internal object HeaderActionMotion {
    const val DurationMillis = 520
    const val TailMillis = DurationMillis - HeaderLayout.navigationDuration
    const val ConnectionSmoothing = 18f
    fun navigationPhase(visibility: Float): Float {
        // 输入是页面透明度曲线，不是线性时间。反解同一曲线，避免再把鼓起/换图集中压缩一次。
        var low = 0f
        var high = 1f
        repeat(20) {
            val middle = (low + high) / 2f
            if (FastOutSlowInEasing.transform(middle) < visibility) low = middle else high = middle
        }
        val time = when {
            visibility <= 0f -> 0f
            visibility >= 1f -> 1f
            else -> (low + high) / 2f
        }
        return time * HeaderLayout.navigationDuration / DurationMillis
    }
    fun smooth(t: Float, start: Float, end: Float): Float = phase(t, start, end).let { it * it * (3f - 2f * it) }
    fun geometry(t: Float): Float = phase(t, 20f, 260f).let { 1f - (1f - it) * (1f - it) * (1f - it) }
    fun pulse(t: Float): Float {
        // Hermite 节点取自高度曲线；过零处有斜率，不在静态尺寸停顿再另起一次缩小。
        val times = floatArrayOf(0f, 100f, 240f, 330f, 520f)
        val values = floatArrayOf(0f, 1f, 0f, -0.16f, 0f)
        val slopes = floatArrayOf(0f, 0f, -0.004f, 0f, 0f)
        val i = (0..3).firstOrNull { t <= times[it + 1] } ?: return 0f
        val span = times[i + 1] - times[i]
        val p = phase(t, times[i], times[i + 1])
        val p2 = p * p
        val p3 = p2 * p
        return (2f * p3 - 3f * p2 + 1f) * values[i] + (p3 - 2f * p2 + p) * span * slopes[i] +
            (-2f * p3 + 3f * p2) * values[i + 1] + (p3 - p2) * span * slopes[i + 1]
    }
}

/** 导航模型只接受当前画面和目标布局，不接收页面名称，不排队运行旧动画。 */
internal class HeaderActionMorph(private val from: HeaderActionFrame, private val to: HeaderActionFrame) {
    private data class Carrier(val source: Int?, val target: Int?)
    private val carriers: List<Carrier> = run {
        // 表面按空间位置从右侧延续；图标身份单独匹配，不让表面追着某个图标穿越。
        val sources = from.shells.indices.sortedByDescending { from.shells[it].bounds.center.x }
        val targets = to.shells.indices.sortedByDescending { to.shells[it].bounds.center.x }
        List(maxOf(sources.size, targets.size)) { Carrier(sources.getOrNull(it), targets.getOrNull(it)) }
    }

    fun frame(progress: Float): HeaderActionFrame {
        val p = progress.coerceIn(0f, 1f)
        if (p <= 0f) return from
        if (p >= 1f) return to
        val t = p * HeaderActionMotion.DurationMillis
        val replacesSurfaces = from.shells.isNotEmpty() && to.shells.isNotEmpty()
        val pulse = if (replacesSurfaces) HeaderActionMotion.pulse(t) else 0f
        val geometryProgress = HeaderActionMotion.geometry(t)
        val merging = from.shells.size > to.shells.size && to.shells.isNotEmpty()
        val splitting = from.shells.isNotEmpty() && from.shells.size < to.shells.size
        val bases = mutableListOf<Rect>()
        val changing = carriers.map { c ->
            c.source == null || c.target == null || from.shells[c.source].bounds != to.shells[c.target].bounds ||
                from.glyphs.filter { it.shell == c.source }.map { it.identity } != to.glyphs.filter { it.shell == c.target }.map { it.identity }
        }
        carriers.forEach { c ->
            val a = c.source?.let { from.shells[it].bounds }
            val b = c.target?.let { to.shells[it].bounds }
            bases += when {
                a != null && b != null -> mix(a, b,
                    if (merging) HeaderActionMotion.geometry(t - 30f) else geometryProgress)
                b == null && merging -> {
                    // 未延续的左组变短、变薄，最终完全收进右侧主体；不复制完整目标矩形。
                    val center = bases.first().center
                    mix(requireNotNull(a), Rect(center.x - .001f, center.y - .001f, center.x + .001f, center.y + .001f),
                        HeaderActionMotion.smooth(t, 0f, 140f))
                }
                a == null && splitting -> {
                    val target = requireNotNull(b)
                    val main = bases.first()
                    val growth = HeaderActionMotion.smooth(t, 20f, 200f)
                    val right = mix(main.left + main.height * .12f, target.right, HeaderActionMotion.smooth(t, 170f, 270f))
                    val height = maxOf(.002f, target.height * growth)
                    val width = maxOf(height, target.width * growth)
                    Rect(right - width, target.center.y - height / 2f, right, target.center.y + height / 2f)
                }
                else -> requireNotNull(a ?: b)
            }
        }
        val shells = carriers.mapIndexed { i, c ->
            val rect = bases[i]
            val amount = if (changing[i]) pulse else 0f
            val dy = rect.height * .10f * amount
            val dx = (rect.height * .10f + (rect.width - rect.height) * .03f) * amount
            HeaderActionShell(Rect(rect.left - dx, rect.top - dy, rect.right + dx, rect.bottom + dy),
                c.source?.let { from.shells[it].keys }.orEmpty() + c.target?.let { to.shells[it].keys }.orEmpty())
        }
        fun project(x: Float, a: Rect, b: Rect): Float = b.left + (x - a.left) / a.width.coerceAtLeast(1f) * b.width
        fun shellAt(x: Float) = shells.indices.minByOrNull { abs(shells[it].bounds.center.x - x) } ?: 0
        fun foregroundX(x: Float, index: Int) = bases[index].center.x +
            (x - bases[index].center.x) * (1f + if (changing[index]) .20f * pulse else 0f)
        val oldById = from.glyphs.associateBy { it.identity }
        val targetIds = to.glyphs.map { it.identity }.toSet()
        val glyphs = buildList {
            to.glyphs.forEach { target ->
                val old = oldById[target.identity]
                val index = carriers.indexOfFirst { it.target == target.shell }
                val baseX = if (old != null) mix(old.x, target.x, geometryProgress)
                    else project(target.x, to.shells[target.shell].bounds, bases[index])
                val x = foregroundX(baseX, index)
                add(target.copy(x = x, shell = shellAt(x),
                    width = mix(old?.width ?: target.width, target.width, geometryProgress),
                    alpha = if (old != null) mix(old.alpha, 1f, geometryProgress) else HeaderActionMotion.smooth(t, 80f, 145f),
                    blur = if (old != null) old.blur * (1f - HeaderActionMotion.smooth(t, 0f, 210f)) else 8f * (1f - HeaderActionMotion.smooth(t, 90f, 215f)),
                    scale = mix(old?.scale ?: 1f, 1f, geometryProgress) * (1f + if (changing[index]) .20f * pulse else 0f)))
            }
            from.glyphs.filter { it.identity !in targetIds }.forEach { old ->
                val index = carriers.indexOfFirst { it.source == old.shell }
                val x = foregroundX(project(old.x, from.shells[old.shell].bounds, bases[index]), index)
                add(old.copy(x = x, shell = shellAt(x),
                    alpha = old.alpha * (1f - HeaderActionMotion.smooth(t, 65f, 140f)),
                    blur = mix(old.blur, 8f, HeaderActionMotion.smooth(t, 20f, 85f)),
                    scale = old.scale * (1f + if (changing[index]) .20f * pulse else 0f)))
            }
        }.filter { it.alpha > 0.001f }
        val alpha = when {
            to.shells.isEmpty() -> from.shellAlpha * (1f - HeaderActionMotion.smooth(t, 40f, 180f))
            from.shells.isEmpty() -> HeaderActionMotion.smooth(t, 0f, 120f)
            else -> mix(from.shellAlpha, 1f, geometryProgress)
        }
        val connection = if (replacesSurfaces) HeaderActionMotion.smooth(t, 0f, 50f) * (1f - HeaderActionMotion.smooth(t, 200f, 280f)) else 0f
        val width = when {
            from.shells.isEmpty() -> to.width
            to.shells.isEmpty() -> from.width
            else -> mix(from.width, to.width, geometryProgress)
        }
        return HeaderActionFrame(width, shells, glyphs, alpha,
            if (to.shells.isEmpty()) mix(from.shellBlur, 8f, HeaderActionMotion.smooth(t, 40f, 180f))
            else if (from.shells.isEmpty()) 8f * (1f - HeaderActionMotion.smooth(t, 0f, 160f)) else from.shellBlur * (1f - geometryProgress),
            if (shells.size > 1) from.connection * (1f - geometryProgress) + connection * (1f - from.connection) else 0f)
    }
}

internal class HeaderActionTransitionState(initial: HeaderActionFrame, source: Any, signature: Any) {
    var source by mutableStateOf(source)
        private set
    var signature by mutableStateOf(signature)
        private set
    var morph by mutableStateOf<HeaderActionMorph?>(null)
        private set
    var startProgress = 0f
        private set
    var lastFrame = initial
    fun retarget(target: HeaderActionFrame, source: Any, signature: Any, progress: Float, immediate: Boolean) {
        this.source = source
        this.signature = signature
        startProgress = progress.coerceIn(0f, 1f)
        morph = if (immediate || startProgress >= 0.999f) null else HeaderActionMorph(lastFrame, target)
        if (morph == null) lastFrame = target
    }
    fun progress(navigationProgress: Float) = ((navigationProgress - startProgress) / (1f - startProgress).coerceAtLeast(0.001f)).coerceIn(0f, 1f)
    fun finish(target: HeaderActionFrame) {
        morph = null
        lastFrame = target
    }
}
