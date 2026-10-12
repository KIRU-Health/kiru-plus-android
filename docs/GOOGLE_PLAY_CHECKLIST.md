# Google Play Console — Checklist de cumplimiento KIRU+ Android

> **Owner:** Equipo móvil KIRU+ · **Última revisión:** 2026-05-17

Estado: ☐ pendiente · ◐ en progreso · ☑ hecho · ⚠️ riesgo · n/a no aplica

---

## A. Identidad de la app y store listing

| Item | Estado | Notas |
|------|--------|-------|
| Nombre app: **KIRU+** | ☐ | Mismo que iOS |
| Package name: `com.medsurgery.kiruplus` | ☐ | Definido en `app/build.gradle.kts` |
| Categoría primaria: **Medicina** | ☐ | Google Play categoriza apps de salud bajo "Medical" o "Health & Fitness" |
| Audiencia objetivo: **18+ profesionales médicos** | ☐ | NO declarar audiencia infantil para evitar Families Policy |
| Descripción corta (≤80 chars) | ☐ | Idéntica a iOS App Store |
| Descripción larga (≤4000 chars) | ☐ | Traducir desde iOS; NO uses claims terapéuticos |
| Screenshots: phone (mínimo 2), tablet (recomendado) | ☐ | Reusar arte iOS adaptado a Android aspect ratios |
| Feature graphic 1024×500 | ☐ | Reusar arte de marca |
| Icono 512×512 | ☐ | Mismo branding |

## B. Data Safety Form

| Item | Estado | Notas |
|------|--------|-------|
| Inicio del cuestionario | ☐ | Obligatorio antes de submission |
| Recolecta datos personales | ☐ | **Sí** — email, nombre, cédula profesional |
| Comparte con terceros | ☐ | **Sí** — Sentry (crash reports), MUX (video analytics), RevenueCat (purchases), Stripe (productos físicos), Supabase (hosting datos) |
| Datos cifrados en tránsito (HTTPS/TLS) | ☐ | **Sí** — Supabase + Render + APIs externos |
| Usuario puede solicitar eliminación de datos | ☐ | **Sí** — flujo in-app `process_account_deletions` con 48h grace + URL pública |
| Tipos de datos recolectados | ☐ | Email, Nombre, ID de usuario (auth.uid), Cédula profesional (PII sensible) |
| Tipos de actividad recolectada | ☐ | App interactions, in-app purchases (vía RevenueCat), in-app search |
| Optional vs Required collection | ☐ | Email/cédula = required para validación profesional |
| Compromiso de seguridad de datos | ☐ | Política privacidad en medsurgery.academy referenciada |

## C. Permisos y APIs sensibles

| Permiso | Estado | Justificación que va en Play Console |
|---------|--------|--------------------------------------|
| `INTERNET` | ☐ | Conexión con Supabase y servicios cloud |
| `ACCESS_NETWORK_STATE` | ☐ | Detectar offline para fallback de caché |
| `RECORD_AUDIO` | ☐ | Voz Dr. Kapibaya (tutor de voz educativo) + dictado clínico |
| `POST_NOTIFICATIONS` (Android 13+) | ☐ | Notificaciones de contenido educativo semanal |
| Foreground service `microphone` (si aplica) | ⚠️ | Solo si el voice engine necesita continuar en background; evitar si no es indispensable |
| Sin `READ_CONTACTS`, `ACCESS_FINE_LOCATION`, `CAMERA`, `READ_EXTERNAL_STORAGE` | ☐ | KIRU+ NO requiere ninguno de estos |
| `READ_MEDIA_IMAGES` (solo para upload cédula) | ◐ | Evaluar: ¿usar Photo Picker (sin permiso)? Photo Picker es la ruta recomendada Android 13+ |

## D. Account Deletion (REQUERIDO desde 2024)

| Item | Estado | Notas |
|------|--------|-------|
| Flujo in-app de eliminación de cuenta | ☐ | E4 — pantalla en Settings → "Eliminar mi cuenta" |
| URL pública de eliminación de cuenta | ☐ | medsurgery.academy/eliminar-cuenta o similar; debe permitir solicitar sin descargar la app |
| Grace period 48h | ☐ | Ya implementado en backend (`account_deletion_requests` con expiración 48h) |
| Confirmación al usuario | ☐ | Email vía `process_account_deletions` Edge Function |
| Datos eliminados o anonimizados | ☐ | RLS cascades + tablas con `auth.uid()` ON DELETE |

## E. In-App Purchases / Subscriptions

| Item | Estado | Notas |
|------|--------|-------|
| Suscripciones digitales via Google Play Billing | ☐ | RevenueCat Android SDK encapsula Google Play Billing |
| Subscription products definidos en Play Console | ☐ | Los product id de Play deben ser exactamente los de iOS (`AccessProductCatalog` / `KIRUProductID`): `kiru_resident_elite_monthly`, `kiru_surgeon_pro_monthly`, `kiru_surgeon_max_annual`, `kiru_surgeon_mega_monthly`. En RevenueCat se ligan a `tier_resident`, `tier_pro`, `tier_max` y el legacy `KIRU+ App`. Offering `21875673`. Si el product id no coincide, `resolveCustomerInfo` cae a Free. |
| Pricing por país | ☐ | Replicar tier pricing de iOS |
| Subscription details correctos (período, free trial, intro pricing) | ☐ | Espejo de iOS |
| Política de cancelación visible | ☐ | URL: medsurgery.academy/politica-de-suscripciones-kiru-app (igual que iOS) |
| Restore purchases | ☐ | Cubierto por RevenueCat SDK |
| Webhook RevenueCat → `revenuecat_webhook` Edge Function | ☐ | Ya existe (verifica fuente única de verdad en `iap_entitlements`) |

## F. Productos físicos y external payments

| Item | Estado | Notas |
|------|--------|-------|
| Productos físicos pueden usar payment externo | ☐ | **Sí, está permitido** — Stripe Checkout via Chrome Custom Tabs |
| Hacer claro que es producto físico, NO contenido digital | ☐ | UI labels: "Productos KIRU+ Store" (medical books, scrubs) |
| NO mezclar IAP y external payment en mismo flujo | ☐ | Tab Store completamente separado de Tab Premium/Paywall |
| Política de envíos y devoluciones visible | ☐ | Link a medsurgery.academy |

## G. Sensitive content / Health Apps Policy

| Item | Estado | Notas |
|------|--------|-------|
| Disclaimer médico claro y visible | ☐ | Modal bloqueante en primera ejecución (5 secciones), banner persistente en Home (texto iOS reusable) |
| NO hacer claims de diagnóstico ni tratamiento | ☐ | Texto iOS ya cumple: "NO constituye consejo médico, diagnóstico ni tratamiento" |
| Audiencia: profesionales médicos certificados | ☐ | Validación de cédula obligatoria para premium |
| AI/ML disclosures | ⚠️ | Dr. Kapibaya usa Gemini (LLM). Declarar en descripción y disclaimer que las respuestas son educativas, no clínicas |
| Wellbeing alerts: NO storage de PHI ni conversación completa | ☐ | Ya enforced en `wellbeing_alerts.summary_redacted ≤ 500 chars` |
| Sin diagnóstico ofrecido al usuario final | ☐ | App es para profesionales; pacientes no son target |

## H. Children's Privacy / Families Policy

| Item | Estado | Notas |
|------|--------|-------|
| Audiencia 18+ declarada | ☐ | NO opt-in a Families program |
| Sin contenido dirigido a menores | ☐ | Confirmar en formulario "Target Audience" |
| Sin SDK con menores como audiencia | ☐ | Google Play SDK Index inspection limpia |

## I. App Content Rating (IARC)

| Item | Estado | Notas |
|------|--------|-------|
| Cuestionario IARC completo | ☐ | Mature 17+ likely por contenido quirúrgico/médico explícito |
| Imágenes médicas / cirugía / sangre (educativas) | ☐ | Declarar en IARC |

## J. Security and policy compliance

| Item | Estado | Notas |
|------|--------|-------|
| Target SDK 35 (Android 15) | ☐ | Requerido Google Play desde Aug 2025 |
| Sin librerías con vulnerabilidades conocidas (Google Play SDK Index) | ☐ | Revisar dependencias en E11 |
| App Bundle (.aab), no .apk | ☐ | `assembleRelease` produce .aab |
| 64-bit binary | ☐ | Compose es 64-bit native |
| ProGuard/R8 habilitado | ☐ | En `release` build type |
| Signing config seguro (Play App Signing) | ☐ | Upload key generado, Play App Signing activado |
| No hard-coded secrets en código compilado | ☐ | BuildConfig injection desde `local.properties` |
| `usesCleartextTraffic = false` | ☐ | Manifest + Network Security Config |

## K. Login credentials for App Review

| Item | Estado | Notas |
|------|--------|-------|
| Cuenta demo con premium activo | ☐ | Equivalente a `review@medsurgery.academy` / `KiruPlu$` de iOS |
| Instrucciones de login en Play Console "App access" | ☐ | Indicar que cualquier cuenta requiere validación cédula → usar demo |
| Bypass o feature flag de "App Review" para Android | ☐ | Crear equivalente del "Apple Review premium bridge" iOS (mismo concepto, distinto trigger) |

## L. Notifications Policy

| Item | Estado | Notas |
|------|--------|-------|
| Permiso `POST_NOTIFICATIONS` solicitado in-app después de explicar valor | ☐ | NO disparar al inicio (mejor UX) |
| Channels declarados | ☐ | "Drops semanales", "Sistema", "Pedidos de tienda" |
| Sin spam / clickbait | ☐ | Solo notificaciones educativas o transaccionales |

## M. Privacy policy and disclosures

| Item | Estado | Notas |
|------|--------|-------|
| URL pública política privacidad | ☐ | https://www.medsurgery.academy/politica-de-privacidad-kiru-app (reusada) |
| Detalle de SDKs de terceros en política | ☐ | Sentry, MUX, RevenueCat, Stripe, Supabase, Resend, LogRocket (si se incluye) |
| Sección específica para datos médicos | ☐ | Cédula profesional, validación, NO PHI de pacientes |
| Sección AI/LLM | ⚠️ | Dr. Kapibaya respuestas son educativas, no diagnóstico; usuario consiente al continuar |

## N. Release tracks

| Track | Estado | Notas |
|-------|--------|-------|
| Internal testing (hasta 100 testers) | ☐ | E12 — devs y QA |
| Closed testing (lista de emails) | ☐ | E13 — 15+ médicos de confianza |
| Open testing | n/a | Saltar a producción tras closed |
| Production rollout 10% → 50% → 100% | ☐ | E14 |
| Pre-launch report habilitado | ☐ | Detecta crashes en pre-launch automático |

## O. Compliance items específicos México

| Item | Estado | Notas |
|------|--------|-------|
| LFPDPPP (privacidad México) | ☐ | Cubierto por política de privacidad + `data_export_requests` |
| NOM-004-SSA3-2012 (expediente clínico) | ☐ | Solo afecta backend (logs quirúrgicos); UI Android replica el flujo iOS de `InformedConsentView` con firmas + SHA-256 |
| NOM-024-SSA3-2012 (intercambio info salud) | ☐ | Aplica al backend (Edge Functions) |

## P. Items finales antes de submit

| Item | Estado | Notas |
|------|--------|-------|
| Build firmado + .aab subido | ☐ | E11 |
| Versionamiento alineado (versionCode > anterior) | ☐ | Empezar en versionCode=1, versionName="1.0.0" |
| Release notes en es y en | ☐ | Idéntico patrón iOS |
| Screenshots subidos | ☐ | |
| Política de privacidad URL en consola | ☐ | |
| Account deletion URL en consola | ☐ | |
| Data Safety form completo | ☐ | |
| Content rating completo | ☐ | |
| Target audience declarado | ☐ | |
| Pricing y distribution países | ☐ | |
| Pre-launch report verde | ☐ | |
| Submit for review | ☐ | ETA: tras E13 completo |

---

## Riesgos principales a mitigar antes de submit

1. **AI-generated medical content (Dr. Kapibaya)** — Google ha endurecido políticas sobre LLM en salud. Mitigación: disclaimer ya pulido + opt-in explícito + telemetría de wellbeing_alerts demuestra responsabilidad.
2. **Validación cédula como gatekeeper** — debe ser claro que la app es para profesionales certificados, NO público general. Mitigación: copy del disclaimer es categórico.
3. **External payments (Stripe) para productos físicos** — permitido, pero debe estar visualmente segregado de IAP. Mitigación: tab Store separado.
4. **NOM-004 firmas y PDF** — el sistema iOS genera PDF firmado localmente. En Android usar `android.graphics.pdf.PdfDocument` + signature canvas; ofrecer mismo SHA-256 attestation.
