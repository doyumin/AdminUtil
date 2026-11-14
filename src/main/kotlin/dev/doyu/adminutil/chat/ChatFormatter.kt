package dev.doyu.adminutil.chat

import dev.doyu.adminutil.lookup.nickname
import io.papermc.paper.chat.ChatRenderer
import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

/** 채팅 이벤트는 비동기라, 렌더러는 저장소의 현재 서식만 읽고 Bukkit 상태를 바꾸지 않는다. */
class ChatFormatter(private val store: ChatFormatStore) : Listener {
    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val format = store.current
        event.renderer(
            ChatRenderer.viewerUnaware { source, _, message ->
                format.render(source.nickname() ?: source.name, source.name, message)
            },
        )
    }
}
