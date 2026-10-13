package com.medsurgery.kiruplus.core.premium

import com.medsurgery.kiruplus.domain.logbook.FeatureQuota
import com.medsurgery.kiruplus.domain.logbook.LogbookCapacityRule
import org.junit.Assert.assertEquals
import org.junit.Test

class LogbookPlanQuotaTest {
    @Test
    fun `customer info maps each plan to the iOS logbook quota`() {
        assertEquals(
            FeatureQuota.Limited(LogbookCapacityRule.FREE_ENTRIES),
            resolveCustomerInfo(emptyList()).logbookQuota(),
        )
        assertEquals(
            FeatureQuota.Limited(LogbookCapacityRule.ELITE_ENTRIES),
            customerInfo(
                KiruRevenueCatIds.productResidentEliteMonthly,
                KiruRevenueCatIds.entitlementTierResident,
            ).logbookQuota(),
        )
        assertEquals(
            FeatureQuota.Unmetered,
            customerInfo(
                KiruRevenueCatIds.productSurgeonMegaMonthly,
                KiruRevenueCatIds.entitlementTierPro,
            ).logbookQuota(),
        )
        assertEquals(
            FeatureQuota.Unmetered,
            customerInfo(
                KiruRevenueCatIds.productSurgeonProMonthly,
                KiruRevenueCatIds.entitlementTierPro,
            ).logbookQuota(),
        )
        val max = customerInfo(KiruRevenueCatIds.productSurgeonMaxAnnual, KiruRevenueCatIds.entitlementTierMax)
        val pro = customerInfo(KiruRevenueCatIds.productSurgeonProMonthly, KiruRevenueCatIds.entitlementTierPro)
        assertEquals(pro.logbookQuota(), max.logbookQuota())
        assertEquals(FeatureQuota.Unmetered, max.logbookQuota())
    }

    @Test
    fun `inactive product and unresolved state stay on the free quota`() {
        val expired =
            resolveCustomerInfo(
                listOf(
                    EntitlementFact(
                        identifier = KiruRevenueCatIds.entitlementTierPro,
                        productIdentifier = KiruRevenueCatIds.productSurgeonProMonthly,
                        isActive = false,
                    ),
                ),
            )
        assertEquals(FeatureQuota.Limited(LogbookCapacityRule.FREE_ENTRIES), expired.logbookQuota())
        assertEquals(FeatureQuota.Limited(LogbookCapacityRule.FREE_ENTRIES), EntitlementState.Loading.logbookQuota())
        assertEquals(
            FeatureQuota.Limited(LogbookCapacityRule.FREE_ENTRIES),
            EntitlementState.Error("network").logbookQuota(),
        )
    }

    @Test
    fun `review build override is unmetered for every resolved plan`() {
        val plans =
            listOf(
                EntitlementState.Free,
                customerInfo(
                    KiruRevenueCatIds.productResidentEliteMonthly,
                    KiruRevenueCatIds.entitlementTierResident,
                ),
                customerInfo(
                    KiruRevenueCatIds.productSurgeonMegaMonthly,
                    KiruRevenueCatIds.entitlementTierPro,
                ),
                customerInfo(
                    KiruRevenueCatIds.productSurgeonProMonthly,
                    KiruRevenueCatIds.entitlementTierPro,
                ),
            )
        plans.forEach { state ->
            assertEquals(FeatureQuota.Unmetered, state.logbookQuota(unmetered = true))
        }
    }

    private fun customerInfo(
        productId: String,
        entitlementId: String,
    ): EntitlementState =
        resolveCustomerInfo(
            listOf(
                EntitlementFact(
                    identifier = entitlementId,
                    productIdentifier = productId,
                    isActive = true,
                ),
            ),
        )
}
