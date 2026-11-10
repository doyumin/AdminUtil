package dev.doyu.adminutil.command

import dev.doyu.adminutil.toggle.AdminToggle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SubcommandTest {
    @Test
    fun `subcommands parse from english and korean names`() {
        assertEquals(Subcommand.FLY, Subcommand.parse("FLY"))
        assertEquals(Subcommand.ENDER, Subcommand.parse("엔더상자"))
        assertNull(Subcommand.parse("home"))
        assertNull(Subcommand.parse(null))
    }

    @Test
    fun `every toggle has a subcommand with the same name`() {
        for (toggle in AdminToggle.entries) assertEquals(toggle, Subcommand.parse(toggle.id)?.toggle)
    }

    @Test
    fun `permissions follow the adminutil naming`() {
        assertEquals("adminutil.knife", Subcommand.KNIFE.permission)
    }
}
