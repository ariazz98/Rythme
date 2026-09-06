package com.aria.rythme.ui.component

import com.aria.rythme.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TopBarActionTest {
    @Test
    fun callbackIdentityDoesNotChangeVisualIdentity() {
        val first = Action.Icon(
            actionKey = "more",
            iconRes = R.drawable.ic_more,
            onClick = { error("first") }
        )
        val second = Action.Icon(
            actionKey = "more",
            iconRes = R.drawable.ic_more,
            onClick = { error("second") }
        )

        assertEquals(listOf(first).visualKey(), listOf(second).visualKey())
    }

    @Test
    fun stableKeyAndVisualsDefineIdentity() {
        val more = Action.Icon(actionKey = "more", iconRes = R.drawable.ic_more)
        val add = Action.Icon(actionKey = "add", iconRes = R.drawable.ic_add)

        assertNotEquals(listOf(more).visualKey(), listOf(add).visualKey())
    }

    @Test
    fun activeStateChangesVisualIdentity() {
        val inactive = Action.Icon(
            actionKey = "star",
            iconRes = R.drawable.ic_star,
            isActive = false
        )
        val active = inactive.copy(isActive = true)

        assertNotEquals(listOf(inactive).visualKey(), listOf(active).visualKey())
        assertEquals(listOf(inactive).contentKey(), listOf(active).contentKey())
    }
}
