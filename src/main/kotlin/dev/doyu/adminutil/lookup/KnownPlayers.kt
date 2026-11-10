package dev.doyu.adminutil.lookup

import dev.doyu.adminutil.inspect.PlayerDataFiles
import dev.doyu.adminutil.inspect.storedPdcString
import org.bukkit.NamespacedKey
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType
import java.util.logging.Level
import java.util.logging.Logger

/**
 * SMPUtil과의 약속(DESIGN 6장, SMPUtil DESIGN 3장). SMPUtil 플러그인 이름이 바뀌어도 유지되도록 네임스페이스를 문자열로 고정한다.
 * 코드로 의존하지 않고 데이터로만 읽으므로 SMPUtil이 없으면 닉네임이 없을 뿐이다.
 */
val NICKNAME_KEY = NamespacedKey("smputil", "nickname")

fun Player.nickname(): String? = persistentDataContainer.get(NICKNAME_KEY, PersistentDataType.STRING)

/** 접속한 적 있는 플레이어만 찾는다. getOfflinePlayer(name)는 모르는 이름에도 가짜 UUID를 만들기 때문이다. */
class KnownPlayers(private val server: Server, private val files: PlayerDataFiles, private val logger: Logger) {
    fun find(query: String): NameMatch {
        val online = server.onlinePlayers.map { NamedPlayer(it.uniqueId, it.name, it.nickname()) }
        val onlineMatch = matchName(query, online)
        when (onlineMatch) {
            is NameMatch.Found, is NameMatch.Ambiguous -> return onlineMatch
            NameMatch.NotFound -> {}
        }
        val cached = server.getOfflinePlayerIfCached(query)?.takeIf { it.hasPlayedBefore() }
        val cachedName = cached?.name
        if (cached != null && cachedName != null) return NameMatch.Found(NamedPlayer(cached.uniqueId, cachedName, null))
        return matchName(query, offlineWithNicknames())
    }

    /** 자동완성은 닉네임을 우선 보여 준다(SMPUtil DESIGN 2장). 보낸 사람이 볼 수 없는(투명화된) 플레이어는 뺀다. */
    fun suggestions(sender: CommandSender, prefix: String): List<String> =
        server.onlinePlayers
            .filter { sender !is Player || sender.canSee(it) }
            .flatMap { listOfNotNull(it.nickname(), it.name) }
            .filter { it.startsWith(prefix, ignoreCase = true) }

    /** 오프라인 닉네임은 파일에만 있으므로, 진짜 이름으로 못 찾았을 때만 모든 파일을 훑는다. 친구 서버라 파일 수가 적다. */
    private fun offlineWithNicknames(): List<NamedPlayer> = files.knownPlayerIds().mapNotNull { id ->
        val name = server.getOfflinePlayer(id).name ?: return@mapNotNull null
        val nickname = try {
            files.read(id)?.let { storedPdcString(it, NICKNAME_KEY.asString()) }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Could not read the nickname of $id", e)
            null
        }
        NamedPlayer(id, name, nickname)
    }
}
