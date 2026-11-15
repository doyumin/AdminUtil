package dev.doyu.adminutil.mode

/** [id]는 플레이어 PDC에 저장된다. 바꾸면 이미 보관된 인벤토리를 못 찾는다. */
enum class InventoryMode(val id: String, val korean: String, val label: String) {
    PLAYER("player", "플레이어", "플레이어 인벤토리"),
    ADMIN("admin", "관리자", "관리자 인벤토리"),
    ;

    val other: InventoryMode get() = if (this == PLAYER) ADMIN else PLAYER

    companion object {
        fun parse(text: String?): InventoryMode? = entries.firstOrNull { it.id == text?.lowercase() || it.korean == text }
    }
}
