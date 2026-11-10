package dev.doyu.adminutil.inspect

import org.bukkit.inventory.ItemStack
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

data class OfflineItems(
    val inventory: Map<Int, ItemStack>,
    val enderChest: Map<Int, ItemStack>,
    val equipment: Map<String, ItemStack>,
    /** 읽지 못한 아이템 수. 0이 아니면 화면이 실제와 다를 수 있음을 알려야 한다. */
    val unreadable: Int,
)

class OfflinePlayerItems(private val files: PlayerDataFiles, private val logger: Logger) {
    fun read(player: UUID): OfflineItems? {
        val stored = storedPlayerItemsOf(files.read(player) ?: return null)
        var unreadable = 0
        fun convert(item: NbtTag.CompoundTag): ItemStack? = try {
            ItemStack.deserializeBytes(serializedItemBytes(item, stored.dataVersion))
        } catch (e: Exception) {
            unreadable++
            logger.log(Level.WARNING, "Could not read an item of $player", e)
            null
        }
        return OfflineItems(
            inventory = stored.inventory.mapNotNull { (slot, item) -> convert(item)?.let { slot to it } }.toMap(),
            enderChest = stored.enderChest.mapNotNull { (slot, item) -> convert(item)?.let { slot to it } }.toMap(),
            equipment = stored.equipment.mapNotNull { (slot, item) -> convert(item)?.let { slot to it } }.toMap(),
            unreadable = unreadable,
        )
    }
}
