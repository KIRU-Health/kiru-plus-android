package com.medsurgery.kiruplus.data.logbook

import com.medsurgery.kiruplus.core.premium.EntitlementState
import com.medsurgery.kiruplus.core.premium.KiruAccessTier
import com.medsurgery.kiruplus.core.premium.logbookQuota
import com.medsurgery.kiruplus.domain.logbook.FeatureQuota
import com.medsurgery.kiruplus.domain.logbook.LogbookCapacityRule
import com.medsurgery.kiruplus.domain.logbook.LogbookSaveOutcome
import com.medsurgery.kiruplus.domain.logbook.NewLogInput
import com.medsurgery.kiruplus.domain.logbook.SurgicalRole
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.crypto.spec.SecretKeySpec

class LogbookCapacityRepositoryTest {
    @Test
    fun `free plan denies the twenty first entry and delete frees a slot`() =
        runTest {
            val repo = repository { EntitlementState.Free.logbookQuota() }
            repeat(LogbookCapacityRule.FREE_ENTRIES) { index ->
                assertTrue(repo.createLog(input(index)).getOrThrow() is LogbookSaveOutcome.Created)
            }
            val denied = repo.createLog(input(LogbookCapacityRule.FREE_ENTRIES)).getOrThrow()
            check(denied is LogbookSaveOutcome.DeniedAtCapacity)
            assertEquals(LogbookCapacityRule.FREE_ENTRIES, denied.capacity.count)
            assertEquals(LogbookCapacityRule.FREE_ENTRIES, repo.fetchLogs().getOrThrow().size)

            val oldest = repo.fetchLogs().getOrThrow().last().id
            assertTrue(repo.deleteLog(oldest).isSuccess)
            val reopened = repo.createLog(input(LogbookCapacityRule.FREE_ENTRIES)).getOrThrow()
            assertTrue(reopened is LogbookSaveOutcome.Created)
        }

    @Test
    fun `resident elite denies the two hundred first entry`() =
        runTest {
            val repo =
                repository {
                    EntitlementState.Premium(KiruAccessTier.ResidentElite, null).logbookQuota()
                }
            repeat(LogbookCapacityRule.ELITE_ENTRIES) { index ->
                assertTrue(repo.createLog(input(index)).getOrThrow() is LogbookSaveOutcome.Created)
            }
            val denied = repo.createLog(input(LogbookCapacityRule.ELITE_ENTRIES)).getOrThrow()
            check(denied is LogbookSaveOutcome.DeniedAtCapacity)
            assertEquals(LogbookCapacityRule.ELITE_ENTRIES, denied.capacity.count)
        }

    @Test
    fun `mega and pro stay unmetered past the free cap`() =
        runTest {
            listOf(
                EntitlementState.Premium(KiruAccessTier.SurgeonMega, null),
                EntitlementState.Premium(KiruAccessTier.SurgeonPro, null),
            ).forEach { plan ->
                val repo = repository { plan.logbookQuota() }
                repeat(LogbookCapacityRule.FREE_ENTRIES + 1) { index ->
                    assertTrue(repo.createLog(input(index)).getOrThrow() is LogbookSaveOutcome.Created)
                }
            }
        }

    @Test
    fun `downgrade preserves records and blocks new ones`() =
        runTest {
            var quota: FeatureQuota = FeatureQuota.Unmetered
            val repo = repository { quota }
            repeat(OVER_FREE) { index ->
                assertTrue(repo.createLog(input(index)).isSuccess)
            }
            quota = FeatureQuota.Limited(LogbookCapacityRule.FREE_ENTRIES)
            val denied = repo.createLog(input(OVER_FREE)).getOrThrow()
            check(denied is LogbookSaveOutcome.DeniedAtCapacity)
            assertTrue(denied.capacity.isOverCapacity)
            assertEquals(OVER_FREE, repo.fetchLogs().getOrThrow().size)
            assertTrue(denied.capacity.reachedMessage.contains("conservan"))
        }

    private fun repository(quota: () -> FeatureQuota): LocalLogbookRepository {
        val file = File.createTempFile("surgical_logbook", ".bin")
        file.deleteOnExit()
        file.delete()
        val key = SecretKeySpec(ByteArray(AES_BYTES) { index -> index.toByte() }, "AES")
        return LocalLogbookRepository(EncryptedLogbookStore(file, AesGcmLogbookCipher(key)), quota)
    }

    private fun input(index: Int) =
        NewLogInput(
            procedureName = "Procedimiento $index",
            procedureDate = LocalDate(2026, 5, 20),
            patientInitials = "JD",
            medicalRecordNumber = null,
            role = SurgicalRole.Cirujano,
            supervisorName = "Dr. Supervisor",
            notes = "",
            durationMinutes = 60,
        )

    private companion object {
        const val AES_BYTES = 16
        const val OVER_FREE = 25
    }
}
