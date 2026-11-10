package dev.doyu.adminutil.item

import org.bukkit.entity.Allay
import org.bukkit.entity.ItemFrame
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerArmorStandManipulateEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEntityEvent

/** 관리자 아이템이 다른 사람 손에 들어가지 않게, 떨구기·상자에 넣기·사망 드롭을 막는다(DESIGN 4장). */
class AdminItemGuard(private val items: AdminItems) : Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDrop(event: PlayerDropItemEvent) {
        if (items.kindOf(event.itemDrop.itemStack) != null) event.isCancelled = true
    }

    /**
     * 자기 인벤토리만 열려 있을 때(위쪽이 2×2 조합칸)는 정리하게 둔다.
     * 다른 창이 열려 있으면 관리자 아이템이 걸린 클릭을 모두 막는다. shift·숫자키·F키처럼 위쪽으로 옮기는 경로를 하나씩 따지면 빠뜨리기 쉽다.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onClick(event: InventoryClickEvent) {
        if (event.view.topInventory.type == InventoryType.CRAFTING) return
        val player = event.whoClicked
        val involved = listOfNotNull(
            event.currentItem,
            event.cursor,
            if (event.hotbarButton >= 0) player.inventory.getItem(event.hotbarButton) else null,
            if (event.click == ClickType.SWAP_OFFHAND) player.inventory.itemInOffHand else null,
        )
        if (involved.any { items.kindOf(it) != null }) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDrag(event: InventoryDragEvent) {
        val top = event.view.topInventory
        if (top.type == InventoryType.CRAFTING || items.kindOf(event.oldCursor) == null) return
        if (event.rawSlots.any { it < top.size }) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onPlaceIntoEntity(event: PlayerInteractEntityEvent) {
        if (event.rightClicked !is ItemFrame && event.rightClicked !is Allay) return
        if (items.kindOf(event.player.inventory.getItem(event.hand)) != null) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun onArmorStand(event: PlayerArmorStandManipulateEvent) {
        if (items.kindOf(event.playerItem) != null) event.isCancelled = true
    }

    /** 드롭에서 빼기만 하면 아이템이 사라지므로, itemsToKeep에 넣어 리스폰 후에도 인벤토리에 남긴다. */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onDeath(event: PlayerDeathEvent) {
        val kept = event.drops.filter { items.kindOf(it) != null }
        if (kept.isEmpty()) return
        event.drops.removeAll(kept)
        event.itemsToKeep.addAll(kept)
    }
}
