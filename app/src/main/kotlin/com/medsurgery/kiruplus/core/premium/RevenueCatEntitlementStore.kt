package com.medsurgery.kiruplus.core.premium

import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Singleton

/**
 * CustomerInfo es la autoridad del plan. El listener cubre compras y
 * renovaciones; [refresh] se llama al volver a primer plano.
 * Offering reconocido: [KiruRevenueCatIds.offeringDefault] (`21875673`).
 */
@Singleton
class RevenueCatEntitlementStore {
    private val authority = EntitlementAuthority()
    val state: StateFlow<EntitlementState> = authority.state
    private var listening = false

    fun refresh() {
        if (!Purchases.isConfigured) {
            authority.onNetworkFailure(UNCONFIGURED)
            return
        }
        val purchases = Purchases.sharedInstance
        if (!listening) {
            listening = true
            purchases.updatedCustomerInfoListener = UpdatedCustomerInfoListener { info ->
                authority.onCustomerInfo(info.originalAppUserId, info.toFacts())
            }
        }
        purchases.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                authority.onCustomerInfo(customerInfo.originalAppUserId, customerInfo.toFacts())
            }

            override fun onError(error: PurchasesError) {
                authority.onNetworkFailure(purchases.appUserID)
            }
        })
    }

    private companion object {
        const val UNCONFIGURED = "unconfigured"
    }
}

private fun CustomerInfo.toFacts(): List<EntitlementFact> =
    entitlements.all.values.map { entitlement ->
        EntitlementFact(
            identifier = entitlement.identifier,
            productIdentifier = entitlement.productIdentifier,
            isActive = entitlement.isActive,
        )
    }
