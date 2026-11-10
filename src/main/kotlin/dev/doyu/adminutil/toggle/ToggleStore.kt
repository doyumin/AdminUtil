package dev.doyu.adminutil.toggle

import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

/** 토글은 플레이어 PDC에 둔다. 메모리에만 두면 재시작할 때 풀려, 비행 중이던 관리자가 재접속하면 추락한다. */
class ToggleStore {
    private val keys = AdminToggle.entries.associateWith { NamespacedKey("adminutil", it.id) }

    fun isOn(player: Player, toggle: AdminToggle): Boolean =
        player.persistentDataContainer.get(keys.getValue(toggle), PersistentDataType.BOOLEAN) == true

    fun onToggles(player: Player): Set<AdminToggle> = AdminToggle.entries.filter { isOn(player, it) }.toSet()

    fun set(player: Player, toggle: AdminToggle, on: Boolean) {
        if (on) {
            player.persistentDataContainer.set(keys.getValue(toggle), PersistentDataType.BOOLEAN, true)
        } else {
            player.persistentDataContainer.remove(keys.getValue(toggle))
        }
    }
}
