package com.medsurgery.kiruplus.domain.logbook

import java.util.Objects

/**
 * Cupo de la bitácora, transcripción de `FeatureQuota` + `LogbookCapacity.swift`.
 * No hay periodo: el tope es cuántos registros pueden existir a la vez.
 */
sealed interface FeatureQuota {
    data object None : FeatureQuota

    data class Limited(val limit: Int) : FeatureQuota

    data object Unmetered : FeatureQuota

    fun remaining(used: Int): Int? =
        when (this) {
            None -> NONE_REMAINING
            is Limited -> (limit - used.coerceAtLeast(0)).coerceAtLeast(0)
            Unmetered -> null
        }

    fun allows(used: Int): Boolean =
        when (this) {
            None -> false
            is Limited -> used < limit
            Unmetered -> true
        }

    val cap: Int?
        get() =
            when (this) {
                None -> NONE_REMAINING
                is Limited -> limit
                Unmetered -> null
            }

    val isBlocked: Boolean
        get() = this == None
}

private const val NONE_REMAINING = 0

enum class LogbookPlan {
    Free,
    ResidentElite,
    SurgeonMega,
    SurgeonPro,
}

class LogbookCapacity(
    val quota: FeatureQuota,
    count: Int,
) {
    val count: Int = count.coerceAtLeast(0)

    val canCreate: Boolean
        get() = quota.allows(count)

    val limit: Int?
        get() = quota.cap

    val remaining: Int?
        get() = quota.remaining(count)

    val isMetered: Boolean
        get() = limit != null

    val isOverCapacity: Boolean
        get() = limit?.let { cap -> count > cap } ?: false

    val reachedMessage: String
        get() {
            val cap = limit
            return when {
                cap == null -> ""
                cap == 0 -> PLAN_EXCLUDES_LOGBOOK
                isOverCapacity -> overCapacityMessage(cap, count)
                else -> atCapacityMessage(cap, count)
            }
        }

    override fun equals(other: Any?): Boolean {
        val rhs = other as? LogbookCapacity ?: return false
        return quota == rhs.quota && count == rhs.count
    }

    override fun hashCode(): Int = Objects.hash(quota, count)

    private companion object {
        const val PLAN_EXCLUDES_LOGBOOK = "Tu plan no incluye la bitácora quirúrgica."
    }
}

sealed interface LogbookSaveOutcome {
    data class Created(val capacity: LogbookCapacity) : LogbookSaveOutcome

    data object Updated : LogbookSaveOutcome

    data class DeniedAtCapacity(val capacity: LogbookCapacity) : LogbookSaveOutcome

    val isPersisted: Boolean
        get() = this !is DeniedAtCapacity
}

object LogbookCapacityRule {
    const val FREE_ENTRIES = 20
    const val ELITE_ENTRIES = 200

    val defaultQuota: FeatureQuota = FeatureQuota.Limited(FREE_ENTRIES)

    fun quota(
        plan: LogbookPlan,
        unmetered: Boolean = false,
    ): FeatureQuota {
        if (unmetered) return FeatureQuota.Unmetered
        return when (plan) {
            LogbookPlan.Free -> FeatureQuota.Limited(FREE_ENTRIES)
            LogbookPlan.ResidentElite -> FeatureQuota.Limited(ELITE_ENTRIES)
            LogbookPlan.SurgeonMega,
            LogbookPlan.SurgeonPro,
            -> FeatureQuota.Unmetered
        }
    }

    fun capacity(
        count: Int,
        quota: FeatureQuota,
    ): LogbookCapacity = LogbookCapacity(quota, count)
}

private fun overCapacityMessage(
    limit: Int,
    count: Int,
): String =
    "Tu plan actual permite $limit registros y tienes $count. " +
        "Se conservan todos; para añadir uno nuevo, elimina hasta quedar " +
        "por debajo de $limit o amplía tu plan."

private fun atCapacityMessage(
    limit: Int,
    count: Int,
): String =
    "Tu plan permite $limit registros en la bitácora y ya tienes $count. " +
        "Elimina alguno o amplía tu plan para registrar más cirugías."
