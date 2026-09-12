package com.aria.rythme.ui.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Density
import com.aria.rythme.core.music.data.model.LyricLine
import com.aria.rythme.core.music.data.model.LyricWord
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.ceil

internal data class TimedLyricGlyph(val start: Int, val end: Int, val wordIndex: Int?)

/** 保持原文字形和分段时间；不为普通 LRC 或缺失时间的词均分伪造时间戳。 */
internal fun timedLyricGlyphs(line: LyricLine): List<TimedLyricGlyph> {
    val words = line.words.orEmpty()
    if (words.isEmpty()) return emptyList()
    var cursor = 0
    val ranges = words.map { word ->
        val start = line.text.indexOf(word.text, cursor)
        if (word.text.isEmpty() || start < cursor || word.endTimeMs < word.startTimeMs) return emptyList()
        if (line.text.substring(cursor, start).any { !it.isWhitespace() }) return emptyList()
        cursor = start + word.text.length
        start until cursor
    }
    if (line.text.substring(cursor).any { !it.isWhitespace() }) return emptyList()
    val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(line.text) }
    val result = mutableListOf<TimedLyricGlyph>()
    var start = iterator.first()
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        val word = ranges.indexOfFirst { start in it && end - 1 in it }.takeIf { it >= 0 }
        result += TimedLyricGlyph(start, end, word)
        start = end
        end = iterator.next()
    }
    return result
}

internal fun lyricWordProgress(word: LyricWord, positionMs: Long): Float = when {
    positionMs < word.startTimeMs -> 0f
    word.endTimeMs <= word.startTimeMs -> 1f // 只有起点：真实时间点切换，不猜测结束时间。
    else -> ((positionMs - word.startTimeMs).toFloat() / (word.endTimeMs - word.startTimeMs)).coerceIn(0f, 1f)
}

internal fun lyricGlyphFill(wordProgress: Float, advanceBefore: Float, advance: Float, total: Float): Float =
    if (advance <= 0f) 0f else ((wordProgress * total - advanceBefore) / advance).coerceIn(0f, 1f)

/** Text 负责同一份布局／语义；绘制时按原布局的字形范围提亮、抬升，不重新拆行排版。 */
@Composable
internal fun LyricsKaraokeText(
    line: LyricLine,
    alpha: Float,
    karaokeWeight: Float,
    positionMs: () -> Long,
    referenceScale: Float,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    var layout by remember(line.text) { mutableStateOf<TextLayoutResult?>(null) }
    val glyphs = remember(line) { timedLyricGlyphs(line) }
    val placements = remember(layout, glyphs) {
        val textLayout = layout
        if (textLayout == null) emptyList() else glyphs.map { glyph ->
            val box = textLayout.getBoundingBox(glyph.start)
            Triple(textLayout.getPathForRange(glyph.start, glyph.end), box,
                textLayout.getBidiRunDirection(glyph.start) == ResolvedTextDirection.Rtl)
        }
    }
    val widths = remember(placements, glyphs) {
        val totals = FloatArray(line.words.orEmpty().size)
        val before = FloatArray(glyphs.size)
        placements.forEachIndexed { index, (_, box, _) ->
            glyphs[index].wordIndex?.let { word ->
                before[index] = totals[word]
                totals[word] += box.width
            }
        }
        totals to before
    }
    Text(
        text = line.text,
        color = Color.White.copy(alpha = alpha),
        style = style.copy(textMotion = TextMotion.Animated),
        onTextLayout = { layout = it },
        modifier = modifier.drawWithCache {
            val textLayout = layout
            if (textLayout == null || glyphs.isEmpty()) {
                return@drawWithCache onDrawWithContent { drawContent() }
            }
            // 字形先在固定坐标栅格化，再移动离屏层。直接 translate + drawText
            // 在真机仍会把纵向位置取整，即使 TextMotion.Animated 也会每 1px 跳动。
            val padding = 2f
            // 不把当前可变 DrawScope 当作离屏 record 的 Density；嵌套录制时会自引用。
            val recordingDensity = Density(density, fontScale)
            val recordingDirection = layoutDirection
            val layers = placements.map {
                obtainGraphicsLayer().apply { compositingStrategy = CompositingStrategy.Offscreen }
            }
            val recordedFills = FloatArray(placements.size) { Float.NaN }
            onDrawWithContent {
                if (karaokeWeight <= .001f) {
                    drawContent()
                    return@onDrawWithContent
                }
                val now = positionMs()
                placements.forEachIndexed { index, (path, box, rtl) ->
                    val glyph = glyphs[index]
                    val fill = glyph.wordIndex?.let { wordIndex ->
                        lyricGlyphFill(lyricWordProgress(line.words!![wordIndex], now),
                            widths.second[index], box.width, widths.first[wordIndex])
                    } ?: 0f
                    val lift = LyricsPresentationMotion.GlyphLiftDp.dp.toPx() * referenceScale *
                        LyricsPresentationMotion.Easing.transform(fill) * karaokeWeight
                    val layer = layers[index]
                    if (recordedFills[index] != fill) {
                        layer.record(density = recordingDensity, layoutDirection = recordingDirection, size = IntSize(
                            ceil(box.width + padding * 2).toInt().coerceAtLeast(1),
                            ceil(box.height + padding * 2).toInt().coerceAtLeast(1)
                        )) {
                            translate(left = padding - box.left, top = padding - box.top) {
                                clipPath(path) {
                                    val baseAlpha = alpha +
                                        (alpha.coerceAtMost(LyricsPresentationMotion.UnsungAlpha) - alpha) * karaokeWeight
                                    drawText(textLayout, color = Color.White.copy(alpha = baseAlpha))
                                    if (fill > 0f) {
                                        if (fill >= 1f) {
                                            drawText(textLayout, color = Color.White.copy(alpha = alpha * karaokeWeight))
                                        } else {
                                            val edge = if (rtl) box.right - box.width * fill else box.left + box.width * fill
                                            val feather = (2.dp.toPx() * referenceScale).coerceAtMost(box.width / 2f)
                                            val bright = Color.White.copy(alpha = alpha * karaokeWeight)
                                            val brush = Brush.horizontalGradient(
                                                if (rtl) listOf(Color.Transparent, bright) else listOf(bright, Color.Transparent),
                                                startX = edge - feather, endX = edge + feather
                                            )
                                            drawText(textLayout, brush = brush)
                                        }
                                    }
                                }
                            }
                        }
                        recordedFills[index] = fill
                    }
                    translate(left = box.left - padding, top = box.top - padding - lift) { drawLayer(layer) }
                }
            }
        }
    )
}
