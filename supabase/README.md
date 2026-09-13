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

## Nota sobre el esquema del deep link

Se mantuvo `factuar://mp-connected` sin cambios a propósito: si se renombra
a `vektorgo://...` hay que actualizarlo en la app Y en este servicio al
mismo tiempo, o se rompe la vinculación en producción. Se puede hacer en un
paso aparte, coordinado.
