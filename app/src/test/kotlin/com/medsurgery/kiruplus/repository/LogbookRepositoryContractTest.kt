package com.medsurgery.kiruplus.repository

import com.medsurgery.kiruplus.domain.logbook.FeatureQuota
import com.medsurgery.kiruplus.domain.logbook.LogbookCapacity
import com.medsurgery.kiruplus.domain.logbook.LogbookRepository
import com.medsurgery.kiruplus.domain.logbook.LogbookSaveOutcome
import com.medsurgery.kiruplus.domain.logbook.NewLogInput
import com.medsurgery.kiruplus.domain.logbook.SurgicalLog
import com.medsurgery.kiruplus.domain.logbook.SurgicalRole
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogbookRepositoryContractTest {
    private val sampleLog =
        SurgicalLog(
            id = "log-001",
            dateIso = "2026-05-20",
            procedureName = "Laparotomía exploradora",
            patientInitials = "AB",
            medicalRecordNumber = null,
            role = SurgicalRole.Cirujano,
            supervisorName = "Dr. Vega",
            notes = "No complications",
            durationMinutes = 90,
        )

    private val newLog =
        NewLogInput(
            procedureName = "Laparotomía exploradora",
            procedureDate = LocalDate(2026, 5, 20),
            patientInitials = "AB",
            medicalRecordNumber = null,
            role = SurgicalRole.Cirujano,
            supervisorName = "Dr. Vega",
            notes = "",
            durationMinutes = 90,
        )

    @Test
    fun `fetchLogs returns surgical logs on success`() =
        runTest {
            val repo = FakeLogbookRepository(logs = listOf(sampleLog))
            val result = repo.fetchLogs()
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow().size)
            assertEquals("log-001", result.getOrThrow()[0].id)
        }

    @Test
    fun `createLog returns success and persists entry`() =
        runTest {
            val repo = FakeLogbookRepository()
            assertTrue(repo.createLog(newLog).isSuccess)
            assertEquals("Laparotomía exploradora", repo.fetchLogs().getOrThrow()[0].procedureName)
        }

    @Test
    fun `deleteLog removes the entry`() =
        runTest {
            val repo = FakeLogbookRepository(logs = mutableListOf(sampleLog))
            assertTrue(repo.deleteLog("log-001").isSuccess)
            assertTrue(repo.fetchLogs().getOrThrow().isEmpty())
        }
}

private class FakeLogbookRepository(
    logs: List<SurgicalLog> = emptyList(),
    private val shouldFail: Boolean = false,
) : LogbookRepository {
    private val store: MutableList<SurgicalLog> = logs.toMutableList()

    override suspend fun fetchLogs(): Result<List<SurgicalLog>> {
        if (shouldFail) {
            return Result.failure(RuntimeException("io error"))
        }
        return Result.success(store.toList())
    }

    override suspend fun createLog(input: NewLogInput): Result<LogbookSaveOutcome> {
        if (shouldFail) return Result.failure(RuntimeException("io error"))
        store.add(
            SurgicalLog(
                id = "fake",
                dateIso = input.procedureDate.toString(),
                procedureName = input.procedureName,
                patientInitials = input.patientInitials,
                medicalRecordNumber = input.medicalRecordNumber,
                role = input.role,
                supervisorName = input.supervisorName,
                notes = input.notes,
                durationMinutes = input.durationMinutes,
            ),
        )
        return Result.success(
            LogbookSaveOutcome.Created(LogbookCapacity(FeatureQuota.Unmetered, store.size)),
        )
    }

    override suspend fun deleteLog(id: String): Result<Unit> {
        if (shouldFail) return Result.failure(RuntimeException("io error"))
        store.removeAll { it.id == id }
        return Result.success(Unit)
    }
}
