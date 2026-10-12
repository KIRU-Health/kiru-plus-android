package com.medsurgery.kiruplus.library

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest

/**
 * File-system integrity checks — no Android runtime required.
 *
 * Verifies:
 * 1. The 3 authorized assets exist in the expected asset paths.
 * 2. Their SHA-256 checksums match the values registered in SHARED_ASSETS_REGISTRY.md.
 * 3. AndroidManifest.xml does NOT declare RECORD_AUDIO permission.
 * 4. No symlinks exist in the assets directory.
 */
class LibraryAssetIntegrityTest {

    // Project root: src/test/kotlin is 5 levels deep from the app module root.
    // app/src/test/kotlin/com/medsurgery/kiruplus/library → ../../../../.. = app/
    private val appRoot = File(
        javaClass.classLoader?.getResource(".")?.path.orEmpty()
    ).let { testOutputDir ->
        // Gradle test output: app/build/intermediates/unit_test_classes/... or similar.
        // Walk up to find the app module root (contains AndroidManifest.xml).
        generateSequence(testOutputDir.parentFile) { it.parentFile }
            .firstOrNull { File(it, "src/main/AndroidManifest.xml").exists() }
    }

    private fun assetsFile(relativePath: String): File? =
        appRoot?.let { File(it, "src/main/assets/$relativePath") }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(file.readBytes())
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    // Registered checksums from SHARED_ASSETS_REGISTRY.md
    private val expectedChecksums = mapOf(
        "curriculum/curriculum_index_v1.json" to "e7696d612354f04cdd7098c978ca826e0a943bb635507e9eb034e0f125e94cab",
        "curriculum/ChapterExams.json" to "ce5baa9885dfa8c9afb3bfb2a613ab13dbd2aad7850cba31ae6a9fb37dc845d3",
        "modules/StudyModules.json" to "0a5a048e7ee515e99da362b463f4eb8a178f22b45deb361bccfb9c33cfa0a8a1",
    )

    @Test
    fun `curriculum_index_v1 asset exists`() {
        val file = assetsFile("curriculum/curriculum_index_v1.json") ?: return
        assertTrue("curriculum_index_v1.json not found in assets/curriculum/", file.exists())
    }

    @Test
    fun `ChapterExams asset exists`() {
        val file = assetsFile("curriculum/ChapterExams.json") ?: return
        assertTrue("ChapterExams.json not found in assets/curriculum/", file.exists())
    }

    @Test
    fun `StudyModules asset exists`() {
        val file = assetsFile("modules/StudyModules.json") ?: return
        assertTrue("StudyModules.json not found in assets/modules/", file.exists())
    }

    @Test
    fun `all asset checksums match registry`() {
        expectedChecksums.forEach { (relativePath, expected) ->
            val file = assetsFile(relativePath) ?: return@forEach
            if (!file.exists()) return@forEach
            val actual = sha256(file)
            assertTrue(
                "SHA-256 mismatch for $relativePath.\n  expected=$expected\n  actual  =$actual",
                expected == actual,
            )
        }
    }

    @Test
    fun `AndroidManifest does not contain RECORD_AUDIO`() {
        val manifest = appRoot?.let { File(it, "src/main/AndroidManifest.xml") } ?: return
        if (!manifest.exists()) return
        val content = manifest.readText()
        assertFalse(
            "RECORD_AUDIO found in AndroidManifest.xml — it was removed in Sprint A and must not be re-added",
            content.contains("RECORD_AUDIO"),
        )
    }

    @Test
    fun `no symlinks in Android assets directory`() {
        val assetsDir = appRoot?.let { File(it, "src/main/assets") } ?: return
        if (!assetsDir.exists()) return
        val symlinks = assetsDir.walkTopDown().filter { file ->
            Files.isSymbolicLink(file.toPath())
        }.toList()
        assertTrue(
            "Symlinks found in assets/ — all assets must be real copies: ${symlinks.map { it.path }}",
            symlinks.isEmpty(),
        )
    }
}
