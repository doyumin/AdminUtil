package dev.doyu.adminutil.toggle

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import dev.doyu.adminutil.item.AdminItem
import dev.doyu.adminutil.item.AdminItems
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityExhaustionEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerRespawnEvent
import org.bukkit.plugin.Plugin

/** 관리자 토글을 켜고 끄고, 켜진 토글의 효과를 낸다. 상태는 [ToggleStore](플레이어 PDC)에만 있다. */
class AdminToggles(
    private val plugin: Plugin,
    private val store: ToggleStore,
    private val glass: GlassHelmet,
    private val items: AdminItems,
) : Listener {
    fun isOn(player: Player, toggle: AdminToggle): Boolean = store.isOn(player, toggle)

    fun flip(player: Player, toggle: AdminToggle): ToggleSwitch {
        val result = switchToggle(store.onToggles(player), toggle)
        for (off in result.turnedOff) {
            store.set(player, off, false)
            turnedOff(player, off)
        }
        store.set(player, toggle, result.nowOn)
        if (result.nowOn) turnedOn(player, toggle) else turnedOff(player, toggle)
        return result
    }

    /** 접속·리스폰·게임 모드 변경은 비행 허용과 머리 칸을 서버가 다시 정하므로, 그 뒤에 저장된 토글을 다시 적용한다. */
    fun reapply(player: Player) {
        if (store.isOn(player, AdminToggle.FLY)) player.allowFlight = true
        if (store.isOn(player, AdminToggle.GLASS)) glass.wear(player)
        if (store.isOn(player, AdminToggle.UNSTARVE)) fillHunger(player)
    }

    /** `/clear`로 유리가 사라져도 다시 씌운다. 명령어 문자열을 가로채는 대신 상태를 보고 맞춰서, 다른 경로로 사라져도 같게 처리한다. */
    fun keepGlassOn() {
        for (player in plugin.server.onlinePlayers) {
            if (store.isOn(player, AdminToggle.GLASS) && !glass.isWorn(player)) glass.wear(player)
        }
    }

    private fun turnedOn(player: Player, toggle: AdminToggle) {
        when (toggle) {
            AdminToggle.FLY -> player.allowFlight = true
            AdminToggle.NODAMAGE -> player.health = player.getAttribute(Attribute.MAX_HEALTH)?.value ?: player.health
            AdminToggle.UNSTARVE -> fillHunger(player)
            AdminToggle.GLASS -> glass.wear(player)
            AdminToggle.INVULNERABLE, AdminToggle.PICKUP, AdminToggle.SHUNPO -> {}
        }
    }

    private fun turnedOff(player: Player, toggle: AdminToggle) {
        when (toggle) {
            // 크리에이티브·관전자는 원래 날 수 있으므로 건드리지 않는다.
            AdminToggle.FLY -> if (player.gameMode == GameMode.SURVIVAL || player.gameMode == GameMode.ADVENTURE) {
                player.isFlying = false
                player.allowFlight = false
            }
            AdminToggle.GLASS -> glass.takeOff(player)
            AdminToggle.INVULNERABLE, AdminToggle.NODAMAGE, AdminToggle.UNSTARVE, AdminToggle.PICKUP, AdminToggle.SHUNPO -> {}
        }
    }

    private fun fillHunger(player: Player) {
        player.foodLevel = MAX_FOOD
        player.saturation = MAX_FOOD.toFloat()
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) = reapply(event.player)

    @EventHandler
    fun onRespawn(event: PlayerRespawnEvent) = reapplyNextTick(event.player)

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    fun onGameModeChange(event: PlayerGameModeChangeEvent) = reapplyNextTick(event.player)

    /**
     * `/kill`(DamageCause.KILL)은 무적·노데미지여도 통하게 둔다. 관리자가 스스로를 처리할 방법이 막히면 안 된다(옛 KillBypassListener).
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        if (event.cause == EntityDamageEvent.DamageCause.KILL) return
        if (store.isOn(player, AdminToggle.INVULNERABLE)) {
            event.isCancelled = true
            return
        }
        // 피해량만 0으로 두어 넉백과 피격 효과는 남긴다.
        if (store.isOn(player, AdminToggle.NODAMAGE)) event.damage = 0.0
    }

    @EventHandler(ignoreCancelled = true)
    fun onFoodChange(event: FoodLevelChangeEvent) {
        val player = event.entity as? Player ?: return
        if (store.isOn(player, AdminToggle.UNSTARVE)) event.foodLevel = MAX_FOOD
    }

    @EventHandler(ignoreCancelled = true)
    fun onExhaustion(event: EntityExhaustionEvent) {
        val player = event.entity as? Player ?: return
        if (store.isOn(player, AdminToggle.UNSTARVE)) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        if (store.isOn(player, AdminToggle.PICKUP)) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onPickupExperience(event: PlayerPickupExperienceEvent) {
        if (store.isOn(event.player, AdminToggle.PICKUP)) event.isCancelled = true
    }

    /** 유리 헬멧은 벗을 수 없다. 벗겨도 [keepGlassOn]이 다시 씌우지만, 그 사이 유리를 집어 다른 데 둘 수 있다. */
    @EventHandler(priority = EventPriority.HIGHEST)
    fun onClickGlass(event: InventoryClickEvent) {
        if (items.kindOf(event.currentItem) == AdminItem.GLASS || items.kindOf(event.cursor) == AdminItem.GLASS) event.isCancelled = true
    }

    private fun reapplyNextTick(player: Player) {
        plugin.server.scheduler.runTask(plugin) { _ -> if (player.isOnline) reapply(player) }
    }

    private companion object {
        const val MAX_FOOD = 20
    }
}
