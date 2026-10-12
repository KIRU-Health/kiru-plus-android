package com.medsurgery.kiruplus.core.premium

import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PremiumEntryPoint {
    fun entitlementStore(): RevenueCatEntitlementStore
}

@Module
@InstallIn(SingletonComponent::class)
object PremiumModule {

    @Provides
    @Singleton
    fun provideEntitlementStore(): RevenueCatEntitlementStore = RevenueCatEntitlementStore()

    @Provides
    @Singleton
    fun provideEntitlementStateFlow(
        store: RevenueCatEntitlementStore,
    ): StateFlow<EntitlementState> = store.state
}
