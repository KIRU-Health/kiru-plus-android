package com.medsurgery.kiruplus.data.kcortex

import com.medsurgery.kiruplus.BuildConfig
import com.medsurgery.kiruplus.domain.kcortex.ClinicalDataRow
import com.medsurgery.kiruplus.domain.kcortex.KCortexAnalysis
import com.medsurgery.kiruplus.domain.kcortex.KCortexRepository
import com.medsurgery.kiruplus.domain.kcortex.KCortexRequest
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val ANALYZE_PATH = "/v1/kcortex/analyze"

@Singleton
class KCortexRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    private val okHttpClient: OkHttpClient,
) : KCortexRepository {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    override suspend fun analyze(request: KCortexRequest): Result<KCortexAnalysis> =
        withContext(Dispatchers.IO) {
            runCatching {
                val baseUrl = BuildConfig.BACKEND_BASE_URL.trim().trimEnd('/')
                if (baseUrl.isEmpty()) {
                    error("K-CORTEX backend is not configured for this environment")
                }
                // Resolve auth session (best-effort – backend may not require it for text-only)
                val uid = runCatching { supabase.auth.currentUserOrNull()?.id }.getOrNull()

                // Build prompt mirroring iOS KCortexAnalysisType.prompt
                val prompt = buildPrompt(request)

                // Multipart body: text_input + modality + prompt (no binary image on Android v1)
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("text_input", request.clinicalInput)
                    .addFormDataPart("modality", request.analysisType.backendKey)
                    .addFormDataPart("prompt", prompt)
                    .also { builder ->
                        if (uid != null) builder.addFormDataPart("user_id", uid)
                    }
                    .build()

                val httpRequest = Request.Builder()
                    .url("$baseUrl$ANALYZE_PATH")
                    .post(body)
                    // Include Supabase anon key so the backend can validate the caller
                    .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                    .build()

                okHttpClient.newCall(httpRequest).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""

                    Timber.d("[KCortex] HTTP %d — %s", response.code, responseBody.take(200))

                    if (!response.isSuccessful) {
                        error("K-CORTEX backend returned HTTP ${response.code}")
                    }

                    parseResponse(responseBody, request)
                }
            }
        }

    // ── Prompt builder (mirrors iOS KCortexAnalysisType.prompt) ──────────────────

    private val safetyPreamble = """
        REGLAS CLÍNICAS DE SEGURIDAD (NO NEGOCIABLES):
        1. NO INVENTES valores numéricos, rangos de referencia, fechas ni edades. Si un valor no está visible en el texto, escribe "no identificable".
        2. NO INFIERAS la institución si el nombre no aparece literal en el texto.
        3. NO ASUMAS la especie del paciente. Asume PACIENTE HUMANO ADULTO.
        4. NO ASUMAS edad, sexo, peso ni comorbilidades que no estén escritos.
        5. Si la calidad del texto es insuficiente, dilo claramente y NO improvises.
    """.trimIndent()

    private val outputFormat = """
        FORMATO DE RESPUESTA OBLIGATORIO:
        A. Datos visibles extraídos
        B. Datos no identificables
        C. Interpretación clínica (condicionada a A)
        D. Limitaciones y siguiente paso
    """.trimIndent()

    private fun buildPrompt(request: KCortexRequest): String {
        val input = request.clinicalInput
        return when (request.analysisType) {
            com.medsurgery.kiruplus.domain.kcortex.KCortexAnalysisType.LABORATORIOS -> """
                Eres el Dr. Kapibaya, asistente clínico de inteligencia artificial. Analiza los siguientes valores de laboratorio.
                $safetyPreamble
                DATOS DEL CASO:
                $input
                INSTRUCCIONES: Usa rangos de referencia SSA/IMSS 2024. Marca valores críticos con 🔴 CRÍTICO.
                $outputFormat
            """.trimIndent()

            com.medsurgery.kiruplus.domain.kcortex.KCortexAnalysisType.GASOMETRIA -> """
                Eres el Dr. Kapibaya, asistente clínico de inteligencia artificial. Analiza la siguiente gasometría.
                $safetyPreamble
                DATOS DEL CASO:
                $input
                INSTRUCCIONES: SOLO interpreta los parámetros visibles (pH, PaCO2, HCO3, PaO2, BE, lactato, SaO2, FiO2). NO completes valores faltantes.
                $outputFormat
            """.trimIndent()

            com.medsurgery.kiruplus.domain.kcortex.KCortexAnalysisType.ECG -> """
                Eres el Dr. Kapibaya, cardiólogo virtual de inteligencia artificial. Analiza el siguiente reporte de ECG.
                $safetyPreamble
                DATOS DEL CASO:
                $input
                INSTRUCCIONES: Clasifica urgencia: 🔴 INMEDIATA / 🟠 URGENTE / 🟡 ELECTIVA / 🟢 RUTINA.
                $outputFormat
            """.trimIndent()

            com.medsurgery.kiruplus.domain.kcortex.KCortexAnalysisType.IMAGEN_MEDICA -> """
                Eres el Dr. Kapibaya, radiólogo virtual de inteligencia artificial. Analiza la siguiente descripción de imagen médica.
                $safetyPreamble
                DATOS DEL CASO:
                $input
                INSTRUCCIONES: SOLO interpreta hallazgos descritos. Usa nomenclatura estándar (ACR, BI-RADS, Fleischner) si los datos lo permiten.
                $outputFormat
            """.trimIndent()

            com.medsurgery.kiruplus.domain.kcortex.KCortexAnalysisType.TEXTO_CLINICO -> """
                Eres el Dr. Kapibaya, internista virtual de inteligencia artificial. Analiza el siguiente texto clínico.
                $safetyPreamble
                DATOS DEL CASO:
                $input
                INSTRUCCIONES: Lista diagnósticos diferenciales por probabilidad. Señala 🔴 red flags urgentes.
                $outputFormat
            """.trimIndent()
        }
    }

    // ── Response parser ───────────────────────────────────────────────────────────

    private fun parseResponse(body: String, request: KCortexRequest): KCortexAnalysis {
        // Try structured response first
        val structured = runCatching {
            json.decodeFromString<KCortexClinicalResponseDto>(body)
        }.getOrNull()

        if (structured != null) {
            return structured.toDomain(request)
        }

        // Fallback: backend returned a simple text response (older format or text-only mode)
        val textResponse = runCatching {
            json.decodeFromString<KCortexTextResponseDto>(body)
        }.getOrNull()

        val analysisText = textResponse?.analysis
            ?: textResponse?.message
            ?: body.take(4000)

        return KCortexAnalysis(
            id = UUID.randomUUID().toString(),
            analysisType = request.analysisType,
            status = "partial",
            quality = "Parcialmente analizable",
            modality = request.analysisType.displayName,
            findings = analysisText,
            preliminaryInterpretation = "",
            limitations = "Análisis en modo texto simplificado.",
            missingData = "",
            recommendations = "Consulta el resultado con tu médico tratante.",
            redFlags = emptyList(),
            clinicalData = emptyList(),
            clinicalDataExtractedCount = 0,
            adminDataIgnoredCount = 0,
            rejectionReason = null,
            rawInput = request.clinicalInput,
        )
    }
}

// ── Mapping ───────────────────────────────────────────────────────────────────

private fun KCortexClinicalResponseDto.toDomain(request: KCortexRequest): KCortexAnalysis =
    KCortexAnalysis(
        id = UUID.randomUUID().toString(),
        analysisType = request.analysisType,
        status = status,
        quality = quality,
        modality = modality,
        findings = findings,
        preliminaryInterpretation = preliminaryInterpretation,
        limitations = limitations,
        missingData = missingData,
        recommendations = recommendations,
        redFlags = redFlags,
        clinicalData = clinicalData.map { it.toDomain() },
        clinicalDataExtractedCount = clinicalDataExtractedCount,
        adminDataIgnoredCount = adminDataIgnoredCount,
        rejectionReason = rejectionReason,
        rawInput = request.clinicalInput,
    )

private fun KCortexClinicalDataDto.toDomain(): ClinicalDataRow =
    ClinicalDataRow(
        analyte = analyte,
        value = value,
        unit = unit ?: "",
        referenceRange = referenceRange ?: "",
        interpretation = interpretation,
        confidence = confidence,
    )

// ── Hilt binding ──────────────────────────────────────────────────────────────

@Module
@InstallIn(SingletonComponent::class)
abstract class KCortexRepositoryBindings {
    @Binds
    abstract fun bindKCortexRepository(impl: KCortexRepositoryImpl): KCortexRepository
}
