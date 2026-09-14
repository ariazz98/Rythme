package com.aria.rythme.ui.component

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.aria.rythme.ui.theme.rythmeColors
import com.kyant.backdrop.Backdrop
import com.kyant.capsule.ContinuousCapsule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ProfileEditor(
    name: String,
    avatar: String?,
    backdrop: Backdrop,
    onSave: suspend (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var draftName by remember { mutableStateOf(name.takeUnless { it == "未设置昵称" }.orEmpty()) }
    var draftAvatar by remember { mutableStateOf(avatar) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.rythmeColors
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { draftAvatar = uri.toString(); error = null }
    }
    GlassAlertDialog(backdrop = backdrop, onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("编辑个人资料") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderAvatarContent(Action.Avatar("profile-preview", url = draftAvatar, name = draftName, contentDescription = "头像预览"),
                    referenceScale = 1.5f,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .background(Brush.verticalGradient(listOf(HeaderAvatarTop, HeaderAvatarBottom)), CircleShape))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) { GlassDialogButton(primary = false, enabled = !saving, onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }) { Text("选择头像") } }
                    if (draftAvatar != null) Box(Modifier.weight(1f)) {
                        GlassDialogButton(primary = false, enabled = !saving, onClick = { draftAvatar = null }) { Text("恢复默认") }
                    }
                }
                TextField(value = draftName, onValueChange = { if (it.length <= 30) { draftName = it; error = null } },
                    modifier = Modifier.fillMaxWidth(), enabled = !saving, singleLine = true,
                    shape = ContinuousCapsule, placeholder = { Text("输入昵称") },
                    colors = TextFieldDefaults.colors(focusedContainerColor = colors.textColor.copy(alpha = .06f),
                        unfocusedContainerColor = colors.textColor.copy(alpha = .06f),
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        cursorColor = colors.primary, focusedTextColor = colors.textColor, unfocusedTextColor = colors.textColor))
                error?.let { Text(it, color = colors.primary) }
            }
        },
        dismissButton = { GlassDialogButton(primary = false, enabled = !saving, onClick = onDismiss) { Text("取消") } },
        confirmButton = { GlassDialogButton(enabled = !saving, onClick = {
            saving = true
            scope.launch {
                try { onSave(draftName.trim().ifEmpty { "未设置昵称" }, draftAvatar); onDismiss() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (failure: Exception) { error = "保存失败，请重试或重新选择图片。" }
                finally { saving = false }
            }
        }) { Text(if (saving) "保存中…" else "保存") } })
}
