package com.aria.rythme.ui.component

import com.aria.rythme.R
import org.junit.Assert.*
import org.junit.Test

class SongContextMenuConfigsTest {
    @Test fun quickActionsAndRowsHaveTheApprovedOrder() {
        val menu=buildSongContextMenuConfigs({}, {})
        val quick=menu.first() as MenuConfig.Group
        assertEquals(listOf(R.string.song_menu_favorite,R.string.song_menu_share),quick.items.map { it.titleRes })
        assertEquals(listOf(R.string.song_menu_add_to_playlist,R.string.song_menu_play_next,
            R.string.song_menu_play_later,R.string.song_menu_go_to_album,R.string.song_menu_info,
            R.string.song_edit),menu.filterIsInstance<MenuConfig.Item>().map { it.titleRes })
        assertEquals(4,menu.count { it==MenuConfig.Separator })
        assertTrue(menu.last() is MenuConfig.Item)
    }

    @Test fun plannedActionsDoNotPretendToChangeStateOrLaunchTheEditor() {
        var dismisses=0
        var edits=0
        val menu=buildSongContextMenuConfigs({dismisses++},{edits++})
        val planned=(menu.first() as MenuConfig.Group).items+menu.filterIsInstance<MenuConfig.Item>().dropLast(1)
        planned.forEach { assertFalse(it.isChecked);it.onClick() }
        assertEquals(7,dismisses)
        assertEquals(0,edits)
    }

    @Test fun existingEditorStillOpensAfterDismissal() {
        val calls=mutableListOf<String>()
        val menu=buildSongContextMenuConfigs({calls+="dismiss"},{calls+="edit"})
        (menu.last() as MenuConfig.Item).onClick()
        assertEquals(listOf("dismiss","edit"),calls)
    }
}
