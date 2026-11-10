package dev.doyu.adminutil.toy

import dev.doyu.adminutil.item.AdminItem
import dev.doyu.adminutil.item.AdminItems
import org.bukkit.NamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityResurrectEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin
import java.util.UUID

/** 장미칼: 태그가 붙은 칼로 때리면 즉사시키고 토템 부활도 무시한다. */
class RoseKnife(private val plugin: Plugin, private val items: AdminItems) : Listener {
    private val playersKey = NamespacedKey("adminutil", "knife_players")

    /** 이번 틱에 장미칼에 맞은 대상. 토템 부활은 같은 틱 안에서 판정되므로 다음 틱에 비운다. */
    private val struck = mutableSetOf<UUID>()

    fun hurtsPlayers(admin: Player): Boolean = admin.persistentDataContainer.get(playersKey, PersistentDataType.BOOLEAN) == true

    fun setHurtsPlayers(admin: Player, on: Boolean) {
        admin.persistentDataContainer.set(playersKey, PersistentDataType.BOOLEAN, on)
    }

    /** 다른 플러그인(구역 보호, 무적 토글 등)이 취소한 공격은 건드리지 않도록 가장 늦게 본다. */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onHit(event: EntityDamageByEntityEvent) {
        val attacker = event.damager as? Player ?: return
        if (items.kindOf(attacker.inventory.itemInMainHand) != AdminItem.KNIFE) return
        val victim = event.entity as? LivingEntity ?: return
        if (victim is Player && !hurtsPlayers(attacker)) {
            event.isCancelled = true
            return
        }
        struck += victim.uniqueId
        plugin.server.scheduler.runTask(plugin) { _ -> struck.remove(victim.uniqueId) }
        // 큰 피해를 주면 그만큼 상대 갑옷 내구도가 깎이므로, 옛 장미칼처럼 피해는 0으로 두고 체력을 바로 0으로 만든다.
        event.damage = 0.0
        victim.health = 0.0
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    fun onResurrect(event: EntityResurrectEvent) {
        if (struck.remove(event.entity.uniqueId)) event.isCancelled = true
    }
}
