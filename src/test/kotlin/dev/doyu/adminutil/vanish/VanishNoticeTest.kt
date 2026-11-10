package dev.doyu.adminutil.vanish

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class VanishNoticeTest {
    @Test
    fun `notices parse from english and korean names`() {
        assertEquals(VanishNotice.FAKE, VanishNotice.parse("FAKE"))
        assertEquals(VanishNotice.ANNOUNCE, VanishNotice.parse("공지"))
        assertEquals(VanishNotice.SILENT, VanishNotice.parse("silent"))
        assertNull(VanishNotice.parse("none"))
        assertNull(VanishNotice.parse(null))
    }
}
