package dev.doyu.adminutil.chat

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

/**
 * 채팅 서식은 명령어로 바꾸고 재시작해도 남아야 하므로, 사람이 쓰는 config.yml(주석이 지워질 수 있다)과 따로 파일에 둔다.
 * 파일 값이 틀리면 기본값으로 덮지 않고 시작 시 실패한다.
 */
class ChatFormatStore(private val file: File) {
    @Volatile
    var current: ChatFormat = load()
        private set

    fun set(format: ChatFormat) {
        val yaml = YamlConfiguration()
        yaml.set(KEY, format.template)
        yaml.save(file)
        current = format
    }

    fun reset() {
        file.delete()
        current = ChatFormat.DEFAULT
    }

    private fun load(): ChatFormat {
        if (!file.exists()) return ChatFormat.DEFAULT
        val template = YamlConfiguration.loadConfiguration(file).getString(KEY)
        return requireNotNull(template?.let(ChatFormat::parse)) { "${file.name} must have '$KEY' containing ${ChatFormat.MSG}, was $template" }
    }

    private companion object {
        const val KEY = "format"
    }
}
