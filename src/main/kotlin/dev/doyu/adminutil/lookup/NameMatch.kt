package dev.doyu.adminutil.lookup

import java.util.UUID

data class NamedPlayer(val id: UUID, val name: String, val nickname: String?)

sealed interface NameMatch {
    data class Found(val player: NamedPlayer) : NameMatch
    data object NotFound : NameMatch

    /** 닉네임은 SMPUtil이 고유하게 관리한다고 가정할 수 없으므로, 겹치면 고르지 않고 알린다. */
    data class Ambiguous(val candidates: List<NamedPlayer>) : NameMatch
}

/**
 * 진짜 이름이 닉네임보다 먼저다. 누군가 다른 사람의 진짜 이름을 닉네임으로 쓰더라도 진짜 주인이 찾아져야 하기 때문이다.
 * 진짜 이름은 마인크래프트가 대소문자를 구분하지 않으므로 대소문자를 무시하고, 닉네임은 적힌 그대로 비교한다.
 */
fun matchName(query: String, candidates: List<NamedPlayer>): NameMatch {
    val byName = candidates.filter { it.name.equals(query, ignoreCase = true) }.distinctBy { it.id }
    if (byName.size == 1) return NameMatch.Found(byName.single())
    if (byName.size > 1) return NameMatch.Ambiguous(byName)
    val byNickname = candidates.filter { it.nickname == query }.distinctBy { it.id }
    return when (byNickname.size) {
        0 -> NameMatch.NotFound
        1 -> NameMatch.Found(byNickname.single())
        else -> NameMatch.Ambiguous(byNickname)
    }
}
