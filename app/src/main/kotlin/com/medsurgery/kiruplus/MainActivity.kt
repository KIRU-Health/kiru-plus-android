package com.medsurgery.kiruplus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.medsurgery.kiruplus.app.App
import com.medsurgery.kiruplus.core.premium.PremiumEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onStart() {
        super.onStart()
        val store = EntryPointAccessors.fromApplication(
            applicationContext,
            PremiumEntryPoint::class.java,
        ).entitlementStore()
        store.refresh()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App() }
    }
}
