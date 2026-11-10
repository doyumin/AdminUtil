package dev.doyu.adminutil.toggle

/** [id]는 PDC 키(`adminutil:<id>`)와 명령어 이름에 쓰인다. 바꾸면 저장된 토글이 풀린다. */
enum class AdminToggle(val id: String, val label: String) {
    FLY("fly", "비행"),
    INVULNERABLE("invulnerable", "무적"),
    NODAMAGE("nodamage", "노데미지"),
    UNSTARVE("unstarve", "배고픔 없음"),
    PICKUP("pickup", "줍기 차단"),
    GLASS("glass", "유리 헬멧"),
    SHUNPO("shunpo", "순보"),
}

data class ToggleSwitch(val toggle: AdminToggle, val nowOn: Boolean, val turnedOff: Set<AdminToggle>)

/** 무적과 노데미지는 둘 다 켜면 어느 쪽이 적용되는지 헷갈리므로, 하나를 켜면 다른 쪽을 끈다(DESIGN 3장). */
fun switchToggle(on: Set<AdminToggle>, toggle: AdminToggle): ToggleSwitch {
    if (toggle in on) return ToggleSwitch(toggle, nowOn = false, turnedOff = emptySet())
    val exclusive = when (toggle) {
        AdminToggle.INVULNERABLE -> setOf(AdminToggle.NODAMAGE)
        AdminToggle.NODAMAGE -> setOf(AdminToggle.INVULNERABLE)
        AdminToggle.FLY, AdminToggle.UNSTARVE, AdminToggle.PICKUP, AdminToggle.GLASS, AdminToggle.SHUNPO -> emptySet()
    }
    return ToggleSwitch(toggle, nowOn = true, turnedOff = exclusive intersect on)
}
