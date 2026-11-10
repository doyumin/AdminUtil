package dev.doyu.adminutil.item

import kotlin.test.Test
import kotlin.test.assertEquals

class RestorationTest {
    @Test
    fun `items go back to their slots when those are free`() {
        val plan = planRestoration(mapOf(5 to "torch", 7 to "bread")) { true }
        assertEquals(Restoration(mapOf(5 to "torch", 7 to "bread"), emptyList()), plan)
    }

    @Test
    fun `items whose slot is taken become leftovers instead of overwriting`() {
        val plan = planRestoration(mapOf(5 to "torch", 7 to "bread")) { it != 7 }
        assertEquals(Restoration(mapOf(5 to "torch"), listOf("bread")), plan)
    }
}
