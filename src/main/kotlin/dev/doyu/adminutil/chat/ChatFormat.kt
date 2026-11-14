package dev.doyu.adminutil.chat

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

/**
 * 채팅 서식. `&` 색 코드와 `<nick>`(SMPUtil 닉네임, 없으면 진짜 이름), `<name>`(진짜 이름), `<msg>`(내용)를 쓴다.
 * 색 코드는 서식에서만 해석한다. 플레이어가 친 내용은 그대로 두어, 일반 플레이어가 `&c`로 채팅을 꾸밀 수 없다.
 */
class ChatFormat private constructor(val template: String) {
    private val styled = LegacyComponentSerializer.legacyAmpersand().deserialize(template)

    fun render(nick: String, name: String, message: Component): Component =
        styled
            .replaceText(placeholder(NICK, Component.text(nick)))
            .replaceText(placeholder(NAME, Component.text(name)))
            .replaceText(placeholder(MSG, message))

    private fun placeholder(token: String, value: Component) =
        TextReplacementConfig.builder().matchLiteral(token).replacement(value).build()

    companion object {
        const val NICK = "<nick>"
        const val NAME = "<name>"
        const val MSG = "<msg>"
        const val DEFAULT_TEMPLATE = "&e<nick>&r: <msg>"

        val DEFAULT = requireNotNull(parse(DEFAULT_TEMPLATE))

        /** `<msg>`가 없으면 채팅 내용이 사라지므로 받지 않는다. */
        fun parse(template: String): ChatFormat? = if (template.contains(MSG)) ChatFormat(template) else null
    }
}
