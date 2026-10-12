# Matriz de paridad iOS → Android — KIRU+

> **Auditoría Fase 0:** 2026-10-10  
> **Fuente de verdad:** código iOS en `kiru-plus-main` (este documento no modifica ese repo).  
> **Android auditado:** `kiru-plus-android` @ `5854421` (`feat(android): Sprint F1–F5 — medical content parity…`) en `main`.  
> **La matriz anterior (2026-05-18, “E0–E10”)** describía un scaffolding que el código actual ya superó (Academy, Quiz, Pearls, Kapibaya, Logbook, K-Cortex, fármacos y casos existen). Quedó sustituida por esta tabla. Ante conflicto entre docs y código, gana el código iOS.

## Cómo se califica

| Estado | Significado |
|---|---|
| CONSERVAR | El comportamiento observable coincide con iOS. Se deja el código. |
| CORREGIR | Hay pantalla o capa útil, con desviación de lógica, contrato, UI o seguridad. |
| REESCRIBIR | La base no puede alinearse con un parche: el origen de verdad está mal. |
| FALTA | El módulo existe en iOS y no tiene equivalente Android. |

**Paridad real** = filas CONSERVAR / filas de esta tabla. Una fila CORREGIR no cuenta como paridad: el usuario no obtiene el mismo resultado.

**Denominador:** 66 módulos de producto (Features iOS + cimientos que cambian el comportamiento: sesión, diseño, premium, monedas, notificaciones, telemetría, legales). No se cuentan carpetas de infraestructura sin flujo propio (`Features/Models`, `Features/Views`, `Features/Shared`, `Features/Navigation` como contenedores).

## Resumen ejecutivo

| Métrica | Valor |
|---|---|
| Módulos en la tabla | 66 |
| CONSERVAR | 1 (1.5%) |
| CORREGIR | 22 (33%) |
| REESCRIBIR | 1 (1.5%) — entitlements |
| FALTA | 42 (64%) |
| **Paridad real** | **1/66 (1.5%)** |
| Superficie con código (CONSERVAR + CORREGIR) | 23/66 ≈ 35% |

**Recomendación: conservar la base actual. No reestructurar.**

Justificación técnica, no de preferencia:

- El módulo único `:app` ya separa `app/` (composition root), `core/`, `domain/`, `data/` y `feature/<nombre>/` con Screen + ViewModel + Hilt. Eso es el espejo conceptual de `App/CompositionRoot/AppBootstrapper.swift` + MVVM + Repository. Partir en `:core:*` / `:feature:*` ahora no corrige ningún contrato y obliga a reescribir DI, tests y navegación antes de tener paridad.
- El cliente usa la anon key, `alwaysAutoRefresh` y PostgREST bajo RLS. No hay service role en el cliente. `allowBackup=false`, `usesCleartextTraffic=false`, R8 activo en release.
- RevenueCat está en el classpath y `PaywallScreen` usa `PaywallDialog`. Lo que está mal es el origen del plan (`PremiumModule` emite `Free` siempre), no la forma del proyecto.
- Los fallos graves son de contrato (borrado de cuenta, Kapibaya, catálogo de tienda, bancos de preguntas) y de cobertura (42 módulos ausentes). Se corrigen feature por feature sobre esta base.

**Propuesta de módulos Gradle (no ejecutar sin visto bueno):** cuando pasen de ~8 features con paridad real, extraer `:core:designsystem`, `:core:network`, `:core:premium` y dejar `:app` como composition root. Hasta entonces, un solo módulo compila más rápido y el CI actual ya apunta a `:app`.

### Top 5 riesgos

1. **Dinero.** `PremiumModule` es un stub que publica `EntitlementState.Free` siempre. RevenueCat se configura en `KiruApp` si hay `goog_` key, pero nadie lee CustomerInfo. El plan nunca puede volverse Pro. Bloqueante de paridad de dinero.
2. **Borrado de cuenta.** Android invoca la Edge Function `process_account_deletions`. En iOS el usuario llama al RPC `kiru_request_account_deletion`. Esa función es un worker solo de credencial de máquina: rechaza el JWT del usuario (`machine_credential_required`). El botón de Android no agenda la baja de 48 h que hace iOS. Tampoco existen `kiru_cancel_account_deletion` ni `kiru_account_deletion_status`.
3. **Kapibaya.** iOS hace SSE contra `functions/v1/ask_kapibaya` con la sesión. Android llama `ask_kapibaya_stream` y manda solo `apikey`, sin `Authorization: Bearer`. El cuerpo (`message`, `conversationId`, `userId`) no está verificado contra el payload Swift. No hay TTS (`kapibaya-tts`) ni voz.
4. **Contenido clínico no demostrado idéntico.** El quiz Android lee `content_items` con `category=question`. El banco que ve el alumno en iOS sale del bundle (`PublishedBankLoadTests`: “content_items no trae preguntas”). Fármacos y casos salen de `drugs.json` / `clinical_cases.json` embebidos, no del camino K-Pharma (`content_items` `cbm_bundle` + bundle local de 687 fármacos). No se inventó texto en esta auditoría; tampoco se comprobó que los JSON coincidan byte a byte con iOS.
5. **Zero-crash y CI incompletos.** Los `!!` de `app/src/main` se quitaron en la rama de fundamentos. Siguen en tests. El README declara detekt y ktlint; no están en `app/build.gradle.kts` ni en `.github/workflows/android-ci.yml`. CI puede quedar verde incumpliendo la política. El logbook Android persiste en `surgical_logs` (tabla real, RLS propio) mientras iOS guarda `surgical_logbook.json` local: cambia dónde vive un dato de salud.

### Hallazgos de calidad transversales

- **Arquitectura:** MVVM + Repository + Hilt, sí. Casos de uso puros, casi no: los ViewModels llaman repositorios directo. Aceptable como espejo del iOS actual, que tampoco tiene una capa UseCase uniforme.
- **Sesión:** `alwaysAutoRefresh = true` y `autoLoadFromStorage = true`. No hay Sign in with Apple ni Google. `AuthRepositoryImpl` registra el email en el label de Timber (`signIn $email`).
- **Entornos:** solo `debug` y `release`. iOS tiene Debug / Staging / Release (`Config/Flags-*.xcconfig`). Debug iOS fija `SUPABASE_PROJECT_REF = tttxmupjteqpljtfgmgo` y `APP_ENVIRONMENT = production`. Android debug usa el mismo default de URL. No hay panel que muestre el project ref (iOS lo muestra en `DebugSandboxSpikeView`).
- **Secretos:** no hay service role ni `local.properties` en git. `*.jks` está en `.gitignore`. En disco local sí existe `kiru-plus-release.jks` (no trackeado). No commitear.
- **Design system:** hex de marca alineados con los comentarios de `KiruDesignTokens.swift` y con el asset `KiruLightBlue` (`#00004C`). Falta `KiruSurgicalSuccess` = `Color(red = 0.13f, green = 0.75f, blue = 0.42f)`. `CasesScreen` hardcodea `Color(0xFF…)`. No hay `KiruSpacing` ni `KiruShapes`. Tipografía de `Type.kt` copia tamaños iOS (40/32/24/20/19) sobre `FontFamily.SansSerif`, sin Dynamic Type equivalente.
- **Strings:** `values/` (en) y `values-es/`. iOS tiene `es.lproj`, no `es-MX`. La primaria debe seguir a iOS: `es`. Muchas cadenas de premium y de casos están en el composable, no en recursos.
- **Tienda:** iOS arma el checkout con la Edge Function `medsurgery-store` (WooCommerce). Android lee la tabla `store_products`, que existe en el baseline SQL pero no es el catálogo que muestra iOS.
- **K-Cortex:** el host `https://kiru-backend.onrender.com/v1/kcortex/analyze` es el mismo `BACKEND_BASE_URL` de `Flags-Debug.xcconfig` y el mismo path que `KCortexVisionService`. Desvío: URL fija (no por entorno), solo texto (iOS manda imagen), sin `/v1/kcortex/access`, y los prompts viven copiados en Kotlin.
- **Exportación de datos:** `process_data_export` sí es la misma Edge Function que llama `PrivacyService` en iOS. El borrado, no.

## Tabla única

| Módulo iOS (ruta) | Equivalente Android (ruta) | Estado | Backend usado | Adaptación de plataforma | Prioridad | Notas |
|---|---|---|---|---|---|---|
| `App/CompositionRoot/AppBootstrapper.swift` | `app/`, `KiruApp.kt`, módulos Hilt en `core/` y `data/` | CORREGIR | Supabase anon + RevenueCat + Sentry | Hilt en lugar de composition root Swift | P0 | Un solo módulo `:app`. Build types `debug` / `staging` / `release` con project ref en Ajustes (`SHOW_ENVIRONMENT_PANEL`). Sigue sin módulos Gradle. |
| `Core/DesignSystem/` (`KiruDesignTokens`, `KiruTypography`, `PremiumTheme`) | `core/designsystem/` (`KiruTokens.kt`, `Color.kt`, `Type.kt`, `Theme.kt`) | CORREGIR | — | Material 3 `ColorScheme` | P0 | Tokens de color, spacing y radios ya existen, incluido el verde quirúrgico. La mayoría de pantallas sigue con `dp` y `MaterialTheme` sueltos. |
| `Core/Session/` + auth email (`Data/Services/StudyContent/AuthService.swift`) | `core/auth/`, `data/repository/AuthRepositoryImpl.kt` | CORREGIR | Auth Supabase, refresh automático | DataStore de supabase-kt (Keychain en iOS) | P0 | Login, registro, reset y logout existen. Sin Apple/Google. Loguea el email. |
| `Core/Payment/EntitlementsService.swift` (`get_premium_status` + RevenueCat) | `core/premium/PremiumModule.kt` | REESCRIBIR | Ninguno (stub) | RevenueCat Purchases está instalado y no conectado al estado | P0 | `MutableStateFlow(EntitlementState.Free)` fijo. El plan no puede ser Pro. |
| `Features/Paywall/` | `feature/paywall/PaywallScreen.kt` | CORREGIR | RevenueCat `PaywallDialog` | Play Billing vía RevenueCat UI | P0 | El diálogo es el camino correcto. Sin offerings propios, restore, Customer Center ni gate real. |
| `Core/Rewards/CoinsService.swift` | — | FALTA | RPC `kiru_coins_balance`, `redeem_reward`, Edge `kiru-coins-award` | — | P0 | Economía de KIRU Coins ausente. |
| `Core/Notifications/` + `APNsSubscriptionRepository` | permiso declarado en el manifest | FALTA | RPC `kiru_push_opt_in` / `kiru_push_opt_out` | FCM aún no | P1 | Sin canales, sin deep links, sin opt-in. |
| `Telemetry/` + Sentry iOS | `KiruApp.installSentry` | CORREGIR | Sentry DSN por BuildConfig | SDK Android | P1 | Opt-in por build, no por ajuste. Sin tags de entorno equivalentes. Sin `analytics_events`. |
| `Core/Configuration/KiruLinks.swift` | `BuildConfig` PRIVACY/TERMS/SUBSCRIPTIONS + `core/ui/WebViewScreen.kt` | CONSERVAR | URLs `medsurgery.academy` | WebView / Custom Tabs | P0 | Las tres URLs coinciden con el contrato iOS (`politica-de-privacidad-kiru-app`, términos, suscripciones). |
| `Features/Onboarding/MedicalDisclaimerView.swift` + splash | `feature/onboarding/` | CORREGIR | — | Compose | P0 | Splash y disclaimer con scroll-gate. Falta el onboarding de perfil (gamertag, `user_profiles`). |
| `Features/Profile/` | `feature/profile/` | CORREGIR | Tabla `profiles` (la misma que `UserProfileViewModel`) | — | P0 | No usa `user_profiles` (gamertag, gate). Sin edición rica ni stats. Borrado de cuenta: ver riesgo 2. |
| `Features/Settings/` | `feature/settings/` | CORREGIR | DataStore | Locale por app | P0 | Tema, idioma es/en, háptica, about, export. Sin preferencias de notificación ni bloqueo biométrico (la dependencia está, no el flujo). |
| `Features/Dashboard/` | `feature/home/HomeScreen.kt` | CORREGIR | — | Compose | P0 | Welcome + accesos rápidos. No es el hero, arena ni perlas del dashboard iOS. |
| `Features/Navigation/MainTabView.swift` | `feature/main/`, `app/nav/KiruRoute.kt` | CORREGIR | — | Navigation Compose tipado | P0 | 5 tabs (Home, Academy, Logbook, Store, Profile). iOS reparte más destinos. ~30 rutas vs el grafo iOS. |
| `Features/Academy/` | `feature/academy/` | CORREGIR | `content_items` | — | P1 | Lista y detalle. No es el currículo, cursos ni `ChapterExamView` de iOS. |
| `Features/Quiz/` | `feature/quiz/` | CORREGIR | Asset `preguntas/Questions.json`, copia byte a byte de `Data/preguntas/Questions.json` | — | P1 | Mismo banco publicado (1450). Falta repetición de fallos y `record_quiz_evaluation`. |
| `Features/Exam/` + `Features/Academy/ChapterExamView.swift` | `feature/chapterexam/` | CORREGIR | Asset local de examen | — | P1 | No es `EvaluationViewModel` ni el examen de capítulo remoto. |
| `Features/FlashCards/` | — | FALTA | Banco local / repetición espaciada | Room cuando se porte | P1 | Incluye adivina-diagnóstico. |
| `Features/Resumenes/` | — | FALTA | RPC `kiru_resumen_payload`, vistas `vw_kiru_resumenes_*` | — | P1 | La biblioteca Android no lee esas vistas. |
| `Features/Models/Pearls/` | `feature/pearls/`, `data/pearls/` | CORREGIR | Tabla `pearls` | — | P1 | iOS también reconstruye perlas desde `content_items`. Falta la sesión de estudio. |
| `Features/Study/` | `feature/library/` | CORREGIR | Assets `StudyModules.json` + índice de currículo | — | P1 | No es `StudyLibraryView` ni los libros de resúmenes remotos. |
| `Features/ClinicalCases/` | `feature/cases/` | CORREGIR | Asset `clinical_cases.json` | — | P1 | Hub y detalle. Falta el flujo serializado con resultados de iOS. |
| `Features/Guidelines/` | — | FALTA | Contenido local / remoto | — | P2 | |
| `Features/MedicalCatalog/` | — | FALTA | `medical_diagnoses_cie10`, `medical_procedures_cie9` | — | P2 | |
| `Features/MedicalText/` | — | FALTA | `medical_books`, `medical_chapters` | — | P2 | |
| `Features/Models/Podcast/` | — | FALTA | Audio de estudio | Media3 | P2 | |
| `Features/Kapibaya/` | `feature/kapibaya/`, `data/kapibaya/` | CORREGIR | Android: `ask_kapibaya_stream`. iOS: `ask_kapibaya` | OkHttp SSE (no supabase-kt Functions) | P1 | Sin Bearer, sin TTS, sin voz, sin batalla, sin citas de resúmenes. |
| `Features/K-Cortex/` | `feature/kcortex/`, `data/kcortex/` | CORREGIR | `POST /v1/kcortex/analyze`; host = `BuildConfig.BACKEND_BASE_URL` | Multipart OkHttp, solo texto | P1 | Mismo path que iOS. Release queda vacío (fail-closed). Sin visión, sin `/access`. Prompts duplicados en Kotlin. |
| `Features/K-Pharma/` | `feature/drugs/` | CORREGIR | Asset `drugs.json` | — | P1 | iOS: `content_items` `cbm_bundle` con fallback local. Sin calculadora de dosis pediátrica. |
| `Features/Dictation/` | — | FALTA | `TranscriptionRepository` | `SpeechRecognizer` | P2 | |
| `Features/Rewards/` | — | FALTA | `reward_catalog`, `user_reward_inventory`, RPC `redeem_reward` | Play no sustituye las coins | P1 | |
| `Features/Gamificacion/` | — | FALTA | RPC de hospital battle | — | P2 | |
| `Features/Prestige/` | — | FALTA | Local + remoto de rangos | — | P2 | |
| `Features/Duel/` | — | FALTA | Tiempo real | Realtime de supabase-kt | P2 | |
| `Features/Multiplayer/` | — | FALTA | Leaderboard | — | P2 | |
| `Features/KiruPardy/` | — | FALTA | `kirupardy_content_packages`, Edge `kirupardy-room` | — | P2 | |
| `Features/Arena_KIRU_Plus/` | — | FALTA | RPC `get_elite_league_season`, `arena_elo` | — | P2 | |
| `Features/Games/` | — | FALTA | `GameSessionStatsRepository` | — | P2 | |
| `Features/Progress/` | — | FALTA | Progreso de aprendizaje | — | P1 | Readiness score ausente. |
| `Features/VaultReader/` | — | FALTA | Edge `vault-*` | PdfRenderer + `FLAG_SECURE` | P1 | iOS lo apaga en Debug plano (`VAULT_READER_ENABLED=NO`) y lo enciende en Staging. |
| `Features/Kiru_Video_Plus/` | — | FALTA | Edge `video-catalog`, `video-playback` | Media3 ExoPlayer HLS | P1 | |
| `Features/Calculators/` | `feature/ktools/`, `domain/ktools/KToolsCalculator.kt` | CORREGIR | Cálculo local | — | P1 | 10 de 12: faltan ISS y Trauma Score. Fórmulas no re-verificadas contra los ViewModels Swift en esta fase. |
| `Features/NOM004/` | — | FALTA | `NOM004ClinicalNotesRepository` | PdfDocument + firma | P2 | |
| `Features/Logbook/` | `feature/logbook/`, `data/logbook/` | CORREGIR | Archivo local cifrado `surgical_logbook.json` | Keystore AES-GCM. Sin `surgical_logs`. | P1 | Mismos campos que `SurgicalLogEntry`. `ingest_surgical_log` sigue fuera: es `SurgicalSafetyLogger`. |
| `Store/` (WooCommerce) | `feature/store/`, `data/store/` | CORREGIR | Android: `store_products`. iOS: Edge `medsurgery-store` | Custom Tabs en iOS y en Android para el checkout web | P1 | Catálogo distinto. Sin carrito. |
| `Features/QuirofanoAR/` | — | FALTA | Escenarios locales | ARCore, o degradación documentada | P3 | No marcar NO_PORTABLE hasta probar SceneView. |
| `Features/AR/` | — | FALTA | — | ARCore | P3 | |
| `Features/ARSimulation/` | — | FALTA | — | ARCore | P3 | |
| `Features/KLens/` | — | FALTA | — | ML Kit / CameraX | P3 | |
| `Features/KiruVision/` | — | FALTA | — | CameraX | P3 | |
| `Features/KiruSimulator/` | — | FALTA | — | — | P3 | |
| `Features/Institutional/` | — | FALTA | Cohortes, residentes | — | P3 | Producto B2B, no el flujo del alumno. |
| `Features/KIRUControl/` | — | FALTA | RPC `get_kiru_control_dashboard`, Edge `kiru-control-demo-redeem` | — | P3 | |
| `Features/Alerts/` | — | FALTA | `wellbeing_alerts` | — | P3 | |
| `Features/Reports/` | — | FALTA | — | — | P3 | |
| `Features/Surgical/` | — | FALTA | Consentimiento y seguridad quirúrgica | — | P2 | Distinto del logbook. |
| `Features/Compliance/` | — | FALTA | Repositorio fiscal | — | P2 | Los enlaces legales sí están (fila KiruLinks). |
| `Features/License/` | — | FALTA | Edge `issue_license_signed_url` | Photo Picker | P2 | |
| `Features/Admin/` | — | FALTA | Instituciones | — | P3 | No es superficie del alumno. |
| `Features/ScrubMode/` | — | FALTA | — | — | P3 | |
| `Features/Blockchain/` | — | FALTA | Diploma | — | P3 | |
| `Features/Avatar/` | — | FALTA | — | — | P3 | |
| `Features/ContentModes/` | — | FALTA | — | — | P3 | |
| `Features/Validation/` | — | FALTA | `medical_validations` | — | P2 | |
| `Features/Analysis/` | — | FALTA | — | — | P3 | |
| `Features/Stats/` | — | FALTA | — | Vico cuando haya series | P2 | |

## Inventario Android que no es un módulo iOS nuevo

Estas rutas se conservan; no son features de más, son el lugar donde cayó un módulo iOS:

`feature/auth`, `feature/legal`, `feature/main`, `core/network/SupabaseModule.kt`, `core/prefs`, `core/assets`, `core/locale`, `core/browser`.

## Excepciones de nombre (el concepto no se llama igual)

| iOS | Android | Motivo |
|---|---|---|
| `UserProfileViewModel` | `ProfileViewModel` | La pantalla se llama Profile en ambos; alinear el nombre en Fase 1. |
| `ChapterExamView` | `ChapterExamScreen` / `ChapterExamViewModel` | Convención Compose (`Screen`). El ViewModel sí es gemelo. |
| `KPharmaHubView` | `DrugsScreen` | El módulo iOS es K-Pharma; Android lo nombró drugs. Renombrar a `KPharma*` al corregir el contrato. |
| `HomeDashboardView` | `HomeScreen` | Mismo destino, otro nombre. |
| `MedSurgeryStoreService` | `StoreRepository` | Mismo destino comercial; la fuente de datos todavía no es la misma. |
| `AppBootstrapper` | módulos Hilt (`SupabaseModule`, `PremiumModule`, bindings por repositorio) | No hay una clase `AppBootstrapper`. La DI está centralizada, repartida. |

## Verificación de build (2026-10-10)

Comandos, JDK 21 de Android Studio, Gradle 8.11.1, sobre el árbol de `5854421` (antes de este documento):

| Comando | Resultado |
|---|---|
| `./gradlew assembleDebug` | **PASS** (tarea `:app:assembleDebug` terminó; el APK debug se empaquetó) |
| `./gradlew test` | **PASS** — `testDebugUnitTest` 318 tests, 0 fallos, 0 errores, 0 omitidos (36 clases). `testReleaseUnitTest` también pasó dentro del mismo `test`. |
| `./gradlew lint` (`lintDebug`) | **FAIL** — 3 errores, 0 warnings. Los tres son el mismo hallazgo `RiskyLibrary`: `com.revenuecat.purchases:purchases` **10.6.1** (`gradle/libs.versions.toml:39`). Lint cita que esa versión hace llamadas de API innecesarias y **bloquea la publicación en Play Console**. No se tocó el catálogo en Fase 0. |

El fallo de lint no cambia la recomendación de conservar la base: es un bump de dependencia, no un rediseño. `detekt` y `ktlint` no están en el build, así que no se ejecutaron.
