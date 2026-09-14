package com.aria.rythme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalView
import com.aria.rythme.LocalBackdrop
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

/** 材质参考 AndroidLiquidGlass catalog/DialogContent；全窗口坐标让玻璃采样与页面对齐。 */
@Composable
internal fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: @Composable () -> Unit,
    backdrop: Backdrop = LocalBackdrop.current
) {
    val colors = MaterialTheme.rythmeColors
    val light = colors.surface.luminance() > .5f
    Dialog(onDismissRequest, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(if (light) .23f else .56f) }
        BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding().imePadding()
            .clickable(interactionSource = null, indication = null, onClick = onDismissRequest),
            contentAlignment = Alignment.Center) {
            val availableHeight = maxHeight - 48.dp
            Box(Modifier.padding(horizontal = 24.dp).widthIn(max = 420.dp).fillMaxWidth()
                .clickable(interactionSource = null, indication = null, onClick = {})) {
                GlassBackdropSurface(backdrop, shape = { ContinuousRoundedRectangle(32.dp) },
                    shadow = GlassMenuShadow,
                    effects = {
                        colorControls(brightness = 0f, saturation = 1.5f)
                        blur(if (light) 16.dp.toPx() else 8.dp.toPx())
                        glassLens(24.dp.toPx(), 48.dp.toPx())
                    }, onDrawSurface = {
                        drawRect(if (light) Color(0xFFFAFAFA).copy(alpha = .35f) else Color(0xFF121212).copy(alpha = .4f))
                    })
                Column(Modifier.heightIn(max = availableHeight).padding(20.dp)) {
                    Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
                        if (title != null) {
                            CompositionLocalProvider(LocalContentColor provides colors.textColor) {
                                ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold)) { title() }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        CompositionLocalProvider(LocalContentColor provides colors.textColor.copy(alpha = .68f)) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp)) { text() }
                        }
                    }
                    Spacer(Modifier.height(22.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (dismissButton != null) Box(Modifier.weight(1f)) { dismissButton() }
                        Box(Modifier.weight(1f)) { confirmButton() }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GlassDialogButton(onClick: () -> Unit, primary: Boolean = true, enabled: Boolean = true, content: @Composable RowScope.() -> Unit) {
    val colors = MaterialTheme.rythmeColors
    Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = ContinuousCapsule,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) colors.primary else colors.textColor.copy(alpha = .10f),
            contentColor = if (primary) Color.White else colors.textColor),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)) { content() }
    }
}
