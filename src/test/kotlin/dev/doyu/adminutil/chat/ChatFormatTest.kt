package dev.doyu.adminutil.chat

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ChatFormatTest {
    private fun plain(component: Component) = PlainTextComponentSerializer.plainText().serialize(component)

    @Test
    fun `the default format is the nickname then the message`() {
        val rendered = ChatFormat.DEFAULT.render("미누", "Minu", Component.text("안녕"))
        assertEquals("미누: 안녕", plain(rendered))
    }

    @Test
    fun `only the nickname is colored in the default format`() {
        val rendered = ChatFormat.DEFAULT.render("미누", "Minu", Component.text("안녕"))
        val colors = rendered.children().associate { plain(it) to it.color() }
        assertEquals(NamedTextColor.YELLOW, colors["미누"])
        assertNull(colors["안녕"])
    }

    @Test
    fun `name and nick are separate placeholders`() {
        val format = requireNotNull(ChatFormat.parse("<nick>(<name>) <msg>"))
        assertEquals("미누(Minu) hi", plain(format.render("미누", "Minu", Component.text("hi"))))
    }

    @Test
    fun `color codes typed by a player are not interpreted`() {
        val rendered = ChatFormat.DEFAULT.render("a", "a", Component.text("&cred"))
        assertEquals("a: &cred", plain(rendered))
    }

    @Test
    fun `a format without the message is rejected`() {
        assertNull(ChatFormat.parse("&e<nick>"))
    }
}
