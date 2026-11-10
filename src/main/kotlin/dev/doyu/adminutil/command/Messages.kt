package dev.doyu.adminutil.command

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

fun adminMessage(text: String, color: TextColor = NamedTextColor.WHITE): Component =
    Component.text("[관리] ", NamedTextColor.GOLD).append(Component.text(text, color))

fun onOff(on: Boolean): Component = if (on) Component.text("켜짐", NamedTextColor.GREEN) else Component.text("꺼짐", NamedTextColor.RED)
