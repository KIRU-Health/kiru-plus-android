package com.medsurgery.kiruplus.core.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerInfoAuthorityTest {

    @Test
    fun `no active entitlements resolve to free`() {
        val state = resolveCustomerInfo(emptyList())
        assertEquals(EntitlementState.Free, state)
    }

    @Test
    fun `active surgeon pro product is premium`() {
        val state = resolveCustomerInfo(
            listOf(
                EntitlementFact(
                    identifier = KiruRevenueCatIds.entitlementTierPro,
                    productIdentifier = KiruRevenueCatIds.productSurgeonProMonthly,
                    isActive = true,
                ),
            ),
        )
        val premium = state as EntitlementState.Premium
        assertEquals(KiruAccessTier.SurgeonPro, premium.tier)
        assertEquals(KiruRevenueCatIds.productSurgeonProMonthly, premium.productId)
    }

    @Test
    fun `expired customer info revokes to free`() {
        val authority = EntitlementAuthority()
        authority.onCustomerInfo(
            "user-1",
            listOf(
                EntitlementFact(
                    identifier = KiruRevenueCatIds.entitlementTierPro,
                    productIdentifier = KiruRevenueCatIds.productSurgeonProMonthly,
                    isActive = true,
                ),
            ),
        )
        authority.onCustomerInfo(
            "user-1",
            listOf(
                EntitlementFact(
                    identifier = KiruRevenueCatIds.entitlementTierPro,
                    productIdentifier = KiruRevenueCatIds.productSurgeonProMonthly,
                    isActive = false,
                ),
            ),
        )
        assertEquals(EntitlementState.Free, authority.state.value)
    }

    @Test
    fun `network failure keeps the last customer info`() {
        val authority = EntitlementAuthority()
        authority.onCustomerInfo(
            "user-1",
            listOf(
                EntitlementFact(
                    identifier = KiruRevenueCatIds.entitlementLegacyApp,
                    productIdentifier = "unknown-sku",
                    isActive = true,
                ),
            ),
        )
        authority.onNetworkFailure("user-1")
        val premium = authority.state.value as EntitlementState.Premium
        assertEquals(KiruAccessTier.SurgeonPro, premium.tier)
        assertTrue(premium.productId == null)
    }

    @Test
    fun `network failure without cache does not grant access`() {
        val authority = EntitlementAuthority()
        authority.onNetworkFailure("user-1")
        assertTrue(authority.state.value is EntitlementState.Error)
    }

    @Test
    fun `offering id matches iOS KiruOffering`() {
        assertEquals("21875673", KiruRevenueCatIds.offeringDefault)
    }
}
