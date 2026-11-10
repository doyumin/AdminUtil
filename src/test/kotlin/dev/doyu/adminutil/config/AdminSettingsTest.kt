package dev.doyu.adminutil.config

import dev.doyu.adminutil.vanish.VanishNotice
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AdminSettingsTest {
    private fun yaml(text: String) = YamlConfiguration().apply { loadFromString(text) }

    @Test
    fun `the bundled config is valid`() {
        val bundled = YamlConfiguration.loadConfiguration(File("src/main/resources/config.yml"))
        assertEquals(AdminSettings(VanishNotice.FAKE), readSettings(bundled))
    }

    @Test
    fun `a valid notice is read`() {
        assertEquals(VanishNotice.SILENT, readSettings(yaml("vanish:\n  default-notice: silent\n")).defaultVanishNotice)
    }

    @Test
    fun `an unknown notice stops startup`() {
        assertFailsWith<IllegalArgumentException> { readSettings(yaml("vanish:\n  default-notice: loud\n")) }
    }
}
