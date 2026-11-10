package dev.doyu.adminutil.config

import dev.doyu.adminutil.vanish.VanishNotice
import org.bukkit.configuration.ConfigurationSection

data class AdminSettings(val defaultVanishNotice: VanishNotice)

/**
 * 틀린 값은 기본값으로 덮지 않고 어떤 키인지 밝혀 시작 시 실패한다. 틀린 값을 조용히 쓰는 것보다 멈추는 게 낫다.
 * 빠진 키는 Bukkit이 jar에 든 config.yml 값으로 채운다.
 */
fun readSettings(config: ConfigurationSection): AdminSettings {
    val notice = config.getString("vanish.default-notice")
    return AdminSettings(
        defaultVanishNotice = requireNotNull(VanishNotice.parse(notice)) {
            "vanish.default-notice must be one of ${VanishNotice.entries.joinToString { it.id }}, was $notice"
        },
    )
}
