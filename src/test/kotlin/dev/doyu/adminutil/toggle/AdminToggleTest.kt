package dev.doyu.adminutil.toggle

import kotlin.test.Test
import kotlin.test.assertEquals

class AdminToggleTest {
    @Test
    fun `a toggle that is on turns off`() {
        assertEquals(ToggleSwitch(AdminToggle.FLY, nowOn = false, turnedOff = emptySet()), switchToggle(setOf(AdminToggle.FLY), AdminToggle.FLY))
    }

    @Test
    fun `invulnerable turns nodamage off`() {
        val result = switchToggle(setOf(AdminToggle.NODAMAGE, AdminToggle.FLY), AdminToggle.INVULNERABLE)
        assertEquals(ToggleSwitch(AdminToggle.INVULNERABLE, nowOn = true, turnedOff = setOf(AdminToggle.NODAMAGE)), result)
    }

    @Test
    fun `nodamage turns invulnerable off`() {
        val result = switchToggle(setOf(AdminToggle.INVULNERABLE), AdminToggle.NODAMAGE)
        assertEquals(setOf(AdminToggle.INVULNERABLE), result.turnedOff)
    }

    @Test
    fun `other toggles leave the rest alone`() {
        val result = switchToggle(setOf(AdminToggle.INVULNERABLE), AdminToggle.GLASS)
        assertEquals(ToggleSwitch(AdminToggle.GLASS, nowOn = true, turnedOff = emptySet()), result)
    }

    @Test
    fun `toggle ids are unique`() {
        assertEquals(AdminToggle.entries.size, AdminToggle.entries.map { it.id }.toSet().size)
    }
}
