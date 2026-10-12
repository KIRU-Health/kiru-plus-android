package com.medsurgery.kiruplus.data.logbook

import com.medsurgery.kiruplus.domain.logbook.NewLogInput
import com.medsurgery.kiruplus.domain.logbook.SurgicalRole
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.crypto.spec.SecretKeySpec

class EncryptedLogbookRepositoryTest {
    @Test
    fun `create list delete survive a new repository on the same file`() =
        runTest {
            val file = File.createTempFile("surgical_logbook", ".bin")
            file.delete()
            val first = repository(file)
            val input =
                NewLogInput(
                    procedureName = "Apendicectomía",
                    procedureDate = LocalDate(2026, 5, 20),
                    patientInitials = "a b-12",
                    medicalRecordNumber = "EXP-1",
                    role = SurgicalRole.Ayudante1,
                    supervisorName = "Dra. Ruiz",
                    notes = "Sin complicaciones",
                    durationMinutes = 75,
                )

            assertTrue(first.createLog(input).isSuccess)
            val created = first.fetchLogs().getOrThrow()
            assertEquals(1, created.size)
            assertEquals("AB", created[0].patientInitials)
            assertEquals("Apendicectomía", created[0].procedureName)

            val reopened = repository(file)
            assertEquals(created[0].id, reopened.fetchLogs().getOrThrow()[0].id)

            assertTrue(reopened.deleteLog(created[0].id).isSuccess)
            val afterDelete = repository(file)
            assertTrue(afterDelete.fetchLogs().getOrThrow().isEmpty())
            file.delete()
        }

    private fun repository(file: File): LocalLogbookRepository {
        val key = SecretKeySpec(ByteArray(AES_BYTES) { index -> index.toByte() }, "AES")
        return LocalLogbookRepository(EncryptedLogbookStore(file, AesGcmLogbookCipher(key)))
    }

    private companion object {
        const val AES_BYTES = 16
    }
}
