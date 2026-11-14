package dev.doyu.adminutil.command

import dev.doyu.adminutil.chat.ChatFormat
import dev.doyu.adminutil.chat.ChatFormatStore
import dev.doyu.adminutil.inspect.InventoryInspector
import dev.doyu.adminutil.item.AdminItem
import dev.doyu.adminutil.item.AdminItems
import dev.doyu.adminutil.item.KitDispenser
import dev.doyu.adminutil.item.ToolTier
import dev.doyu.adminutil.lookup.KnownPlayers
import dev.doyu.adminutil.toggle.AdminToggle
import dev.doyu.adminutil.toggle.AdminToggles
import dev.doyu.adminutil.toy.RoseKnife
import dev.doyu.adminutil.vanish.Vanish
import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.logging.Level
import java.util.logging.Logger

/** `/ac`(별칭 `/admin`, `/관리`). 옛 `/ac` 습관을 유지하려고 관리자 기능을 한 명령어 아래에 모은다. */
class AdminCommand(
    private val toggles: AdminToggles,
    private val items: AdminItems,
    private val dispenser: KitDispenser,
    private val knife: RoseKnife,
    private val vanish: Vanish,
    private val inspector: InventoryInspector,
    private val players: KnownPlayers,
    private val chatFormats: ChatFormatStore,
    private val logger: Logger,
) : BasicCommand {
    override fun execute(source: CommandSourceStack, args: Array<String>) {
        val sender = source.sender
        val subcommand = Subcommand.parse(args.firstOrNull())
        if (subcommand == null || !sender.hasPermission(subcommand.permission)) {
            sender.sendMessage(adminMessage("사용법: /ac <${allowed(sender).joinToString("|") { it.id }}>", NamedTextColor.GRAY))
            return
        }
        if (sender !is Player) {
            sender.sendMessage(adminMessage("플레이어만 쓸 수 있습니다.", NamedTextColor.RED))
            return
        }
        when (subcommand) {
            Subcommand.FLY, Subcommand.INVULNERABLE, Subcommand.NODAMAGE, Subcommand.UNSTARVE,
            Subcommand.PICKUP, Subcommand.GLASS, Subcommand.SHUNPO -> flip(sender, requireNotNull(subcommand.toggle))
            Subcommand.LIST -> showList(sender)
            Subcommand.TOOLS -> tools(sender, args.getOrNull(1))
            Subcommand.KNIFE -> knife(sender, args.drop(1))
            Subcommand.LIGHTNING -> lightning(sender)
            Subcommand.INV -> inspect(sender, args.getOrNull(1), subcommand, inspector::openInventory)
            Subcommand.ENDER -> inspect(sender, args.getOrNull(1), subcommand, inspector::openEnderChest)
            Subcommand.CHAT -> chat(sender, args.drop(1))
        }
    }

    override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> {
        val sender = source.sender
        val subcommand = Subcommand.parse(args.firstOrNull())
        val last = args.lastOrNull().orEmpty()
        return when {
            args.size <= 1 -> allowed(sender).flatMap { listOf(it.id, it.korean) }.filter { it.startsWith(last, ignoreCase = true) }
            subcommand == null || !sender.hasPermission(subcommand.permission) -> emptyList()
            args.size == 2 && (subcommand == Subcommand.INV || subcommand == Subcommand.ENDER) -> players.suggestions(sender, last)
            args.size == 2 && subcommand == Subcommand.TOOLS -> ToolTier.entries.map { it.id }.filter { it.startsWith(last, ignoreCase = true) }
            args.size == 2 && subcommand == Subcommand.CHAT -> listOf("reset").filter { it.startsWith(last, ignoreCase = true) }
            args.size == 2 && subcommand == Subcommand.KNIFE -> listOf("players").filter { it.startsWith(last, ignoreCase = true) }
            args.size == 3 && subcommand == Subcommand.KNIFE -> listOf("on", "off").filter { it.startsWith(last, ignoreCase = true) }
            else -> emptyList()
        }
    }

    override fun canUse(sender: CommandSender): Boolean = allowed(sender).isNotEmpty()

    private fun allowed(sender: CommandSender) = Subcommand.entries.filter { sender.hasPermission(it.permission) }

    private fun flip(player: Player, toggle: AdminToggle) {
        val result = toggles.flip(player, toggle)
        for (off in result.turnedOff) player.sendMessage(adminMessage("${off.label}: ").append(onOff(false)).append(Component.text(" (${toggle.label}와 함께 켤 수 없음)", NamedTextColor.GRAY)))
        player.sendMessage(adminMessage("${toggle.label}: ").append(onOff(result.nowOn)))
    }

    private fun showList(player: Player) {
        player.sendMessage(adminMessage("토글 상태", NamedTextColor.YELLOW))
        for (toggle in AdminToggle.entries) player.sendMessage(Component.text(" ${toggle.label}: ").append(onOff(toggles.isOn(player, toggle))))
        player.sendMessage(Component.text(" 관리자 도구: ").append(onOff(dispenser.isGiven(player, AdminItem.TOOLS))))
        player.sendMessage(Component.text(" 장미칼: ").append(onOff(dispenser.isGiven(player, AdminItem.KNIFE))).append(Component.text(" (플레이어 피해 ")).append(onOff(knife.hurtsPlayers(player))).append(Component.text(")")))
        player.sendMessage(Component.text(" 번개 막대: ").append(onOff(dispenser.isGiven(player, AdminItem.LIGHTNING))))
        player.sendMessage(Component.text(" 투명화: ").append(onOff(vanish.isVanished(player))))
    }

    private fun tools(player: Player, tierText: String?) {
        if (dispenser.isGiven(player, AdminItem.TOOLS)) {
            recall(player, AdminItem.TOOLS, "관리자 도구")
            return
        }
        val tier = ToolTier.parse(tierText)
        if (tier == null) {
            player.sendMessage(adminMessage("재질은 ${ToolTier.entries.joinToString("|") { it.id }} 중 하나입니다.", NamedTextColor.RED))
            return
        }
        dispenser.give(player, AdminItem.TOOLS, items.tools(tier))
        player.sendMessage(adminMessage("${tier.label} 관리자 도구를 지급했습니다. 다시 입력하면 회수합니다.", NamedTextColor.GREEN))
    }

    private fun knife(player: Player, options: List<String>) {
        if (options.isNotEmpty()) {
            val on = when (options.getOrNull(1)?.lowercase()) {
                "on", "켜기" -> true
                "off", "끄기" -> false
                else -> null
            }
            if (options[0].lowercase() != "players" || on == null) {
                player.sendMessage(adminMessage("사용법: /ac knife [players on|off]", NamedTextColor.GRAY))
                return
            }
            knife.setHurtsPlayers(player, on)
            player.sendMessage(adminMessage("장미칼 플레이어 피해: ").append(onOff(on)))
            return
        }
        if (dispenser.isGiven(player, AdminItem.KNIFE)) {
            recall(player, AdminItem.KNIFE, "장미칼")
            return
        }
        dispenser.give(player, AdminItem.KNIFE, mapOf(KNIFE_SLOT to items.knife()))
        player.sendMessage(adminMessage("장미칼을 지급했습니다. 플레이어 피해: ").append(onOff(knife.hurtsPlayers(player))))
    }

    private fun lightning(player: Player) {
        if (dispenser.isGiven(player, AdminItem.LIGHTNING)) {
            recall(player, AdminItem.LIGHTNING, "번개 막대")
            return
        }
        dispenser.give(player, AdminItem.LIGHTNING, mapOf(LIGHTNING_SLOT to items.lightningRod()))
        player.sendMessage(adminMessage("번개 막대를 지급했습니다. 들고 우클릭하면 바라보는 곳에 번개가 칩니다.", NamedTextColor.GREEN))
    }

    private fun recall(player: Player, kit: AdminItem, label: String) {
        try {
            dispenser.recall(player, kit)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Could not restore the items ${player.name} had before $label", e)
            player.sendMessage(adminMessage("원래 아이템을 읽지 못해 회수하지 않았습니다. 서버 로그를 확인하세요.", NamedTextColor.RED))
            return
        }
        player.sendMessage(adminMessage("${label}을(를) 회수하고 원래 아이템을 돌려놓았습니다.", NamedTextColor.YELLOW))
    }

    private fun chat(player: Player, words: List<String>) {
        if (words.isEmpty()) {
            player.sendMessage(adminMessage("채팅 서식: ${chatFormats.current.template}", NamedTextColor.YELLOW))
            player.sendMessage(adminMessage("사용법: /ac chat <서식>|reset  (${ChatFormat.NICK} ${ChatFormat.NAME} ${ChatFormat.MSG}, &색코드)", NamedTextColor.GRAY))
            return
        }
        if (words.singleOrNull().equals("reset", ignoreCase = true)) {
            chatFormats.reset()
            player.sendMessage(adminMessage("채팅 서식을 기본값(${ChatFormat.DEFAULT_TEMPLATE})으로 되돌렸습니다.", NamedTextColor.YELLOW))
            return
        }
        val format = ChatFormat.parse(words.joinToString(" "))
        if (format == null) {
            player.sendMessage(adminMessage("서식에는 ${ChatFormat.MSG}가 있어야 합니다.", NamedTextColor.RED))
            return
        }
        chatFormats.set(format)
        player.sendMessage(adminMessage("채팅 서식을 바꿨습니다: ").append(format.render(player.name, player.name, Component.text("미리보기"))))
    }

    private fun inspect(player: Player, query: String?, subcommand: Subcommand, open: (Player, String) -> Unit) {
        if (query == null) {
            player.sendMessage(adminMessage("사용법: /ac ${subcommand.id} <플레이어>", NamedTextColor.GRAY))
            return
        }
        open(player, query)
    }

    private companion object {
        /** 핫바 4번 칸(DESIGN 4장). */
        const val KNIFE_SLOT = 3

        /** 핫바 3번 칸. 도구(6~9번)·장미칼(4번)과 겹치지 않는 자리. */
        const val LIGHTNING_SLOT = 2
    }
}
