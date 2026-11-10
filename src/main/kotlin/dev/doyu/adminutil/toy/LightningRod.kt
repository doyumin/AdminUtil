package dev.doyu.adminutil.toy

import dev.doyu.adminutil.item.AdminItem
import dev.doyu.adminutil.item.AdminItems
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/** 번개 막대: 태그가 붙은 막대기만 반응한다. 옛 `/stick`은 아무 막대기에나 반응했다. */
class LightningRod(private val items: AdminItems) : Listener {
    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        if (items.kindOf(event.item) != AdminItem.LIGHTNING) return
        val block = event.player.getTargetBlockExact(RANGE) ?: return
        event.isCancelled = true
        block.world.strikeLightning(block.location.add(0.5, 1.0, 0.5))
    }

    private companion object {
        const val RANGE = 128
    }
}
