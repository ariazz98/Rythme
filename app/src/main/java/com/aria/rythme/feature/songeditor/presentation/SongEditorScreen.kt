package com.aria.rythme.feature.songeditor.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.ui.theme.rythmeColors

@Composable
fun SongEditorContent(
    song: Song,
    scrollState: ScrollState = rememberScrollState(),
    onDismiss: () -> Unit
) {
    var title by remember(song.id) { mutableStateOf(song.title) }
    var artist by remember(song.id) { mutableStateOf(song.artist) }
    var album by remember(song.id) { mutableStateOf(song.album) }
    var genre by remember(song.id) { mutableStateOf(song.genre) }
    var composer by remember(song.id) { mutableStateOf(song.composer) }
    var albumArtist by remember(song.id) { mutableStateOf(song.albumArtist) }
    var trackNumber by remember(song.id) { mutableStateOf(song.trackNumber.toString()) }
    var discNumber by remember(song.id) { mutableStateOf(song.discNumber.toString()) }
    var year by remember(song.id) { mutableStateOf(if (song.year > 0) song.year.toString() else "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 21.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "编辑歌曲信息",
            fontSize = 20.sp,
            color = MaterialTheme.rythmeColors.textColor
        )

        Spacer(modifier = Modifier.height(16.dp))

        EditField(label = "标题", value = title, onValueChange = { title = it })
        EditField(label = "艺术家", value = artist, onValueChange = { artist = it })
        EditField(label = "专辑", value = album, onValueChange = { album = it })
        EditField(label = "专辑艺术家", value = albumArtist, onValueChange = { albumArtist = it })
        EditField(label = "流派", value = genre, onValueChange = { genre = it })
        EditField(label = "作曲者", value = composer, onValueChange = { composer = it })
        EditField(label = "音轨号", value = trackNumber, onValueChange = { trackNumber = it })
        EditField(label = "碟片号", value = discNumber, onValueChange = { discNumber = it })
        EditField(label = "年份", value = year, onValueChange = { year = it })

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                // TODO: 保存歌曲信息
                onDismiss()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.rythmeColors.primary
            )
        ) {
            Text("保存", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.rythmeColors.primary,
            focusedLabelColor = MaterialTheme.rythmeColors.primary,
            unfocusedBorderColor = MaterialTheme.rythmeColors.weakColor,
            focusedTextColor = MaterialTheme.rythmeColors.textColor,
            unfocusedTextColor = MaterialTheme.rythmeColors.textColor
        )
    )
}
