package dev.doyu.adminutil.mode

import dev.doyu.adminutil.item.AdminItem
import dev.doyu.adminutil.item.KitDispenser
import dev.doyu.adminutil.toggle.AdminToggle
import dev.doyu.adminutil.toggle.AdminToggles
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

sealed interface SwitchResult {
    data class Switched(val to: InventoryMode, val firstTime: Boolean) : SwitchResult
    data class AlreadyIn(val mode: InventoryMode) : SwitchResult

    /** 지급된 관리자 아이템이 남아 있다. 보관하면 회수할 때 원래 아이템이 복제되거나 사라진다. */
    data class Blocked(val reasons: List<String>) : SwitchResult

    /** 보관된 인벤토리를 읽지 못했다. 아무것도 바꾸지 않았다. */
    data object Unreadable : SwitchResult
}

/**
 * 플레이어 인벤토리와 관리자 인벤토리를 오가게 한다. 갑옷·왼손 칸까지 41칸 전체를 바꾼다.
 * 보관 중인 쪽은 플레이어 PDC에 직렬화해 둬서, 재시작하거나 접속이 끊겨도 남는다.
 */
class InventorySwitcher(private val dispenser: KitDispenser, private val toggles: AdminToggles) {
    fun modeOf(player: Player): InventoryMode =
        InventoryMode.parse(player.persistentDataContainer.get(MODE_KEY, PersistentDataType.STRING)) ?: InventoryMode.PLAYER

    fun switch(player: Player, target: InventoryMode?): SwitchResult {
        val current = modeOf(player)
        val to = target ?: current.other
        if (to == current) return SwitchResult.AlreadyIn(current)
        val blockers = blockers(player)
        if (blockers.isNotEmpty()) return SwitchResult.Blocked(blockers)

        val pdc = player.persistentDataContainer
        // 어느 단계에서 실패해도 현재 인벤토리와 보관된 것을 잃지 않도록 순서를 지킨다.
        // 1) 가져올 쪽을 먼저 읽는다. 읽지 못하면 아무것도 건드리지 않는다.
        val stored = pdc.get(stashKey(to), PersistentDataType.BYTE_ARRAY)
        val incoming: Array<ItemStack> = try {
            if (stored == null) Array(INVENTORY_SIZE) { ItemStack.empty() } else ItemStack.deserializeItemsFromBytes(stored)
        } catch (e: Exception) {
            return SwitchResult.Unreadable
        }

        // 2) 커서·제작대에 든 아이템이 인벤토리로 돌아오도록 창을 닫은 뒤, 지금 인벤토리를 보관하고 다시 읽어 일치하는지 확인한다.
        player.closeInventory()
        val outgoing = ItemStack.serializeItemsAsBytes(player.inventory.contents.map { it ?: ItemStack.empty() })
        pdc.set(stashKey(current), PersistentDataType.BYTE_ARRAY, outgoing)
        if (!outgoing.contentEquals(pdc.get(stashKey(current), PersistentDataType.BYTE_ARRAY))) return SwitchResult.Unreadable

        // 3) 보관이 확인된 뒤에만 인벤토리를 바꾸고, 마지막에 모드를 기록한다.
        player.inventory.setContents(incoming.copyOf(player.inventory.contents.size))
        player.updateInventory()
        pdc.set(MODE_KEY, PersistentDataType.STRING, to.id)
        pdc.remove(stashKey(to))
        return SwitchResult.Switched(to, firstTime = stored == null)
    }

    private fun blockers(player: Player): List<String> = buildList {
        if (dispenser.isGiven(player, AdminItem.TOOLS)) add("관리자 도구(/ac tools)")
        if (dispenser.isGiven(player, AdminItem.KNIFE)) add("장미칼(/ac knife)")
        if (dispenser.isGiven(player, AdminItem.LIGHTNING)) add("번개 막대(/ac lightning)")
        if (toggles.isOn(player, AdminToggle.GLASS)) add("유리 헬멧(/ac glass)")
    }

    private fun stashKey(mode: InventoryMode) = NamespacedKey("adminutil", "stash_${mode.id}")

    private companion object {
        /** 본 칸 36 + 갑옷 4 + 왼손 1. */
        const val INVENTORY_SIZE = 41
        val MODE_KEY = NamespacedKey("adminutil", "inventory_mode")
    }
}
