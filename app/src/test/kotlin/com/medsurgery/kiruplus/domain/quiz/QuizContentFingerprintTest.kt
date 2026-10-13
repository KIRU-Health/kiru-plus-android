package com.medsurgery.kiruplus.domain.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizContentFingerprintTest {
    private val stem = "¿Cuál es el signo de Murphy?"
    private val options = listOf("b opción", "a opción", "á opción", "Z opción")

    @Test
    fun `normalize collapses only ascii whitespace and keeps non ascii spaces`() {
        assertEquals(
            "Colecistitis aguda grave",
            QuizContentFingerprint.normalize("  Colecistitis\t aguda\n grave  "),
        )
        assertEquals("A B C", QuizContentFingerprint.normalize("A\u000BB\u000CC"))
        assertEquals("linea uno linea dos", QuizContentFingerprint.normalize("linea uno\r\nlinea dos"))
        assertEquals("", QuizContentFingerprint.normalize(""))
        assertEquals("", QuizContentFingerprint.normalize("   "))
        assertEquals("Colecistitis\u00A0aguda", QuizContentFingerprint.normalize("Colecistitis\u00A0aguda"))
        assertEquals("A\u2009B", QuizContentFingerprint.normalize("A\u2009B"))
        assertEquals("A\u3000B", QuizContentFingerprint.normalize("A\u3000B"))
        assertEquals("ABC abc", QuizContentFingerprint.normalize("ABC abc"))
    }

    @Test
    fun `normalize applies nfc`() {
        val nfd = "Maria\u0301"
        val nfc = "Mari\u00E1"
        assertNotEquals(nfd, nfc)
        assertEquals(QuizContentFingerprint.normalize(nfc), QuizContentFingerprint.normalize(nfd))
        assertEquals(nfc, QuizContentFingerprint.normalize(nfd))
    }

    @Test
    fun `option id matches the python vectors`() {
        assertEquals("1c2e39618e9031c3", QuizContentFingerprint.optionId("  Colecistitis\t aguda\n grave  "))
        assertEquals("1a47398b453fae1d", QuizContentFingerprint.optionId("Colecistitis\u00A0aguda"))
        assertEquals("4b57f30455cf0f6a", QuizContentFingerprint.optionId("A\u2009B"))
        assertEquals("e048bc0ab8c002ce", QuizContentFingerprint.optionId("A\u3000B"))
        assertEquals("87bdf7f25d1dd764", QuizContentFingerprint.optionId("Colecistitis aguda de Mari\u00E1"))
        assertEquals("87bdf7f25d1dd764", QuizContentFingerprint.optionId("Colecistitis aguda de Maria\u0301"))
        assertEquals("f8ef5f9c7214669b", QuizContentFingerprint.optionId("A\u000BB\u000CC"))
        assertEquals("b3e1385a797ad094", QuizContentFingerprint.optionId("hernia inguino-crural \uD83E\uDE7A"))
        assertEquals("4214ef345ad2d485", QuizContentFingerprint.optionId("linea uno\r\nlinea dos"))
        assertEquals("e3b0c44298fc1c14", QuizContentFingerprint.optionId(""))
        assertEquals("e3b0c44298fc1c14", QuizContentFingerprint.optionId("   "))
    }

    @Test
    fun `hash16 is sixteen lowercase hex chars`() {
        val hash = QuizContentFingerprint.hash16("x")
        assertEquals(16, hash.length)
        assertTrue(hash.all { it in "0123456789abcdef" })
    }

    @Test
    fun `content version matches the python vector and ignores order and stem whitespace`() {
        assertEquals(
            "97b39f120f01bdd0",
            QuizContentFingerprint.contentVersion(stem, options, "a opción"),
        )
        val permuted = listOf("Z opción", "á opción", "a opción", "b opción")
        assertNotEquals(permuted, options)
        assertEquals(
            "97b39f120f01bdd0",
            QuizContentFingerprint.contentVersion(stem, permuted, "a opción"),
        )
        assertEquals(
            "97b39f120f01bdd0",
            QuizContentFingerprint.contentVersion("  ¿Cuál es el signo\tde Murphy? ", options, "a opción"),
        )
    }

    @Test
    fun `editing any text changes the version`() {
        val edited = listOf("b opción", "a opción", "á opción", "Z opciôn")
        assertEquals(
            "ddecccd95f956123",
            QuizContentFingerprint.contentVersion(stem, edited, "a opción"),
        )
        assertNotEquals(
            QuizContentFingerprint.contentVersion(stem, options, "b opción"),
            QuizContentFingerprint.contentVersion(stem, options, "a opción"),
        )
    }

    @Test
    fun `option order is utf8 bytes not locale order`() {
        val sorted =
            options.sortedWith { left, right ->
                val a = QuizContentFingerprint.normalize(left).toByteArray(Charsets.UTF_8)
                val b = QuizContentFingerprint.normalize(right).toByteArray(Charsets.UTF_8)
                compareBytes(a, b)
            }
        assertEquals(listOf("Z opción", "a opción", "b opción", "á opción"), sorted)
    }

    private fun compareBytes(
        a: ByteArray,
        b: ByteArray,
    ): Int {
        val shared = minOf(a.size, b.size)
        for (index in 0 until shared) {
            val delta = (a[index].toInt() and 0xFF) - (b[index].toInt() and 0xFF)
            if (delta != 0) return delta
        }
        return a.size - b.size
    }
}
