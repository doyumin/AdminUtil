package econet.deploy

import org.gradle.api.GradleException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DeploySettingsTest {
    /** 같은 plugins 폴더를 쓰는 세 플러그인. 새 플러그인이 같은 서버에 들어오면 여기에 더한다. */
    private val siblings = listOf("Econet", "SMPUtil", "AdminUtil")

    @Test
    fun `property keys map to upper snake case environment variables`() {
        assertEquals("ECONET_SFTP_HOST", environmentNameOf("econet.sftp.host"))
        assertEquals("ECONET_SFTP_KEY_PATH", environmentNameOf("econet.sftp.keyPath"))
        assertEquals("ECONET_PANEL_SERVER_ID", environmentNameOf("econet.panel.serverId"))
        assertEquals("ECONET_PANEL_API_KEY", environmentNameOf("econet.panel.apiKey"))
    }

    @Test
    fun `sftp addresses accept the panel url form`() {
        assertEquals(SftpAddress("sftp.example.com", 2022), parseSftpAddress("sftp://sftp.example.com:2022"))
        assertEquals(SftpAddress("sftp.example.com", null), parseSftpAddress("sftp.example.com"))
        assertFailsWith<GradleException> { parseSftpAddress("sftp://host:99999") }
        assertFailsWith<GradleException> { parseSftpAddress("sftp://") }
    }

    @Test
    fun `only this plugin's jars and unfinished uploads are selected`() {
        val names = listOf(
            "AdminUtil.jar", "AdminUtil.jar.uploading", "AdminUtil-1.0.jar", "AdminUtil-0.9.jar", "AdminUtil-1.0.jar.uploading",
            "AdminUtilAddon-1.0.jar", "AdminUtilAddon.jar", "Vault.jar", "AdminUtil", "AdminUtil-notes.txt", "AdminUtil.jar.bak", "LuckPerms-5.4.jar",
        )

        assertEquals(
            listOf("AdminUtil-0.9.jar", "AdminUtil-1.0.jar", "AdminUtil-1.0.jar.uploading", "AdminUtil.jar", "AdminUtil.jar.uploading"),
            ownedRemoteFiles(names, "AdminUtil"),
        )
    }

    @Test
    fun `deploying AdminUtil leaves Econet and SMPUtil alone`() {
        val folder = listOf("Econet.jar", "SMPUtil.jar", "AdminUtil.jar")

        assertEquals(listOf("AdminUtil.jar"), ownedRemoteFiles(folder, "AdminUtil"))
    }

    @Test
    fun `deploying SMPUtil leaves Econet and AdminUtil alone`() {
        val folder = listOf("Econet.jar", "SMPUtil.jar", "AdminUtil.jar")

        assertEquals(listOf("SMPUtil.jar"), ownedRemoteFiles(folder, "SMPUtil"))
    }

    @Test
    fun `no sibling's jar, old versions or unfinished upload is ever deleted by another`() {
        val folder = siblings.flatMap { listOf("$it.jar", "$it.jar.uploading", "$it-1.0.jar", "$it-1.0.jar.uploading") } + "Vault.jar"

        for (deployed in siblings) {
            val deleted = ownedRemoteFiles(folder, deployed)
            for (other in siblings - deployed) {
                val protected = listOf("$other.jar", "$other.jar.uploading", "$other-1.0.jar", "$other-1.0.jar.uploading")
                assertTrue(deleted.none { it in protected }, "deploying $deployed must not delete $other's files, but selected $deleted")
            }
            assertTrue("Vault.jar" !in deleted, "deploying $deployed must not delete other plugins' files")
            assertEquals(4, deleted.size, "deploying $deployed should select exactly its own four files: $deleted")
        }
    }
}
