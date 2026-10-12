package com.medsurgery.kiruplus.feature.auth

import com.medsurgery.kiruplus.MainDispatcherRule
import com.medsurgery.kiruplus.R
import com.medsurgery.kiruplus.core.auth.AuthRepository
import com.medsurgery.kiruplus.core.auth.SessionState
import com.medsurgery.kiruplus.domain.privacy.ACCOUNT_DELETION_GRACE_HOURS
import com.medsurgery.kiruplus.domain.privacy.AccountDeletionReceipt
import com.medsurgery.kiruplus.domain.privacy.AccountDeletionStatus
import com.medsurgery.kiruplus.domain.privacy.AccountDeletionStatusKind
import com.medsurgery.kiruplus.domain.privacy.PrivacyError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AccountDeletionViewModelTest {

    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repo: AuthRepository = mockk(relaxed = true) {
        coEvery { sessionState } returns MutableSharedFlow<SessionState>()
        coEvery { accountDeletionStatus() } returns Result.success(noneStatus())
    }

    private fun vm() = AccountDeletionViewModel(repo)

    @Test
    fun `not authenticated is not shown as no request`() = runTest {
        coEvery { repo.accountDeletionStatus() } returns Result.failure(PrivacyError.NotAuthenticated)
        val viewModel = vm()

        assertEquals(AccountDeletionPhase.NotAuthenticated, viewModel.state.value.phase)
    }

    @Test
    fun `scheduled and cancelable exposes the server grace window`() = runTest {
        coEvery { repo.accountDeletionStatus() } returns Result.success(
            AccountDeletionStatus(
                kind = AccountDeletionStatusKind.Scheduled,
                scheduledForIso = "2023-11-14T22:13:20Z",
                canCancel = true,
                graceHours = ACCOUNT_DELETION_GRACE_HOURS,
            ),
        )
        val viewModel = vm()
        val phase = viewModel.state.value.phase as AccountDeletionPhase.Scheduled

        assertTrue(phase.canCancel)
        assertEquals(ACCOUNT_DELETION_GRACE_HOURS, phase.graceHours)
    }

    @Test
    fun `submit without acknowledge is a no-op`() = runTest {
        val viewModel = vm()
        viewModel.submit()

        assertFalse(viewModel.state.value.completed)
        coVerify(exactly = 0) { repo.requestAccountDeletion(null) }
    }

    @Test
    fun `submit success stays scheduled and does not sign out until confirm`() = runTest {
        coEvery { repo.requestAccountDeletion(null) } returns Result.success(
            AccountDeletionReceipt(
                status = "scheduled",
                scheduledForIso = "2023-11-14T22:13:20Z",
                canCancel = true,
                graceHours = ACCOUNT_DELETION_GRACE_HOURS,
            ),
        )
        val viewModel = vm()
        viewModel.onAcknowledgeChange(true)
        viewModel.submit()

        val phase = viewModel.state.value.phase as AccountDeletionPhase.Scheduled
        assertEquals(ACCOUNT_DELETION_GRACE_HOURS, phase.graceHours)
        assertTrue(phase.canCancel)
        assertFalse(viewModel.state.value.completed)
        coVerify(exactly = 0) { repo.signOut() }

        viewModel.confirmTeardown()
        assertTrue(viewModel.state.value.completed)
        coVerify(exactly = 1) { repo.signOut() }
    }

    @Test
    fun `cancel is available only when the server says canCancel`() = runTest {
        coEvery { repo.accountDeletionStatus() } returns Result.success(
            AccountDeletionStatus(
                kind = AccountDeletionStatusKind.Scheduled,
                scheduledForIso = "2023-11-14T22:13:20Z",
                canCancel = false,
                graceHours = ACCOUNT_DELETION_GRACE_HOURS,
            ),
        )
        val viewModel = vm()
        viewModel.cancelScheduledDeletion()
        coVerify(exactly = 0) { repo.cancelAccountDeletion() }
    }

    @Test
    fun `submit failure does not sign out`() = runTest {
        coEvery { repo.requestAccountDeletion(null) } returns Result.failure(PrivacyError.NotAuthenticated)
        val viewModel = vm()
        viewModel.onAcknowledgeChange(true)
        viewModel.submit()

        assertFalse(viewModel.state.value.completed)
        assertEquals(R.string.delete_account_not_authenticated, viewModel.state.value.errorRes)
        coVerify(exactly = 0) { repo.signOut() }
    }

    private fun noneStatus() = AccountDeletionStatus(
        kind = AccountDeletionStatusKind.None,
        scheduledForIso = null,
        canCancel = false,
        graceHours = ACCOUNT_DELETION_GRACE_HOURS,
    )
}
