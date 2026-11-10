package dev.doyu.adminutil.item

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

/**
 * 관리자 아이템 세트를 정해진 칸에 지급하고 회수한다.
 * 밀려난 원래 아이템은 플레이어 PDC에 직렬화해 둔다. 옛 코드는 메모리에만 백업해서, 도구를 든 채 서버가 재시작하면 원래 아이템이 사라졌다.
 */
class KitDispenser(private val items: AdminItems) {
    fun isGiven(player: Player, kit: AdminItem): Boolean =
        player.persistentDataContainer.get(givenKey(kit), PersistentDataType.BOOLEAN) == true

    fun give(player: Player, kit: AdminItem, stacks: Map<Int, ItemStack>) {
        val inventory = player.inventory
        val pdc = player.persistentDataContainer
        val displaced = pdc.adapterContext.newPersistentDataContainer()
        val otherAdminItems = mutableListOf<ItemStack>()
        for ((slot, stack) in stacks) {
            val old = inventory.getItem(slot)
            if (old != null && !old.isEmpty) {
                // 다른 세트의 관리자 아이템까지 저장해 두면, 그 세트를 회수한 뒤 이 세트를 회수할 때 되살아나 복제된다.
                if (items.kindOf(old) == null) displaced.set(slotKey(slot), PersistentDataType.BYTE_ARRAY, old.serializeAsBytes()) else otherAdminItems += old
            }
            inventory.setItem(slot, stack)
        }
        pdc.set(displacedKey(kit), PersistentDataType.TAG_CONTAINER, displaced)
        pdc.set(givenKey(kit), PersistentDataType.BOOLEAN, true)
        // 넘치는 관리자 아이템은 버린다. 명령어로 다시 만들 수 있고, 바닥에 떨구면 다른 사람 손에 들어간다.
        inventory.addItem(*otherAdminItems.toTypedArray())
    }

    /** 다른 칸으로 옮겨 놓은 것까지 인벤토리 전체에서 지운 뒤 원래 아이템을 돌려놓는다. */
    fun recall(player: Player, kit: AdminItem) {
        val pdc = player.persistentDataContainer
        val displaced = pdc.get(displacedKey(kit), PersistentDataType.TAG_CONTAINER)
        // 지우기 전에 먼저 다 읽는다. 읽다가 실패하면 아무것도 바뀌지 않아야 저장된 원래 아이템을 잃지 않는다.
        val stored = displaced?.keys.orEmpty().mapNotNull { key ->
            val slot = key.key.removePrefix(SLOT_PREFIX).toIntOrNull() ?: return@mapNotNull null
            val bytes = displaced?.get(key, PersistentDataType.BYTE_ARRAY) ?: return@mapNotNull null
            slot to ItemStack.deserializeBytes(bytes)
        }.toMap()

        removeAll(player, kit)
        val inventory = player.inventory
        val plan = planRestoration(stored) { slot -> inventory.getItem(slot)?.isEmpty ?: true }
        for ((slot, item) in plan.placed) inventory.setItem(slot, item)
        player.receive(plan.leftovers)
        pdc.remove(displacedKey(kit))
        pdc.remove(givenKey(kit))
    }

    fun removeAll(player: Player, kind: AdminItem) {
        val inventory = player.inventory
        for (slot in 0 until inventory.size) {
            if (items.kindOf(inventory.getItem(slot)) == kind) inventory.setItem(slot, null)
        }
        if (items.kindOf(player.itemOnCursor) == kind) player.setItemOnCursor(null)
    }

    private fun givenKey(kit: AdminItem) = NamespacedKey("adminutil", "kit_${kit.id}")

    private fun displacedKey(kit: AdminItem) = NamespacedKey("adminutil", "displaced_${kit.id}")

    private fun slotKey(slot: Int) = NamespacedKey("adminutil", "$SLOT_PREFIX$slot")

    private companion object {
        const val SLOT_PREFIX = "slot_"
    }
}
