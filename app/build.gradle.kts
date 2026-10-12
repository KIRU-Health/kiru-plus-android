import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktlint)
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    baseline = file("$rootDir/config/detekt/baseline.xml")
}

dependencies {
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
}

ktlint {
    android.set(true)
    baseline.set(file("$rootDir/config/ktlint/baseline.xml"))
    filter {
        exclude("**/*.kts")
    }
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun localProperty(key: String, fallback: String = ""): String =
    localProperties.getProperty(key) ?: System.getenv(key) ?: fallback

android {
    namespace = "com.medsurgery.kiruplus"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.medsurgery.kiruplus"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 51
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        buildConfigField(
            "String",
            "SUPABASE_URL",
            "\"${localProperty("SUPABASE_URL", "https://tttxmupjteqpljtfgmgo.supabase.co")}\"",
        )
        buildConfigField(
            "String",
            "SUPABASE_ANON_KEY",
            "\"${localProperty("SUPABASE_ANON_KEY")}\"",
        )
        buildConfigField(
            "String",
            "REVENUECAT_API_KEY",
            "\"${localProperty("REVENUECAT_API_KEY")}\"",
        )
        buildConfigField(
            "String",
            "SENTRY_DSN",
            "\"${localProperty("SENTRY_DSN")}\"",
        )
        buildConfigField(
            "String",
            "PRIVACY_POLICY_URL",
            "\"https://www.medsurgery.academy/politica-de-privacidad-kiru-app\"",
        )
        buildConfigField(
            "String",
            "TERMS_URL",
            "\"https://www.medsurgery.academy/terminos-y-condiciones-kiru-app\"",
        )
        buildConfigField(
            "String",
            "SUBSCRIPTIONS_POLICY_URL",
            "\"https://www.medsurgery.academy/politica-de-suscripciones-kiru-app\"",
        )
        buildConfigField(
            "String",
            "SUPPORT_WEBSITE_URL",
            "\"https://medsurgery.academy/kiru\"",
        )
        buildConfigField(
            "String",
            "WEBSITE_URL",
            "\"https://medsurgery.academy\"",
        )

        resourceConfigurations += setOf("en", "es")
    }

    signingConfigs {
        create("release") {
            storeFile     = file(localProperty("KEYSTORE_PATH", "../kiru-plus-release.jks"))
            storePassword = localProperty("KEYSTORE_PASSWORD")
            keyAlias      = localProperty("KEY_ALIAS", "kiru-plus")
            keyPassword   = localProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            // Flags-Debug.xcconfig: el canal es local-debug y el project ref
            // efectivo es producción, salvo override en local.properties.
            buildConfigField("String", "APP_ENVIRONMENT", "\"${localProperty("APP_ENVIRONMENT", "production")}\"")
            buildConfigField("String", "SUPABASE_PROJECT_REF", "\"${localProperty("SUPABASE_PROJECT_REF", "tttxmupjteqpljtfgmgo")}\"")
            buildConfigField("String", "KIRU_BUILD_CHANNEL", "\"local-debug\"")
            buildConfigField("String", "BACKEND_BASE_URL", "\"${localProperty("BACKEND_BASE_URL", "https://kiru-backend.onrender.com")}\"")
            buildConfigField("boolean", "SHOW_ENVIRONMENT_PANEL", "true")
        }
        create("staging") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            matchingFallbacks += listOf("debug")
            // Flags-Staging.xcconfig. La anon key no tiene default: vacía = fail-closed.
            buildConfigField("String", "APP_ENVIRONMENT", "\"${localProperty("STAGING_APP_ENVIRONMENT", "staging")}\"")
            buildConfigField("String", "SUPABASE_URL", "\"${localProperty("STAGING_SUPABASE_URL", "https://huzivubxotwvwnimasng.supabase.co")}\"")
            buildConfigField("String", "SUPABASE_ANON_KEY", "\"${localProperty("STAGING_SUPABASE_ANON_KEY")}\"")
            buildConfigField("String", "SUPABASE_PROJECT_REF", "\"${localProperty("STAGING_SUPABASE_PROJECT_REF", "huzivubxotwvwnimasng")}\"")
            buildConfigField("String", "KIRU_BUILD_CHANNEL", "\"local-staging\"")
            buildConfigField("String", "BACKEND_BASE_URL", "\"${localProperty("STAGING_BACKEND_BASE_URL", "https://kiru-backend-staging.onrender.com")}\"")
            buildConfigField("boolean", "SHOW_ENVIRONMENT_PANEL", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
            // Flags-Release.xcconfig: K-CORTEX de producción queda vacío (fail-closed).
            buildConfigField("String", "APP_ENVIRONMENT", "\"${localProperty("APP_ENVIRONMENT", "production")}\"")
            buildConfigField("String", "SUPABASE_PROJECT_REF", "\"${localProperty("SUPABASE_PROJECT_REF", "tttxmupjteqpljtfgmgo")}\"")
            buildConfigField("String", "KIRU_BUILD_CHANNEL", "\"release\"")
            buildConfigField("String", "BACKEND_BASE_URL", "\"${localProperty("RELEASE_BACKEND_BASE_URL", "")}\"")
            buildConfigField("boolean", "SHOW_ENVIRONMENT_PANEL", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/INDEX.LIST",
            )
        }
    }

    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin")
        }
        getByName("test") {
            kotlin.srcDirs("src/test/kotlin")
        }
        getByName("androidTest") {
            kotlin.srcDirs("src/androidTest/kotlin")
        }
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.google.material)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.browser)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose.ui)
    debugImplementation(libs.bundles.compose.debug)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Coroutines + serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)

    // Supabase
    implementation(platform(libs.supabase.bom))
    implementation(libs.bundles.supabase)
    implementation(libs.ktor.client.android)

    // RevenueCat
    implementation(libs.revenuecat.purchases)
    implementation(libs.revenuecat.purchases.ui)

    // Networking helper
    implementation(libs.okhttp)

    // Image loading
    implementation(libs.coil.compose)

    // Telemetry
    implementation(libs.bundles.sentry)
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
