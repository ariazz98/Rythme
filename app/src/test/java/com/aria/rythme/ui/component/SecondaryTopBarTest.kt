package com.aria.rythme.ui.component

import org.junit.Assert.*
import org.junit.Test

class SecondaryTopBarTest {
    private val filter = Action.Icon("filter", 1)
    private val more = Action.Icon("more", 2)

    @Test fun singleAndDoubleActionsStayInOneSurface() {
        val single = secondaryTopBar(filter)
        assertTrue(single.showBackButton)
        assertTrue(single.auxiliaryActions.isEmpty())
        assertEquals(listOf(filter), single.actions)
        val double = secondaryTopBar(filter, more)
        assertTrue(double.auxiliaryActions.isEmpty())
        assertEquals(listOf(filter, more), double.actions)
    }

    @Test fun specialActionThenFilterShareLeftSurfaceAndMoreIsIndependent() {
        var called = false
        val special = Action.Icon("add", 3, onClick = { called = true })
        val config = secondaryTopBar(special, filter, more)
        assertEquals(listOf(special, filter), config.auxiliaryActions)
        assertEquals(listOf(more), config.actions)
        assertSame(special, config.auxiliaryActions.first())
        config.auxiliaryActions.first().onClick?.invoke()
        assertTrue(called)
        assertNull(config.actions.first().onClick)
    }

    @Test fun noActionsStillHasBackButNoEmptySurface() {
        val config = secondaryTopBar()
        assertTrue(config.showBackButton)
        assertTrue(config.actions.isEmpty())
        assertTrue(config.auxiliaryActions.isEmpty())
    }
}
