# mp-oauth-service → Supabase Edge Functions

Reemplaza el servicio serverless de Vercel (`mp-oauth-service` en el repo de
FactuAR) por dos Edge Functions de Supabase, con el mismo comportamiento
exacto (mismo flujo PKCE, mismo deep link `factuar://mp-connected`).

## Por qué

- Vercel: se quería mantener en $0 mientras el abono mensual escala.
- Supabase ya estaba planeado para el panel de administración (usuarios,
  pagos, suscripciones) — con esto queda **un solo backend gratis** en vez de
  Vercel + Supabase corriendo en paralelo.
- Free tier: 500.000 invocaciones de Edge Functions/mes, Postgres 500MB,
  Storage 1GB, todo en el mismo proyecto.

## Archivos

- `functions/mp-authorize/index.ts` — redirige a la pantalla de autorización
  real de Mercado Pago, generando el PKCE `code_verifier`/`code_challenge`.
- `functions/mp-callback/index.ts` — recibe el `code`, lo canjea por un
  access token (con el Client Secret, server-side), y devuelve un deep link
  `factuar://mp-connected?...` que la app intercepta.
- `config.toml` — **clave:** desactiva `verify_jwt` en ambas funciones. Sin
  esto, Supabase rechaza con 401 el redirect de Mercado Pago antes de que
  corra el código, porque no viaja con un token de Supabase.

## Pasos para desplegar

1. Instalar la CLI de Supabase y logearse:
   ```
   npm install -g supabase
   supabase login
   ```
2. Vincular al proyecto (crear uno nuevo si no existe todavía):
   ```
   supabase link --project-ref TU_PROJECT_REF
   ```
3. Configurar los secrets (mismos valores que hoy están en Vercel):
   ```
   supabase secrets set MP_CLIENT_ID=xxxx MP_CLIENT_SECRET=xxxx MP_REDIRECT_URI=https://TU_PROJECT_REF.supabase.co/functions/v1/mp-callback
   ```
4. Desplegar:
   ```
   supabase functions deploy mp-authorize
   supabase functions deploy mp-callback
   ```

## Pasos manuales fuera de este repo (no los puedo hacer yo)

1. **Panel de desarrolladores de Mercado Pago** → tu aplicación → OAuth →
   cambiar la "URL de redirección" de
   `https://factuar-mp-oauth.vercel.app/api/mp-callback` a
   `https://TU_PROJECT_REF.supabase.co/functions/v1/mp-callback`
   (tiene que coincidir carácter por carácter con `MP_REDIRECT_URI`).
2. Actualizar la constante `MP_OAUTH_SERVICE_URL` en
   `app/src/main/java/com/vektorgo/app/MainActivity.kt` (línea ~85) de
   `https://factuar-mp-oauth.vercel.app` a tu URL de Supabase
   (`https://TU_PROJECT_REF.supabase.co/functions/v1`). No lo cambié yo
   todavía a propósito: hacerlo antes de que Supabase esté realmente
   desplegado rompe el OAuth real que hoy funciona contra Vercel.
3. El APK de descarga (`factuar-debug.apk`) hoy se sirve como archivo
   estático desde `mp-oauth-service/public/` en Vercel. Migrar eso a un
   bucket público de **Supabase Storage** (`supabase storage cp` o subida
   manual desde el dashboard) y actualizar el link de descarga en el README.
4. Una vez confirmado que el flujo real funciona end-to-end, dar de baja el
   proyecto en Vercel.

## Suscripción de Vektor Go (`create-subscription` + `subscription-callback`)

Esto cobra la suscripción mensual del NEGOCIO Vektor Go a cada comerciante
que usa la app — es una cuenta de Mercado Pago completamente distinta a la
del comerciante (que factura pagos y transferencias). No confundir los
secrets de una con los de la otra.

**Archivos:**
- `functions/create-subscription/index.ts` — la app abre esta URL en el
  navegador; crea un `preapproval` (recurrencia) de Mercado Pago **sin**
  `free_trial` configurado en el plan, y redirige al checkout real de
  Mercado Pago para esa recurrencia.
- `functions/subscription-callback/index.ts` — `back_url` del checkout.
  Verifica el estado real contra la API de Mercado Pago (nunca confía en
  los query params del redirect, no vienen firmados) y devuelve el deep
  link `factuar://subscription-activated?...`.

**Pasos manuales pendientes (no los puedo hacer yo, y nada de esto anda
hasta que existan):**

1. Crear una cuenta de Mercado Pago separada para el NEGOCIO Vektor Go (no
   la de ningún comerciante cliente). Sacar su Access Token de producción
   desde `developers.mercadopago.com` → esa cuenta → Credenciales.
2. Crear el plan de suscripción una sola vez, con esa cuenta:
   ```
   curl -X POST https://api.mercadopago.com/preapproval_plan \
     -H "Authorization: Bearer TU_BUSINESS_ACCESS_TOKEN" \
     -H "Content-Type: application/json" \
     -d '{
       "reason": "Vektor Go - Suscripción mensual",
       "auto_recurring": {
         "frequency": 1,
         "frequency_type": "months",
         "transaction_amount": 14999,
         "currency_id": "ARS"
       }
     }'
   ```
   Importante: **no** incluir `"free_trial"` — sin ese campo, Mercado Pago
   cobra desde el primer día, que es lo que se pidió (sin período de
   prueba). La respuesta trae un `id`: ese es el `preapproval_plan_id`.
3. Cargar los secrets en Supabase (Project Settings → Edge Functions →
   Secrets, o `supabase secrets set`):
   ```
   VEKTOR_BUSINESS_MP_ACCESS_TOKEN=<el access token del paso 1>
   VEKTOR_SUBSCRIPTION_PLAN_ID=<el id del paso 2>
   VEKTOR_SUBSCRIPTION_BACK_URL=https://TU_PROJECT_REF.supabase.co/functions/v1/subscription-callback
   ```
4. Desplegar:
   ```
   supabase functions deploy create-subscription
   supabase functions deploy subscription-callback
   ```
5. Probar el flujo real: `openSubscriptionCheckout()` en la app abre
   `create-subscription`, que debería redirigir al checkout de Mercado
   Pago. Completarlo con una cuenta de prueba y confirmar que
   `subscription-activated` llega con `status=authorized`.

**Nota sobre el gate en la app:** el paywall (`SubscriptionPaywallScreen`)
está desactivado en builds de debug a propósito (`BuildConfig.DEBUG`),
para no bloquear el resto de las pruebas mientras esta cuenta no exista.
En builds de release el gate funciona de verdad.

## Panel de administración (suscriptores)

Tabla `public.subscribers` en Postgres (RLS habilitado, sin políticas —
solo las Edge Functions con la service role key pueden leerla/escribirla).

- `functions/register-subscriber/index.ts` — la app la llama después de
  activar una suscripción (`BillingRepository.activateSubscription`).
  Guarda/actualiza el registro del comerciante: nombre, CUIT, estado de
  suscripción, monto.
- `functions/admin-subscribers/index.ts` — la lee el panel HTML. Protegida
  por un secret compartido (`ADMIN_PANEL_SECRET`), no por login real —
  suficiente para una herramienta interna de un solo administrador.

**Paso manual pendiente:** cargar el secret `ADMIN_PANEL_SECRET` en
Supabase (Project Settings → Edge Functions → Secrets). El valor generado
para esto se compartió aparte, fuera de este repo — nunca se commitea un
secret real a git, ni en un doc. Si se rota, hay que actualizarlo también
en el panel HTML (login del panel).

## Nota sobre el esquema del deep link

Se mantuvo `factuar://mp-connected` sin cambios a propósito: si se renombra
a `vektorgo://...` hay que actualizarlo en la app Y en este servicio al
mismo tiempo, o se rompe la vinculación en producción. Se puede hacer en un
paso aparte, coordinado.
