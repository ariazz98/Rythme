package com.aria.rythme.ui.component

import com.aria.rythme.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TopBarActionTest {
    @Test
    fun callbackIdentityDoesNotChangeVisualIdentity() {
        val first = Action(
            actionKey = "more",
            iconRes = R.drawable.ic_more,
            onClick = { error("first") }
        )
        val second = Action(
            actionKey = "more",
            iconRes = R.drawable.ic_more,
            onClick = { error("second") }
        )

        assertEquals(listOf(first).visualKey(), listOf(second).visualKey())
    }

    @Test
    fun stableKeyAndVisualsDefineIdentity() {
        val more = Action(actionKey = "more", iconRes = R.drawable.ic_more)
        val add = Action(actionKey = "add", iconRes = R.drawable.ic_add)

        assertNotEquals(listOf(more).visualKey(), listOf(add).visualKey())
    }
}
