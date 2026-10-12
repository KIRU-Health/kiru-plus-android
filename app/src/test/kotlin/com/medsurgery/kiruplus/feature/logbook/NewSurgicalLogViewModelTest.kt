package com.medsurgery.kiruplus.feature.logbook

import com.medsurgery.kiruplus.MainDispatcherRule
import com.medsurgery.kiruplus.R
import com.medsurgery.kiruplus.domain.logbook.LogbookRepository
import com.medsurgery.kiruplus.domain.logbook.NewLogInput
import com.medsurgery.kiruplus.domain.logbook.SurgicalRole
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NewSurgicalLogViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `submit without required fields does not call createLog`() =
        runTest {
            val repo: LogbookRepository = mockk()
            val viewModel = NewSurgicalLogViewModel(repo)

            viewModel.submit()

            assertEquals(R.string.logbook_error_incomplete, viewModel.state.value.errorRes)
            coVerify(exactly = 0) { repo.createLog(any()) }
        }

    @Test
    fun `submit success sets saved`() =
        runTest {
            val repo: LogbookRepository = mockk()
            coEvery { repo.createLog(any()) } returns Result.success(Unit)
            val viewModel = filled(repo)

            viewModel.submit()

            assertTrue(viewModel.state.value.saved)
            assertFalse(viewModel.state.value.isSubmitting)
            assertNull(viewModel.state.value.errorRes)
        }

    @Test
    fun `submit sends the iOS field set`() =
        runTest {
            val repo: LogbookRepository = mockk()
            coEvery { repo.createLog(any()) } returns Result.success(Unit)
            val viewModel = filled(repo)
            val date = LocalDate(2026, 5, 19)
            viewModel.setProcedureDate(date)
            viewModel.setRole(SurgicalRole.Observador)

            viewModel.submit()

            coVerify {
                repo.createLog(
                    NewLogInput(
                        procedureName = "Apendicectomía",
                        procedureDate = date,
                        patientInitials = "JR",
                        medicalRecordNumber = null,
                        role = SurgicalRole.Observador,
                        supervisorName = "Dra. Ruiz",
                        notes = "",
                        durationMinutes = 45,
                    ),
                )
            }
        }

    private fun filled(repo: LogbookRepository): NewSurgicalLogViewModel {
        val viewModel = NewSurgicalLogViewModel(repo)
        viewModel.setProcedureName("Apendicectomía")
        viewModel.setPatientInitials("JR")
        viewModel.setSupervisorName("Dra. Ruiz")
        viewModel.setDurationMinutes("45")
        return viewModel
    }
}
