package dev.doyu.adminutil.toy

import dev.doyu.adminutil.toggle.AdminToggle
import dev.doyu.adminutil.toggle.ToggleStore
import net.kyori.adventure.text.Component
import org.bukkit.Tag
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

/** 순보: 켜 두면 검을 들고 우클릭할 때 바라보는 플레이어의 등 뒤로 순간이동한다. */
class Shunpo(private val toggles: ToggleStore) : Listener {
    @EventHandler
    fun onRightClick(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) return
        val player = event.player
        val item = event.item ?: return
        if (!Tag.ITEMS_SWORDS.isTagged(item.type) || !toggles.isOn(player, AdminToggle.SHUNPO)) return

        val hit = player.world.rayTraceEntities(player.eyeLocation, player.eyeLocation.direction, RANGE, 1.0) { entity ->
            entity is Player && entity != player && player.canSee(entity)
        }
        val target = hit?.hitEntity as? Player ?: return
        val facing = target.location.direction.setY(0)
        if (facing.lengthSquared() == 0.0) return
        // 위아래를 보고 있어도 땅속이나 공중으로 가지 않게 수평 방향으로만 물러선다. 시선은 상대와 같은 쪽을 본다.
        val behind = target.location.clone().subtract(facing.normalize().multiply(DISTANCE_BEHIND))
        player.teleport(behind)
        player.sendActionBar(Component.text("${target.name}님의 뒤로 이동했습니다."))
    }

    private companion object {
        const val RANGE = 15.0
        const val DISTANCE_BEHIND = 1.5
    }
}
