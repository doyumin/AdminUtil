package dev.doyu.adminutil.inspect

import org.bukkit.Server
import java.io.File
import java.util.UUID

/** 오프라인 플레이어의 저장 파일을 찾아 읽는다. 읽기만 한다: 파일을 고치면 접속 시 서버가 덮어써 수정이 사라지거나 아이템이 복제된다. */
class PlayerDataFiles(private val server: Server) {
    /**
     * Paper 26.3은 `<월드 루트>/players/data`에 둔다. 옛 버전에서 옮겨 온 서버를 위해 `playerdata`도 본다.
     * 26.3의 World.getWorldFolder()는 루트가 아니라 차원 폴더(`world/dimensions/minecraft/overworld`)를 돌려주므로,
     * level.dat가 있는 폴더까지 올라가 루트를 찾는다(Econet에서 실제 서버로 확인).
     */
    fun dataFolder(): File {
        val dimensionFolder = server.worlds.first().worldFolder.absoluteFile
        val root = generateSequence(dimensionFolder) { it.parentFile }.firstOrNull { it.resolve("level.dat").isFile } ?: dimensionFolder
        return root.resolve("players/data").takeIf { it.isDirectory } ?: root.resolve("playerdata")
    }

    fun knownPlayerIds(): List<UUID> =
        dataFolder().listFiles { file -> file.extension == "dat" }.orEmpty()
            .mapNotNull { runCatching { UUID.fromString(it.nameWithoutExtension) }.getOrNull() }

    fun read(player: UUID): NbtTag.CompoundTag? {
        val file = dataFolder().resolve("$player.dat")
        if (!file.isFile) return null
        return readCompressedNbt(file.readBytes())
    }
}
