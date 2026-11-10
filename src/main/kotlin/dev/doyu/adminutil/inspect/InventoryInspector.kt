package dev.doyu.adminutil.inspect

import dev.doyu.adminutil.lookup.KnownPlayers
import dev.doyu.adminutil.lookup.NameMatch
import dev.doyu.adminutil.lookup.NamedPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Server
import org.bukkit.entity.Player

/**
 * 접속 중인 플레이어는 실제 인벤토리를 그대로 연다(실시간, 수정 가능).
 * 오프라인 플레이어는 저장 파일의 사본을 읽기 전용으로 연다. 고친 내용은 접속 시 서버가 덮어써 사라지거나 아이템이 복제되기 때문이다.
 */
class InventoryInspector(private val server: Server, private val players: KnownPlayers, private val offline: OfflinePlayerItems) {
    fun openInventory(admin: Player, query: String) {
        val target = find(admin, query) ?: return
        val online = server.getPlayer(target.id)
        if (online != null) {
            admin.openInventory(online.inventory)
            return
        }
        val items = readOffline(admin, target) ?: return
        admin.openInventory(ReadOnlyView(54, Component.text("${target.name} 인벤토리 (읽기 전용)"), server, ReadOnlyView.inventoryLayout(items)).inventory)
    }

    fun openEnderChest(admin: Player, query: String) {
        val target = find(admin, query) ?: return
        val online = server.getPlayer(target.id)
        if (online != null) {
            admin.openInventory(online.enderChest)
            return
        }
        val items = readOffline(admin, target) ?: return
        admin.openInventory(ReadOnlyView(27, Component.text("${target.name} 엔더상자 (읽기 전용)"), server, items.enderChest).inventory)
    }

    private fun find(admin: Player, query: String): NamedPlayer? = when (val match = players.find(query)) {
        is NameMatch.Found -> match.player
        NameMatch.NotFound -> {
            admin.sendMessage(Component.text("${query}님은 이 서버에 접속한 적이 없습니다.", NamedTextColor.RED))
            null
        }
        is NameMatch.Ambiguous -> {
            val names = match.candidates.joinToString { it.name }
            admin.sendMessage(Component.text("'$query'에 해당하는 플레이어가 여럿입니다: $names. 진짜 이름으로 다시 입력하세요.", NamedTextColor.RED))
            null
        }
    }

    private fun readOffline(admin: Player, target: NamedPlayer): OfflineItems? {
        val items = try {
            offline.read(target.id)
        } catch (e: Exception) {
            admin.sendMessage(Component.text("${target.name}님의 저장 파일을 읽지 못했습니다: ${e.message}", NamedTextColor.RED))
            return null
        }
        if (items == null) admin.sendMessage(Component.text("${target.name}님의 저장 파일이 없습니다.", NamedTextColor.RED))
        if (items != null && items.unreadable > 0) {
            admin.sendMessage(Component.text("아이템 ${items.unreadable}개를 읽지 못해 화면에서 빠졌습니다. 서버 로그를 확인하세요.", NamedTextColor.YELLOW))
        }
        return items
    }
}
