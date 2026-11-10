package dev.doyu.adminutil.command

import dev.doyu.adminutil.vanish.Vanish
import dev.doyu.adminutil.vanish.VanishNotice
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

/** `/vanish`(별칭 `/v`, `/투명`), `/vanish notice <fake|announce|silent>`. */
class VanishCommand(private val vanish: Vanish) : BasicCommand {
    override fun execute(source: CommandSourceStack, args: Array<String>) {
        val player = source.sender as? Player
        if (player == null) {
            source.sender.sendMessage(adminMessage("플레이어만 쓸 수 있습니다.", NamedTextColor.RED))
            return
        }
        if (args.isEmpty()) {
            vanish.toggle(player)
            return
        }
        val notice = VanishNotice.parse(args.getOrNull(1))
        if (args[0].lowercase() !in NOTICE_WORDS || notice == null) {
            player.sendMessage(adminMessage("사용법: /vanish [notice <${VanishNotice.entries.joinToString("|") { it.id }}>]", NamedTextColor.GRAY))
            return
        }
        vanish.setNotice(player, notice)
        player.sendMessage(adminMessage("투명화 알림 방식: ${notice.label}", NamedTextColor.GREEN))
    }

    override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> {
        val last = args.lastOrNull().orEmpty()
        return when {
            args.size <= 1 -> NOTICE_WORDS.filter { it.startsWith(last, ignoreCase = true) }
            args.size == 2 && args[0].lowercase() in NOTICE_WORDS -> VanishNotice.entries.map { it.id }.filter { it.startsWith(last, ignoreCase = true) }
            else -> emptyList()
        }
    }

    override fun permission(): String = VANISH_PERMISSION

    private companion object {
        val NOTICE_WORDS = listOf("notice", "알림")
    }
}
