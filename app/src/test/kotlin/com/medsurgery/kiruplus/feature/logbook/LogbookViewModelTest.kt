package com.medsurgery.kiruplus.feature.logbook

import com.medsurgery.kiruplus.MainDispatcherRule
import com.medsurgery.kiruplus.R
import com.medsurgery.kiruplus.domain.logbook.LogbookRepository
import com.medsurgery.kiruplus.domain.logbook.SurgicalLog
import com.medsurgery.kiruplus.domain.logbook.SurgicalRole
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LogbookViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val log =
        SurgicalLog(
            id = "log-1",
            dateIso = "2026-05-18",
            procedureName = "Apendicectomía Laparoscópica",
            patientInitials = "JR",
            medicalRecordNumber = null,
            role = SurgicalRole.Cirujano,
            supervisorName = "Dra. Ruiz",
            notes = "Sin complicaciones.",
            durationMinutes = 60,
        )

    @Test
    fun `init load success populates logs`() =
        runTest {
            val repo: LogbookRepository = mockk()
            coEvery { repo.fetchLogs() } returns Result.success(listOf(log))

            val viewModel = LogbookViewModel(repo)

            assertEquals(listOf(log), viewModel.state.value.logs)
            assertTrue(!viewModel.state.value.isLoading)
            assertNull(viewModel.state.value.errorRes)
        }

    @Test
    fun `init load failure surfaces errorRes`() =
        runTest {
            val repo: LogbookRepository = mockk()
            coEvery { repo.fetchLogs() } returns Result.failure(RuntimeException("boom"))

            val viewModel = LogbookViewModel(repo)

            assertTrue(viewModel.state.value.logs.isEmpty())
            assertEquals(R.string.logbook_error_load, viewModel.state.value.errorRes)
        }
}
