package com.medsurgery.kiruplus.domain.logbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogbookCapacityTest {
    @Test
    fun `each plan uses the iOS matrix`() {
        assertEquals(
            FeatureQuota.Limited(LogbookCapacityRule.FREE_ENTRIES),
            LogbookCapacityRule.quota(LogbookPlan.Free),
        )
        assertEquals(
            FeatureQuota.Limited(LogbookCapacityRule.ELITE_ENTRIES),
            LogbookCapacityRule.quota(LogbookPlan.ResidentElite),
        )
        assertEquals(FeatureQuota.Unmetered, LogbookCapacityRule.quota(LogbookPlan.SurgeonMega))
        assertEquals(FeatureQuota.Unmetered, LogbookCapacityRule.quota(LogbookPlan.SurgeonPro))
        assertEquals(
            LogbookCapacityRule.quota(LogbookPlan.SurgeonPro),
            LogbookCapacityRule.quota(LogbookPlan.SurgeonMega),
        )
        assertEquals(FeatureQuota.Limited(LogbookCapacityRule.FREE_ENTRIES), LogbookCapacityRule.defaultQuota)
    }

    @Test
    fun `unmetered override ignores every plan`() {
        LogbookPlan.entries.forEach { plan ->
            assertEquals(FeatureQuota.Unmetered, LogbookCapacityRule.quota(plan, unmetered = true))
        }
    }

    @Test
    fun `below the limit allows one more`() {
        val capacity =
            LogbookCapacity(FeatureQuota.Limited(LogbookCapacityRule.FREE_ENTRIES), BELOW_FREE)
        assertTrue(capacity.canCreate)
        assertEquals(LogbookCapacityRule.FREE_ENTRIES, capacity.limit)
        assertEquals(1, capacity.remaining)
        assertTrue(capacity.isMetered)
        assertFalse(capacity.isOverCapacity)
    }

    @Test
    fun `at the limit denies without being over capacity`() {
        val capacity =
            LogbookCapacity(
                FeatureQuota.Limited(LogbookCapacityRule.FREE_ENTRIES),
                LogbookCapacityRule.FREE_ENTRIES,
            )
        assertFalse(capacity.canCreate)
        assertEquals(0, capacity.remaining)
        assertFalse(capacity.isOverCapacity)
        assertTrue(capacity.reachedMessage.contains("20"))
    }

    @Test
    fun `over the limit keeps every record and blocks creation`() {
        val capacity =
            LogbookCapacity(FeatureQuota.Limited(LogbookCapacityRule.FREE_ENTRIES), OVER_FREE)
        assertFalse(capacity.canCreate)
        assertTrue(capacity.isOverCapacity)
        assertEquals(0, capacity.remaining)
        assertTrue(capacity.reachedMessage.contains("150"))
        assertTrue(capacity.reachedMessage.contains("conservan"))
    }

    @Test
    fun `unmetered has no limit`() {
        val capacity =
            LogbookCapacity(FeatureQuota.Unmetered, LARGE_COUNT)
        assertTrue(capacity.canCreate)
        assertNull(capacity.limit)
        assertNull(capacity.remaining)
        assertFalse(capacity.isMetered)
        assertFalse(capacity.isOverCapacity)
        assertEquals("", capacity.reachedMessage)
    }

    @Test
    fun `blocked quota never allows creation`() {
        val capacity =
            LogbookCapacity(FeatureQuota.None, 0)
        assertFalse(capacity.canCreate)
        assertEquals(0, capacity.limit)
        assertEquals("Tu plan no incluye la bitácora quirúrgica.", capacity.reachedMessage)
    }

    @Test
    fun `negative count is clamped to zero`() {
        val clamped =
            LogbookCapacity(
                FeatureQuota.Limited(LogbookCapacityRule.FREE_ENTRIES),
                NEGATIVE_COUNT,
            )
        assertEquals(0, clamped.count)
    }

    @Test
    fun `persisted flag follows the outcome`() {
        val full =
            LogbookCapacity(
                FeatureQuota.Limited(LogbookCapacityRule.FREE_ENTRIES),
                LogbookCapacityRule.FREE_ENTRIES,
            )
        assertTrue(LogbookSaveOutcome.Created(full).isPersisted)
        assertTrue(LogbookSaveOutcome.Updated.isPersisted)
        assertFalse(LogbookSaveOutcome.DeniedAtCapacity(full).isPersisted)
    }

    private companion object {
        const val BELOW_FREE = 19
        const val OVER_FREE = 150
        const val LARGE_COUNT = 5_000
        const val NEGATIVE_COUNT = -3
    }
}
