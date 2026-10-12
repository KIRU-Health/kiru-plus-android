package com.medsurgery.kiruplus.core.premium

/**
 * Identificadores copiados del código iOS, no inventados.
 * `KiruEntitlement` y `KiruOffering` en
 * `Data/Services/StudyContent/KiruEntitlement.swift`,
 * `AccessProductCatalog` en `Core/Payment/AccessTier.swift`.
 */
object KiruRevenueCatIds {
    const val entitlementTierPro = "tier_pro"
    const val entitlementTierMax = "tier_max"
    const val entitlementTierResident = "tier_resident"
    const val entitlementLegacyApp = "KIRU+ App"
    const val offeringDefault = "21875673"
    const val productResidentEliteMonthly = "kiru_resident_elite_monthly"
    const val productSurgeonProMonthly = "kiru_surgeon_pro_monthly"
    const val productSurgeonMaxAnnual = "kiru_surgeon_max_annual"
    const val productSurgeonMegaMonthly = "kiru_surgeon_mega_monthly"
}

enum class KiruAccessTier {
    ResidentElite,
    SurgeonMega,
    SurgeonPro,
}

data class EntitlementFact(
    val identifier: String,
    val productIdentifier: String,
    val isActive: Boolean,
)

/**
 * Misma resolución que `AccessTier.resolveIdentity` + `legacyEntitlementTier`:
 * primero el product ID, y solo si no hay SKU reconocido el identificador
 * de entitlement. Un CustomerInfo sin activos es Free (expirado confirmado).
 */
fun resolveCustomerInfo(facts: List<EntitlementFact>): EntitlementState {
    val active = facts.filter { it.isActive }
    val byProduct = resolveByProduct(active.map { it.productIdentifier }.toSet())
    if (byProduct != null) return byProduct
    val legacy = legacyTier(active) ?: return EntitlementState.Free
    return EntitlementState.Premium(tier = legacy, productId = null)
}

private fun resolveByProduct(productIds: Set<String>): EntitlementState.Premium? {
    val recognized = productIds.mapNotNull { id ->
        val tier = tierForProduct(id) ?: return@mapNotNull null
        id to tier
    }
    val winning = recognized.maxByOrNull { it.second.rank } ?: return null
    val atWinning = recognized.filter { it.second == winning.second }
    val productId = if (atWinning.size == 1) atWinning.first().first else null
    return EntitlementState.Premium(tier = winning.second, productId = productId)
}

private fun tierForProduct(productId: String): KiruAccessTier? = when (productId) {
    KiruRevenueCatIds.productResidentEliteMonthly -> KiruAccessTier.ResidentElite
    KiruRevenueCatIds.productSurgeonMegaMonthly -> KiruAccessTier.SurgeonMega
    KiruRevenueCatIds.productSurgeonProMonthly,
    KiruRevenueCatIds.productSurgeonMaxAnnual,
    -> KiruAccessTier.SurgeonPro
    else -> null
}

private fun legacyTier(active: List<EntitlementFact>): KiruAccessTier? {
    val ids = active.map { it.identifier }.toSet()
    return when {
        KiruRevenueCatIds.entitlementTierPro in ids -> KiruAccessTier.SurgeonPro
        KiruRevenueCatIds.entitlementTierMax in ids -> KiruAccessTier.SurgeonPro
        KiruRevenueCatIds.entitlementTierResident in ids -> KiruAccessTier.ResidentElite
        KiruRevenueCatIds.entitlementLegacyApp in ids -> KiruAccessTier.SurgeonPro
        else -> null
    }
}

private val KiruAccessTier.rank: Int
    get() = when (this) {
        KiruAccessTier.ResidentElite -> 1
        KiruAccessTier.SurgeonMega -> 2
        KiruAccessTier.SurgeonPro -> 3
    }

/**
 * Caché del último CustomerInfo. Un fallo de red no revoca el plan.
 * Un cambio de App User ID tira la caché (RevenueCat 10.22.1: no aplicar
 * el CustomerInfo del usuario anterior).
 */
class EntitlementAuthority {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow<EntitlementState>(EntitlementState.Loading)
    val state: kotlinx.coroutines.flow.StateFlow<EntitlementState> = _state

    private var cachedFacts: List<EntitlementFact>? = null
    private var cachedUserId: String? = null

    fun onCustomerInfo(appUserId: String, facts: List<EntitlementFact>) {
        if (cachedUserId != null && cachedUserId != appUserId) {
            cachedFacts = null
        }
        cachedUserId = appUserId
        cachedFacts = facts
        _state.value = resolveCustomerInfo(facts)
    }

    fun onNetworkFailure(appUserId: String) {
        if (cachedUserId != null && cachedUserId != appUserId) {
            cachedFacts = null
            cachedUserId = appUserId
            _state.value = EntitlementState.Error("identity_changed")
            return
        }
        val cached = cachedFacts
        _state.value = if (cached != null) {
            resolveCustomerInfo(cached)
        } else {
            EntitlementState.Error("network")
        }
    }
}
