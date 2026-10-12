package com.medsurgery.kiruplus.domain.kapibaya

import kotlinx.coroutines.flow.Flow

interface KapibayaRepository {
    /**
     * Streams partial text chunks from `ask_kapibaya` via SSE.
     * Completes normally on `event: done`, throws on `event: error`.
     * The Edge Function (service_role) persists turns in Supabase — the
     * client only receives the text stream.
     */
    fun sendMessageStream(
        conversationId: String,
        message: String,
    ): Flow<String>
}
