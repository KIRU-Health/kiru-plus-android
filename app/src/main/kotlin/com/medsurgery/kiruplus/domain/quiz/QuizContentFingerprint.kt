package com.medsurgery.kiruplus.domain.quiz

import java.security.MessageDigest
import java.text.Normalizer

/**
 * Transcripción del contrato de `QuizContentFingerprint.swift`.
 * NFC, blancos ASCII colapsados, recorte solo de U+0020, SHA-256 hex de 16.
 * El servidor acepta el evento solo si esta huella coincide byte a byte.
 */
object QuizContentFingerprint {
    const val UNIT_SEPARATOR = "\u001F"
    private const val HASH_HEX_CHARS = 16
    private const val UNSIGNED_BYTE_MASK = 0xFF
    private val asciiWhitespaceRun = Regex("[ \t\n\r\u000B\u000C]+")

    fun normalize(text: String): String {
        val composed = Normalizer.normalize(text, Normalizer.Form.NFC)
        val collapsed = composed.replace(asciiWhitespaceRun, " ")
        return trimAsciiSpaces(collapsed)
    }

    fun hash16(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        val hex =
            buildString(digest.size * 2) {
                digest.forEach { byte ->
                    append("%02x".format(byte.toInt() and UNSIGNED_BYTE_MASK))
                }
            }
        return hex.substring(0, HASH_HEX_CHARS)
    }

    fun optionId(option: String): String = hash16(normalize(option))

    fun contentVersion(
        stem: String,
        options: List<String>,
        correct: String,
    ): String {
        val sortedOptions = options.map(::normalize).sortedWith(::compareUtf8)
        val canonical =
            listOf(
                normalize(stem),
                sortedOptions.joinToString(UNIT_SEPARATOR),
                normalize(correct),
            ).joinToString(UNIT_SEPARATOR)
        return hash16(canonical)
    }

    private fun trimAsciiSpaces(value: String): String {
        var start = 0
        var end = value.length
        while (start < end && value[start] == ' ') start += 1
        while (end > start && value[end - 1] == ' ') end -= 1
        return value.substring(start, end)
    }

    private fun compareUtf8(
        left: String,
        right: String,
    ): Int {
        val a = left.toByteArray(Charsets.UTF_8)
        val b = right.toByteArray(Charsets.UTF_8)
        val shared = minOf(a.size, b.size)
        for (index in 0 until shared) {
            val delta = (a[index].toInt() and UNSIGNED_BYTE_MASK) - (b[index].toInt() and UNSIGNED_BYTE_MASK)
            if (delta != 0) return delta
        }
        return a.size - b.size
    }
}
