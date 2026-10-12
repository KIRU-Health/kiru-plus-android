package com.medsurgery.kiruplus.data.kapibaya

import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KapibayaStreamRequestTest {

    @Test
    fun `request targets ask_kapibaya with bearer and apikey`() {
        val request = kapibayaStreamRequest(
            supabaseUrl = "https://tttxmupjteqpljtfgmgo.supabase.co",
            anonKey = "anon-key",
            accessToken = "session-jwt",
            userId = "ABC-DEF",
            conversationId = "conv-1",
            message = "hola",
        )

        assertEquals(
            "https://tttxmupjteqpljtfgmgo.supabase.co/functions/v1/ask_kapibaya",
            request.url.toString(),
        )
        assertEquals("anon-key", request.header("apikey"))
        assertEquals("Bearer session-jwt", request.header("Authorization"))
        val body = request.body
        assertTrue(body != null)
        if (body != null) {
            val buffer = Buffer()
            body.writeTo(buffer)
            val json = buffer.readUtf8()
            assertTrue(json.contains("\"userId\":\"abc-def\""))
            assertTrue(json.contains("\"stream\":true"))
            assertTrue(!json.contains("ask_kapibaya_stream"))
        }
    }
}
