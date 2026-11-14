package dev.doyu.adminutil

import dev.doyu.adminutil.chat.ChatFormatStore
import dev.doyu.adminutil.chat.ChatFormatter
import dev.doyu.adminutil.command.AdminCommand
import dev.doyu.adminutil.command.VanishCommand
import dev.doyu.adminutil.config.readSettings
import dev.doyu.adminutil.inspect.InventoryInspector
import dev.doyu.adminutil.inspect.OfflinePlayerItems
import dev.doyu.adminutil.inspect.PlayerDataFiles
import dev.doyu.adminutil.inspect.ReadOnlyGuard
import dev.doyu.adminutil.item.AdminItemGuard
import dev.doyu.adminutil.item.AdminItems
import dev.doyu.adminutil.item.KitDispenser
import dev.doyu.adminutil.lookup.KnownPlayers
import dev.doyu.adminutil.toggle.AdminToggles
import dev.doyu.adminutil.toggle.GlassHelmet
import dev.doyu.adminutil.toggle.ToggleStore
import dev.doyu.adminutil.toy.LightningRod
import dev.doyu.adminutil.toy.RoseKnife
import dev.doyu.adminutil.toy.Shunpo
import dev.doyu.adminutil.vanish.Vanish
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.plugin.java.JavaPlugin

class AdminUtilPlugin : JavaPlugin() {
    override fun onEnable() {
        saveDefaultConfig()
        val settings = readSettings(config)

        val items = AdminItems()
        val dispenser = KitDispenser(items)
        val store = ToggleStore()
        val toggles = AdminToggles(this, store, GlassHelmet(items, dispenser), items)
        val knife = RoseKnife(this, items)
        val vanish = Vanish(this, settings.defaultVanishNotice)
        val files = PlayerDataFiles(server)
        val players = KnownPlayers(server, files, logger)
        val inspector = InventoryInspector(server, players, OfflinePlayerItems(files, logger))

        val chatFormats = ChatFormatStore(dataFolder.resolve("chat-format.yml"))

        val events = server.pluginManager
        events.registerEvents(toggles, this)
        events.registerEvents(AdminItemGuard(items), this)
        events.registerEvents(knife, this)
        events.registerEvents(Shunpo(store), this)
        events.registerEvents(LightningRod(items), this)
        events.registerEvents(vanish, this)
        events.registerEvents(ReadOnlyGuard(), this)
        events.registerEvents(ChatFormatter(chatFormats), this)

        // 서버 실행 중에 플러그인만 다시 켜진 경우, 이미 접속한 사람들에게 저장된 상태를 다시 적용한다.
        vanish.restoreOnline()
        server.onlinePlayers.forEach(toggles::reapply)
        server.scheduler.runTaskTimer(this, { _ -> toggles.keepGlassOn() }, TICKS_PER_SECOND, TICKS_PER_SECOND)
        // 액션바는 몇 초 뒤 사라지므로 계속 다시 보낸다.
        server.scheduler.runTaskTimer(this, { _ -> vanish.showStatusBar() }, TICKS_PER_SECOND, TICKS_PER_SECOND)

        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { event ->
            val commands = event.registrar()
            commands.register(
                "ac",
                "관리자 도구를 씁니다.",
                listOf("admin", "관리"),
                AdminCommand(toggles, items, dispenser, knife, vanish, inspector, players, chatFormats, logger),
            )
            commands.register("vanish", "투명화를 켜고 끕니다.", listOf("v", "투명"), VanishCommand(vanish))
        }
    }

    private companion object {
        const val TICKS_PER_SECOND = 20L
    }
}
