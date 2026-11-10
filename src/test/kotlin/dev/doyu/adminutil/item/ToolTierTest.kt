package dev.doyu.adminutil.item

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ToolTierTest {
    @Test
    fun `the default set is wooden with a stone axe`() {
        val tools = ToolTier.parse(null)!!.toolIds()
        assertEquals(
            mapOf(5 to "minecraft:wooden_shovel", 6 to "minecraft:wooden_hoe", 7 to "minecraft:wooden_pickaxe", 8 to "minecraft:stone_axe"),
            tools,
        )
    }

    @Test
    fun `other sets use one material for every tool`() {
        assertEquals(
            listOf("minecraft:netherite_shovel", "minecraft:netherite_hoe", "minecraft:netherite_pickaxe", "minecraft:netherite_axe"),
            ToolTier.NETHERITE.toolIds().values.toList(),
        )
    }

    @Test
    fun `tiers parse from english and korean names`() {
        assertEquals(ToolTier.DIAMOND, ToolTier.parse("Diamond"))
        assertEquals(ToolTier.DIAMOND, ToolTier.parse("다이아"))
        assertEquals(ToolTier.GOLDEN, ToolTier.parse("gold"))
        assertEquals(ToolTier.IRON, ToolTier.parse("철"))
        assertNull(ToolTier.parse("wood"))
    }
}
