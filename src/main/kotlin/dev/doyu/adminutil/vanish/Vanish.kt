package dev.doyu.adminutil.vanish

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

/**
 * 투명화는 Bukkit hidePlayer로 한다. 탭 목록에서도 빠지고, SMPUtil이 canSee로 걸러 내므로 연동 코드가 따로 필요 없다(DESIGN 8장).
 * 투명화 상태와 알림 방식은 플레이어 PDC에 둬서 재시작·재접속해도 남는다.
 */
class Vanish(private val plugin: Plugin, private val defaultNotice: VanishNotice) : Listener {
    private val vanishedKey = NamespacedKey("adminutil", "vanished")
    private val noticeKey = NamespacedKey("adminutil", "vanish_notice")

    fun isVanished(player: Player): Boolean = player.persistentDataContainer.get(vanishedKey, PersistentDataType.BOOLEAN) == true

    fun noticeOf(player: Player): VanishNotice =
        VanishNotice.parse(player.persistentDataContainer.get(noticeKey, PersistentDataType.STRING)) ?: defaultNotice

    fun setNotice(player: Player, notice: VanishNotice) {
        player.persistentDataContainer.set(noticeKey, PersistentDataType.STRING, notice.id)
    }

    /** 바뀐 뒤 투명화 상태를 돌려준다. */
    fun toggle(player: Player): Boolean {
        val vanishing = !isVanished(player)
        if (vanishing) {
            player.persistentDataContainer.set(vanishedKey, PersistentDataType.BOOLEAN, true)
            hide(player)
        } else {
            player.persistentDataContainer.remove(vanishedKey)
            reveal(player)
        }
        announce(player, vanishing)
        return vanishing
    }

    /** 플러그인이 서버 실행 중에 다시 켜졌을 때, 이미 접속한 사람들의 숨김을 되살린다. */
    fun restoreOnline() {
        for (player in plugin.server.onlinePlayers) if (isVanished(player)) hide(player)
    }

    fun showStatusBar() {
        for (player in plugin.server.onlinePlayers) {
            if (isVanished(player)) player.sendActionBar(Component.text("투명화 상태", NamedTextColor.RED))
        }
    }

    private fun hide(player: Player) {
        for (other in plugin.server.onlinePlayers) {
            if (other == player) continue
            if (isVanished(other)) {
                other.showPlayer(plugin, player)
                player.showPlayer(plugin, other)
            } else {
                other.hidePlayer(plugin, player)
            }
        }
    }

    private fun reveal(player: Player) {
        for (other in plugin.server.onlinePlayers) {
            if (other == player) continue
            other.showPlayer(plugin, player)
            if (isVanished(other)) player.hidePlayer(plugin, other)
        }
    }

    private fun announce(player: Player, vanishing: Boolean) {
        val state = if (vanishing) "투명화" else "투명화 해제"
        when (noticeOf(player)) {
            VanishNotice.FAKE -> {
                val key = if (vanishing) "multiplayer.player.left" else "multiplayer.player.joined"
                plugin.server.broadcast(Component.translatable(key, NamedTextColor.YELLOW, player.displayName()))
            }
            VanishNotice.ANNOUNCE -> plugin.server.broadcast(
                Component.text("[Vanish] ", NamedTextColor.GRAY).append(player.displayName()).append(Component.text(" $state", NamedTextColor.WHITE)),
            )
            VanishNotice.SILENT -> {}
        }
        player.sendMessage(Component.text("${state}했습니다. (알림: ${noticeOf(player).label})", NamedTextColor.GRAY))
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (isVanished(player)) {
            event.joinMessage(null)
            hide(player)
            player.sendMessage(Component.text("투명화 상태로 접속했습니다.", NamedTextColor.GRAY))
            return
        }
        for (other in plugin.server.onlinePlayers) if (other != player && isVanished(other)) player.hidePlayer(plugin, other)
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onQuit(event: PlayerQuitEvent) {
        if (isVanished(event.player)) event.quitMessage(null)
    }

    @EventHandler(ignoreCancelled = true)
    fun onPickupItem(event: EntityPickupItemEvent) {
        val player = event.entity as? Player ?: return
        if (isVanished(player)) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onPickupExperience(event: PlayerPickupExperienceEvent) {
        if (isVanished(event.player)) event.isCancelled = true
    }

    @EventHandler(ignoreCancelled = true)
    fun onTarget(event: EntityTargetLivingEntityEvent) {
        val target = event.target as? Player ?: return
        if (isVanished(target)) event.isCancelled = true
    }
}
