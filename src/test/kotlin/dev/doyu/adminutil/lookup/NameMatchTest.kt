package dev.doyu.adminutil.lookup

import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class NameMatchTest {
    private val doyun = NamedPlayer(UUID.randomUUID(), "Doyun", "도윤")
    private val steve = NamedPlayer(UUID.randomUUID(), "Steve", null)
    private val impostor = NamedPlayer(UUID.randomUUID(), "Alex", "Steve")

    @Test
    fun `real names match ignoring case`() {
        assertEquals(NameMatch.Found(doyun), matchName("doyun", listOf(doyun, steve)))
    }

    @Test
    fun `nicknames match when no real name does`() {
        assertEquals(NameMatch.Found(doyun), matchName("도윤", listOf(doyun, steve)))
    }

    @Test
    fun `a real name wins over someone else's nickname`() {
        assertEquals(NameMatch.Found(steve), matchName("Steve", listOf(impostor, steve)))
    }

    @Test
    fun `shared nicknames are reported instead of guessed`() {
        val other = NamedPlayer(UUID.randomUUID(), "Other", "도윤")
        assertEquals(NameMatch.Ambiguous(listOf(doyun, other)), matchName("도윤", listOf(doyun, other)))
    }

    @Test
    fun `unknown names are not found`() {
        assertEquals(NameMatch.NotFound, matchName("nobody", listOf(doyun, steve)))
    }
}
