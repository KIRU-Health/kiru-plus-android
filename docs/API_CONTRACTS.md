# API Contracts — KIRU+ Shared Backend

**Last updated:** 2026-05-24  
**Supabase project:** `tttxmupjteqpljtfgmgo.supabase.co`  
**Owner:** MedSurgery Technology

> This file is the canonical reference for every backend call made by the Android client.  
> **Do not change Edge Function signatures without updating this file and coordinating with iOS.**

---

## Authentication

All requests use the Supabase session JWT automatically injected by `supabase-kt` via `Authorization: Bearer <jwt>`.  
Public endpoints (e.g., anon read-only) use the publishable anon key via header `apikey: <SUPABASE_ANON_KEY>`.

---

## Edge Functions

### `ask_kapibaya_stream`

Streaming Kapibaya AI chat response (Server-Sent Events).

| Field | Value |
|-------|-------|
| URL | `{SUPABASE_URL}/functions/v1/ask_kapibaya_stream` |
| Method | POST |
| Auth | `apikey: SUPABASE_ANON_KEY` (no JWT required) |
| Content-Type | `application/json` |
| Accept | `text/event-stream` |

**Request body:**
```json
{
  "message": "<user message string>",
  "conversationId": "<uuid string>",
  "userId": "<user id string>"
}
```

**SSE event types:**

| Event field | Payload | Meaning |
|-------------|---------|---------|
| `chunk` | `{"content": "<text>"}` | Partial response text; append to buffer |
| `done` | (no data) | Stream complete |
| `error` | `{"error": "<message>"}` | Hard failure; display error to user |

**Android implementation:** `KapibayaRepositoryImpl.kt` via OkHttp SSE.

---

### `ask_kapibaya`

Non-streaming Kapibaya AI chat (single response). Used as fallback.

| Field | Value |
|-------|-------|
| URL | `{SUPABASE_URL}/functions/v1/ask_kapibaya` |
| Method | POST |
| Auth | Session JWT (auto via supabase-kt) |
| Content-Type | `application/json` |

**Request body:**
```json
{
  "message": "<user message string>",
  "conversationId": "<uuid string>"
}
```

**Response:**
```json
{
  "content": "<full response text>"
}
```

---

### `process_account_deletions`

Worker de borrado definitivo. **No es el contrato que usa iOS para que el usuario pida la baja.**

| Field | Value |
|-------|-------|
| URL | `{SUPABASE_URL}/functions/v1/process_account_deletions` |
| Method | POST |
| Auth | Credencial de máquina. El JWT de usuario se rechaza (`machine_credential_required`). Fuente: `supabase/functions/process_account_deletions/index.ts` |
| Body | (none) |

**iOS:** `Core/Compliance/PrivacyService.swift` llama al RPC `kiru_request_account_deletion` (`p_reason`). Cancelar: `kiru_cancel_account_deletion`. Estado: `kiru_account_deletion_status`.

**Android hoy:** `AuthRepositoryImpl.requestAccountDeletion()` invoca esta Edge Function. Desviación bloqueante, registrada en la matriz. No se cambia el backend desde este repo.

---

### `process_data_export`

Exports all data associated with the authenticated user (GDPR Article 20).

| Field | Value |
|-------|-------|
| URL | `{SUPABASE_URL}/functions/v1/process_data_export` |
| Method | POST |
| Auth | Session JWT (required) |
| Body | (none) |

**Response:** HTTP 200 with export payload or download link; HTTP 4xx/5xx on failure.

**Android implementation:** `AuthRepositoryImpl.requestDataExport()` via `supabase.functions.invoke(...)`.

---

### `revenuecat_webhook`

Receives purchase validation events from RevenueCat (server-to-server, not called by the app directly).

| Field | Value |
|-------|-------|
| URL | `{SUPABASE_URL}/functions/v1/revenuecat_webhook` |
| Method | POST |
| Auth | RevenueCat webhook secret (header `Authorization`) |
| Caller | RevenueCat backend, NOT the Android/iOS app |

**Not called by Android client directly.** Listed here for completeness and backend change awareness.

---

## Supabase Postgrest (REST API)

### `store_products` table

Used by `StoreRepositoryImpl` to populate the Store catalog.

| Field | Value |
|-------|-------|
| URL | `{SUPABASE_URL}/rest/v1/store_products` |
| Method | GET |
| Auth | Session JWT (RLS gated — requires authenticated user) |
| Filters | `select=*`, `order=sort_order.asc` |

**Columns read by Android:** `id`, `title`, `subtitle`, `price_display`, `currency`, `image_url`, `stripe_payment_link`, `product_type`, `sort_order`.

---

## Version freeze policy

- **Breaking change** (rename/remove field, change auth model): requires dual-platform coordinated deploy. Never break a deployed field.
- **Additive change** (new optional field): safe to deploy; both platforms ignore unknown fields.
- **New function**: always additive; old clients unaffected.
- **During iOS App Review**: backend is frozen. No migrations, no function deploys, no RLS changes. Coordinate with iOS owner before any action.

## Contratos verificados contra Swift (2026-10-10)

Si esta sección contradice una sección de arriba, gana el Swift. Android todavía no está alineado en las filas marcadas DESVIACIÓN.

| Contrato | Archivo Swift | Android hoy |
|---|---|---|
| SSE de Kapibaya: `POST /functions/v1/ask_kapibaya` con sesión | `Data/Services/CloudKapibayaService+SSE.swift` | DESVIACIÓN: `ask_kapibaya_stream` solo con `apikey`, sin `Authorization` (`data/kapibaya/KapibayaRepositoryImpl.kt`) |
| RPC `kiru_request_account_deletion` | `Core/Compliance/PrivacyService.swift` | DESVIACIÓN: invoca `process_account_deletions` |
| Edge `process_data_export` | `Core/Compliance/PrivacyService.swift` | Igual (`AuthRepositoryImpl.requestDataExport`) |
| `POST {BACKEND_BASE_URL}/v1/kcortex/analyze` | `Data/KCortex/KCortexVisionService.swift`; URL en `Config/Flags-*.xcconfig` | Mismo path. La URL sale de `BuildConfig.BACKEND_BASE_URL`. Release queda vacío (fail-closed), como iOS. |
| Catálogo comercial: Edge `medsurgery-store` | `Store/MedStoreAPIConfig.swift` | DESVIACIÓN: tabla `store_products` |
| Quiz publicado: bundle, no `content_items` | `Tests and UITests App/Tests/PublishedBankLoadTests.swift` | DESVIACIÓN: `content_items` `category=question` |
| Logbook del alumno: archivo `surgical_logbook.json` | `Features/Logbook/LogbookRepository.swift` | DESVIACIÓN: tablas `surgical_logs` y `procedures` |
| Perfil visible: tabla `profiles` | `Features/Profile/UserProfileViewModel.swift` | Igual. El onboarding de gamertag iOS usa además `user_profiles` (`ProfileOnboardingView.swift`) |
| Perlas: tabla `pearls` | `Data/Repositories/SupabasePearlRepository.swift` | Igual |
| RPC `get_premium_status` + RevenueCat como autoridad | `Core/Payment/EntitlementsService.swift` | DESVIACIÓN: `PremiumModule` emite Free y no llama al RPC ni a CustomerInfo |
| Project ref debug `tttxmupjteqpljtfgmgo`, staging `huzivubxotwvwnimasng` | `Config/Flags-Debug.xcconfig`, `Config/Flags-Staging.xcconfig` | `BuildConfig.SUPABASE_PROJECT_REF`. Panel en Ajustes si `SHOW_ENVIRONMENT_PANEL` |
