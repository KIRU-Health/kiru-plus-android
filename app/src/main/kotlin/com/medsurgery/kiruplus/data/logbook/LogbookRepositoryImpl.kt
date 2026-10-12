package com.medsurgery.kiruplus.data.logbook

import android.content.Context
import com.medsurgery.kiruplus.domain.logbook.LogbookRepository
import com.medsurgery.kiruplus.domain.logbook.NewLogInput
import com.medsurgery.kiruplus.domain.logbook.SurgicalLog
import com.medsurgery.kiruplus.domain.logbook.SurgicalRole
import com.medsurgery.kiruplus.domain.logbook.sanitizedInitials
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

internal class EncryptedLogbookStore(
    private val file: File,
    private val cipher: LogbookCipher,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun read(): List<SurgicalLog> {
        if (!file.exists() || file.length() == 0L) return emptyList()
        val plain = cipher.decrypt(file.readBytes())
        val rows = json.decodeFromString<List<StoredLog>>(plain.decodeToString())
        return rows.map { it.toDomain() }.sortedByDescending { it.dateIso }
    }

    fun write(logs: List<SurgicalLog>) {
        val payload = json.encodeToString(logs.map { StoredLog.from(it) })
        val sealedBytes = cipher.encrypt(payload.encodeToByteArray())
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, "${file.name}.tmp")
        temporary.writeBytes(sealedBytes)
        if (!temporary.renameTo(file)) {
            file.writeBytes(sealedBytes)
            temporary.delete()
        }
    }
}

internal class LocalLogbookRepository(
    private val store: EncryptedLogbookStore,
) : LogbookRepository {
    override suspend fun fetchLogs(): Result<List<SurgicalLog>> = runIo { store.read() }

    override suspend fun createLog(input: NewLogInput): Result<Unit> =
        runIo {
            val current = store.read().toMutableList()
            current.add(input.toEntry())
            store.write(current.sortedByDescending { it.dateIso })
        }

    override suspend fun deleteLog(id: String): Result<Unit> =
        runIo {
            store.write(store.read().filter { it.id != id })
        }

    private suspend fun <T> runIo(block: () -> T): Result<T> = runCatching { withContext(Dispatchers.IO) { block() } }
}

@Singleton
class LogbookRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : LogbookRepository by LocalLogbookRepository(
        EncryptedLogbookStore(
            file = File(context.filesDir, FILE_NAME),
            cipher = AndroidKeystoreLogbookCipher(),
        ),
    ) {
    private companion object {
        const val FILE_NAME = "surgical_logbook.json"
    }
}

internal fun NewLogInput.toEntry(id: String = UUID.randomUUID().toString()): SurgicalLog =
    SurgicalLog(
        id = id,
        dateIso = procedureDate.toString(),
        procedureName = procedureName.trim(),
        patientInitials = sanitizedInitials(patientInitials),
        medicalRecordNumber = medicalRecordNumber?.trim()?.ifBlank { null },
        role = role,
        supervisorName = supervisorName.trim(),
        notes = notes.trim(),
        durationMinutes = durationMinutes,
    )

@Serializable
private data class StoredLog(
    val id: String,
    val dateIso: String,
    val procedureName: String,
    val patientInitials: String,
    val medicalRecordNumber: String? = null,
    val role: String,
    val supervisorName: String,
    val notes: String,
    val durationMinutes: Int,
) {
    fun toDomain(): SurgicalLog =
        SurgicalLog(
            id = id,
            dateIso = dateIso,
            procedureName = procedureName,
            patientInitials = patientInitials,
            medicalRecordNumber = medicalRecordNumber,
            role = SurgicalRole.fromWire(role),
            supervisorName = supervisorName,
            notes = notes,
            durationMinutes = durationMinutes,
        )

    companion object {
        fun from(log: SurgicalLog): StoredLog =
            StoredLog(
                id = log.id,
                dateIso = log.dateIso,
                procedureName = log.procedureName,
                patientInitials = log.patientInitials,
                medicalRecordNumber = log.medicalRecordNumber,
                role = log.role.wire,
                supervisorName = log.supervisorName,
                notes = log.notes,
                durationMinutes = log.durationMinutes,
            )
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class LogbookRepositoryBindings {
    @Binds
    abstract fun bindLogbookRepository(impl: LogbookRepositoryImpl): LogbookRepository
}
