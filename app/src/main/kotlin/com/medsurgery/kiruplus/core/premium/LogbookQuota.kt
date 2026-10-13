package com.medsurgery.kiruplus.core.premium

import com.medsurgery.kiruplus.domain.logbook.FeatureQuota
import com.medsurgery.kiruplus.domain.logbook.LogbookCapacityRule
import com.medsurgery.kiruplus.domain.logbook.LogbookPlan

/**
 * El plan sale de CustomerInfo ya resuelto. Loading, error y Free quedan
 * en el cupo fail-closed. `unmetered` es el build de revisión de iOS;
 * Android no lo enciende: la composición lee el plan real.
 */
fun EntitlementState.logbookPlan(): LogbookPlan =
    when (this) {
        is EntitlementState.Premium ->
            when (tier) {
                KiruAccessTier.ResidentElite -> LogbookPlan.ResidentElite
                KiruAccessTier.SurgeonMega -> LogbookPlan.SurgeonMega
                KiruAccessTier.SurgeonPro -> LogbookPlan.SurgeonPro
            }
        EntitlementState.Free,
        EntitlementState.Loading,
        is EntitlementState.Error,
        -> LogbookPlan.Free
    }

fun EntitlementState.logbookQuota(unmetered: Boolean = false): FeatureQuota =
    LogbookCapacityRule.quota(
        logbookPlan(),
        unmetered,
    )
