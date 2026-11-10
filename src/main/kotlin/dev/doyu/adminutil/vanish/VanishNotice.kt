package dev.doyu.adminutil.vanish

/** [id]는 설정 파일과 플레이어 PDC에 저장된다. */
enum class VanishNotice(val id: String, val korean: String, val label: String) {
    FAKE("fake", "가짜", "가짜 퇴장·입장 메시지"),
    ANNOUNCE("announce", "공지", "[Vanish] 공지"),
    SILENT("silent", "조용히", "본인에게만"),
    ;

    companion object {
        fun parse(text: String?): VanishNotice? = entries.firstOrNull { it.id == text?.lowercase() || it.korean == text }
    }
}
