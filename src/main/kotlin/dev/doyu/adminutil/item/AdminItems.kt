package dev.doyu.adminutil.item

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

/** [id]는 아이템 PDC `adminutil:tool`의 값으로 저장된다. 바꾸면 이미 지급된 아이템을 알아보지 못한다. */
enum class AdminItem(val id: String) {
    TOOLS("tools"),
    KNIFE("knife"),
    LIGHTNING("lightning"),
    GLASS("glass"),
}

/**
 * 관리자 아이템은 PDC 태그로만 알아본다. 옛 코드는 표시 이름에 "장미칼"이 있으면 즉사 효과를 줘서,
 * 아무 검이나 모루로 이름만 바꾸면 누구나 즉사 검을 가질 수 있었다.
 */
class AdminItems {
    private val tagKey = NamespacedKey("adminutil", "tool")
    private val knifeSpeedKey = NamespacedKey("adminutil", "knife_attack_speed")

    fun kindOf(stack: ItemStack?): AdminItem? {
        if (stack == null || stack.isEmpty) return null
        val id = stack.persistentDataContainer.get(tagKey, PersistentDataType.STRING) ?: return null
        return AdminItem.entries.firstOrNull { it.id == id }
    }

    fun tools(tier: ToolTier): Map<Int, ItemStack> = tier.toolIds().mapValues { (_, id) ->
        val material = requireNotNull(Material.matchMaterial(id)) { "unknown tool $id" }
        tagged(ItemStack.of(material), AdminItem.TOOLS, Component.text("관리자 도구", NamedTextColor.AQUA)) { meta ->
            meta.isUnbreakable = true
            meta.addEnchant(Enchantment.EFFICIENCY, 10, true)
            meta.addEnchant(Enchantment.UNBREAKING, 10, true)
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        }
    }

    /** 옛 장미칼과 같이 철검에 공격 속도를 크게 올려 연타할 수 있게 한다. 즉사 피해는 [dev.doyu.adminutil.toy.RoseKnife]가 준다. */
    fun knife(): ItemStack = tagged(ItemStack.of(Material.IRON_SWORD), AdminItem.KNIFE, Component.text("장미칼", NamedTextColor.LIGHT_PURPLE)) { meta ->
        meta.isUnbreakable = true
        meta.lore(listOf(Component.text("장미처럼 날카로운 칼", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)))
        meta.addAttributeModifier(
            Attribute.ATTACK_SPEED,
            AttributeModifier(knifeSpeedKey, 1024.0, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND),
        )
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES)
    }

    fun lightningRod(): ItemStack = tagged(ItemStack.of(Material.STICK), AdminItem.LIGHTNING, Component.text("번개 막대", NamedTextColor.YELLOW))

    fun glass(): ItemStack = tagged(ItemStack.of(Material.GLASS), AdminItem.GLASS, Component.text("유리 헬멧", NamedTextColor.WHITE))

    private fun tagged(stack: ItemStack, kind: AdminItem, name: Component, configure: (ItemMeta) -> Unit = {}): ItemStack {
        stack.editMeta { meta ->
            meta.itemName(name)
            meta.persistentDataContainer.set(tagKey, PersistentDataType.STRING, kind.id)
            configure(meta)
        }
        return stack
    }
}
