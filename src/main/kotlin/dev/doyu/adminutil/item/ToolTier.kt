package dev.doyu.adminutil.item

enum class ToolTier(val id: String, val label: String) {
    WOODEN("wooden", "나무"),
    STONE("stone", "돌"),
    GOLDEN("golden", "금"),
    IRON("iron", "철"),
    DIAMOND("diamond", "다이아몬드"),
    NETHERITE("netherite", "네더라이트"),
    ;

    /** 인벤토리 칸 → 아이템 ID. 핫바 6~9번 칸(5~8)에 삽·괭이·곡괭이·도끼. 나무 세트도 도끼만은 돌이다(DESIGN 4장). */
    fun toolIds(): Map<Int, String> {
        val axeTier = if (this == WOODEN) STONE else this
        return linkedMapOf(
            5 to "minecraft:${id}_shovel",
            6 to "minecraft:${id}_hoe",
            7 to "minecraft:${id}_pickaxe",
            8 to "minecraft:${axeTier.id}_axe",
        )
    }

    companion object {
        /** 재질을 생략하면 나무. 모르는 이름이면 null. */
        fun parse(text: String?): ToolTier? {
            if (text == null) return WOODEN
            val lowered = text.lowercase()
            if (lowered == "gold") return GOLDEN
            if (lowered == "다이아") return DIAMOND
            return entries.firstOrNull { it.id == lowered || it.label == text }
        }
    }
}
