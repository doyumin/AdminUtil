package dev.doyu.adminutil.item

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack

data class Restoration<T>(val placed: Map<Int, T>, val leftovers: List<T>)

/** 원래 칸이 비어 있으면 그 칸에, 그 사이 다른 아이템이 들어와 있으면 그 아이템을 덮지 않도록 남는 것으로 돌린다. */
fun <T> planRestoration(displaced: Map<Int, T>, isFree: (Int) -> Boolean): Restoration<T> {
    val (free, taken) = displaced.entries.partition { isFree(it.key) }
    return Restoration(free.associate { it.key to it.value }, taken.map { it.value })
}

/** 넘치는 것은 발밑에 떨군다. 원래 아이템은 플레이어 것이므로 사라지는 것보다 바닥에 있는 편이 낫다. */
fun Player.receive(items: List<ItemStack>) {
    if (items.isEmpty()) return
    for (leftover in inventory.addItem(*items.toTypedArray()).values) world.dropItemNaturally(location, leftover)
}
