package com.medsurgery.kiruplus.core.premium

/**
 * Current subscription entitlement for the authenticated user.
 *
 * Named `Premium` (not `Pro`) deliberately: entitlement names are technical
 * identifiers, decoupled from marketing names. If the product tier is ever
 * sold as "KIRU+ Pro", "KIRU+ Plus", or anything else, this enum stays stable
 * and avoids naming drift between code and App Store / Play Store listings.
 *
 * The Android implementation resolves this state via RevenueCat once a valid
 * `goog_` API key is configured (Sprint B / task A7).
 * Until then, [EntitlementState.Free] is the safe default — no content is
 * ever unlocked erroneously.
 */
sealed interface EntitlementState {

    /** No active subscription. Show upgrade prompts for premium content. */
    data object Free : EntitlementState

    /**
     * Plan de pago activo. El nivel sale del CustomerInfo, con el mismo
     * orden que iOS: resident < mega < pro. Max y Pro comparten nivel.
     */
    data class Premium(
        val tier: KiruAccessTier,
        val productId: String?,
    ) : EntitlementState

    /**
     * RevenueCat SDK has not yet returned a result (e.g., first launch before
     * network call completes). Treat as [Free] for gating purposes until resolved.
     */
    data object Loading : EntitlementState

    /**
     * RevenueCat returned an error (invalid key, network failure, etc.).
     * Treat as [Free] — never grant access on error.
     */
    data class Error(val message: String) : EntitlementState
}

/** Returns true only when the user has an active premium subscription. */
val EntitlementState.isPremium: Boolean
    get() = this is EntitlementState.Premium

/** Returns true when the state is still being resolved. */
val EntitlementState.isLoading: Boolean
    get() = this is EntitlementState.Loading
