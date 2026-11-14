package dev.doyu.adminutil.command

import dev.doyu.adminutil.toggle.AdminToggle

/** `/ac` 하위 명령어. 권한은 `adminutil.<id>`(기본 OP)로, 옛 코드처럼 isOp만 보지 않는다. */
enum class Subcommand(val id: String, val korean: String, val toggle: AdminToggle? = null) {
    FLY("fly", "비행", AdminToggle.FLY),
    INVULNERABLE("invulnerable", "무적", AdminToggle.INVULNERABLE),
    NODAMAGE("nodamage", "노데미지", AdminToggle.NODAMAGE),
    UNSTARVE("unstarve", "배고픔", AdminToggle.UNSTARVE),
    PICKUP("pickup", "줍기", AdminToggle.PICKUP),
    GLASS("glass", "유리", AdminToggle.GLASS),
    LIST("list", "목록"),
    TOOLS("tools", "도구"),
    KNIFE("knife", "장미칼"),
    SHUNPO("shunpo", "순보", AdminToggle.SHUNPO),
    LIGHTNING("lightning", "번개"),
    INV("inv", "인벤토리"),
    ENDER("ender", "엔더상자"),
    CHAT("chat", "채팅"),
    ;

    val permission: String get() = "adminutil.$id"

    companion object {
        fun parse(word: String?): Subcommand? = entries.firstOrNull { it.id == word?.lowercase() || it.korean == word }
    }
}

const val VANISH_PERMISSION = "adminutil.vanish"
