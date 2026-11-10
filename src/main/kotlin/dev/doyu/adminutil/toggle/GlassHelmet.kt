package dev.doyu.adminutil.toggle

import dev.doyu.adminutil.item.AdminItem
import dev.doyu.adminutil.item.AdminItems
import dev.doyu.adminutil.item.KitDispenser
import dev.doyu.adminutil.item.receive
import org.bukkit.entity.Player

class GlassHelmet(private val items: AdminItems, private val dispenser: KitDispenser) {
    fun isWorn(player: Player): Boolean = items.kindOf(player.inventory.helmet) == AdminItem.GLASS

    /** 원래 머리 칸 아이템은 인벤토리로 옮기고 넘치면 발밑(DESIGN 3장). */
    fun wear(player: Player) {
        if (isWorn(player)) return
        dispenser.removeAll(player, AdminItem.GLASS)
        val inventory = player.inventory
        val previous = inventory.helmet
        inventory.setHelmet(items.glass())
        if (!previous.isEmpty) player.receive(listOf(previous))
    }

    fun takeOff(player: Player) {
        dispenser.removeAll(player, AdminItem.GLASS)
    }
}
