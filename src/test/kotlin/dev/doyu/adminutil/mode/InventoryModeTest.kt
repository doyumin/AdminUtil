package dev.doyu.adminutil.mode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InventoryModeTest {
    @Test
    fun `modes parse from english and korean names`() {
        assertEquals(InventoryMode.ADMIN, InventoryMode.parse("ADMIN"))
        assertEquals(InventoryMode.PLAYER, InventoryMode.parse("플레이어"))
        assertNull(InventoryMode.parse("creative"))
        assertNull(InventoryMode.parse(null))
    }

    @Test
    fun `other is the opposite mode`() {
        for (mode in InventoryMode.entries) assertEquals(mode, mode.other.other)
        assertEquals(InventoryMode.ADMIN, InventoryMode.PLAYER.other)
    }
}
