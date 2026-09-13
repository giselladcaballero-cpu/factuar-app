# FactuAR — Documentación del proyecto

App Android que factura automáticamente ante ARCA (ex AFIP) las ventas
cobradas por Mercado Pago, con CAE real.

- **Repositorio**: https://github.com/giselladcaballero-cpu/factuar-app (privado)
- **Servicio OAuth de Mercado Pago**: https://factuar-mp-oauth.vercel.app
- **Descarga del APK (debug)**: https://factuar-mp-oauth.vercel.app/factuar-debug.apk

---

## 1. Qué hace la app

1. El usuario conecta su cuenta de Mercado Pago (1 toque, OAuth real).
2. Cuando entra un pago aprobado, la app arma automáticamente una Factura
   A/B/C según la condición de IVA del emisor y del comprador.
3. Firma el comprobante con el certificado digital del usuario y lo envía a
   ARCA (WSFE v1) para obtener un **CAE real**.
4. Guarda la factura localmente, con su QR oficial de verificación de ARCA.
5. Permite anular una factura con **Nota de Crédito** (referenciada a la
   original vía `CbtesAsoc`, como exige ARCA).

## 2. Arquitectura

```
┌─────────────────────────────┐
│   App Android (Kotlin +     │
│   Jetpack Compose)          │
│                              │
│  - Base de datos local       │
│    (Room/SQLite, por         │
│    dispositivo, sin backend) │
│  - Llama DIRECTO a:           │
│    · ARCA WSAA / WSFE v1     │
│    · API de Mercado Pago     │
└──────────────┬───────────────┘
               │ (solo para el 1-toque de Mercado Pago)
               ▼
┌─────────────────────────────┐
│  mp-oauth-service (Vercel)   │
│  Funciones serverless        │
│  - Guarda el Client Secret    │
│    de Mercado Pago (nunca en  │
│    el teléfono)               │
│  - Intercambia el código       │
│    OAuth por un Access Token   │
│  - Redirige de vuelta a la app │
│    vía factuar://mp-connected  │
└─────────────────────────────┘
```

**No hay backend propio para ARCA** — cada instalación de la app se
autentica y factura directo contra los servidores de ARCA con su propio
certificado. Esto significa que cada usuario/CUIT necesita su propio
certificado digital (no se puede compartir uno entre varios usuarios).

## 3. Integración con ARCA (ex AFIP)

- **WSAA** (`data/arca/WsaaAuthService.kt`): firma el Ticket de
  Requerimiento de Acceso con un **CMS/PKCS#7 real** (BouncyCastle,
  SHA1withRSA — el algoritmo que efectivamente acepta el validador de
  WSAA). Sin fallback simulado: si falla, se muestra el error real.
- **WSFE v1** (`data/arca/WsfeBillingService.kt`): arma y envía
  `FECAESolicitar` real. Consulta `FECompUltimoAutorizado` a ARCA antes de
  cada emisión (fuente de verdad del próximo número de comprobante, no la
  base local). Soporta Notas de Crédito con referencia a la factura
  original.
- **Certificados** (`data/arca/CertificateProvisioningService.kt`): genera
  un par de claves RSA 2048 y un CSR real (PKCS#10) en el dispositivo. La
  clave privada **nunca sale del teléfono**. El usuario sube el CSR él
  mismo a `auth.afip.gob.ar` (con su propia Clave Fiscal, que la app nunca
  pide) y pega de vuelta el certificado que ARCA le entrega.

### Requisitos de ARCA por cada usuario nuevo (una sola vez)

1. Certificado digital para el servicio WSFE (vía CSR, ver más arriba).
2. Punto de Venta dado de alta con **Sistema: Web Services** (no
   "Facturador Móvil" ni "Controlador Fiscal") — si no, ARCA rechaza con
   "El punto de venta no se encuentra habilitado a usar en el presente WS".
3. Asociar el servicio "Facturación Electrónica" al alias del certificado
   en el **Administrador de Relaciones de Clave Fiscal** — si no, ARCA
   rechaza con "Computador no autorizado a acceder al servicio".

## 4. Integración con Mercado Pago

- **Lectura de pagos** (`data/mercadopago/MercadoPagoService.kt`): usa el
  Access Token del usuario para consultar `/v1/payments/{id}` (pagos
  reales) y `/users/me` (validar la cuenta).
- **Vinculación 1-toque real (OAuth + PKCE)**: la app abre el navegador en
  la pantalla de autorización real de Mercado Pago. El intercambio del
  código por el token pasa por `mp-oauth-service` en Vercel, que guarda el
  Client Secret de la aplicación de Mercado Pago (nunca en el APK).
  - La aplicación de Mercado Pago debe estar creada como tipo que permita
    OAuth, con **PKCE habilitado** en su configuración avanzada, y con la
    **URL de redirección** exacta `https://factuar-mp-oauth.vercel.app/api/mp-callback`
    cargada en el campo correspondiente (no en "URL del sitio en
    producción", que es solo informativo).
  - Una cuenta de Mercado Pago **no puede autorizar a la aplicación que
    ella misma administra** — para probar el flujo hace falta un
    "Usuario de prueba" (sección homónima en el panel de developers) o una
    cuenta distinta a la dueña de la app.
- Queda un modo manual de respaldo (pegar el Access Token a mano) para
  contadores o si el OAuth falla.

## 5. Errores reales que aparecieron y cómo se resolvieron

| Síntoma | Causa real | Solución |
|---|---|---|
| `Certificado no emitido por AC de confianza` | Certificado de Homologación probado contra Producción (o viceversa); certificado autofirmado sin ser Marketplace habilitado | Usar certificado real de ARCA emitido para el ambiente correcto |
| `bad base-64` al probar WSAA | Copiar/pegar el certificado desde el chat insertaba caracteres invisibles que el filtro de espacios no sacaba | Filtrar solo caracteres válidos de Base64 en vez de "espacios" |
| WSAA decía error pero en realidad había autenticado bien | La respuesta de WSAA viene con el XML interno HTML-escapado (`&lt;token&gt;`), y el código buscaba literal `<token>` | Des-escapar entidades HTML antes de revisar/parsear la respuesta |
| `Computador no autorizado a acceder al servicio` | Certificado válido pero sin asociar al servicio WSFE en ARCA | Asociarlo en Administrador de Relaciones |
| `El punto de venta no se encuentra habilitado...` | Punto de Venta no configurado como "Web Services" en ARCA | Dar de alta el Punto de Venta con el sistema correcto |
| Mercado Pago: "no puede conectarse a tu cuenta" | Faltaba activar PKCE en la app de Mercado Pago, o probar con la misma cuenta dueña de la app | Activar PKCE + URL de redirección real + probar con usuario de prueba |
| Certificado se "perdía" después de actualizar la app | Desinstalar el APK antes de instalar el nuevo borra la base de datos local | Instalar la actualización **encima**, sin desinstalar |

## 6. Cómo se compila y despliega

- El proyecto no compila localmente en esta PC por un bug de red de Windows
  (fallo de sockets AF_UNIX de Java, no relacionado con el código). Por eso
  se compila en la nube:
  - `git push` a `main` dispara **GitHub Actions** (`.github/workflows/build.yml`),
    que compila el APK y lo deja como artifact descargable.
  - Ese APK se copia a `mp-oauth-service/public/factuar-debug.apk` y se
    redepliega en Vercel para tener un link de descarga directa.
- El servicio de Mercado Pago (`mp-oauth-service/`) se despliega aparte con
  `vercel deploy --prod` desde esa carpeta.

## 7. Pendiente / próximos pasos

- [ ] Terminar de depurar el error de Nota de Crédito (HTTP 400 vacío de
      WSFE) — se agregó diagnóstico para ver el XML exacto enviado.
- [ ] Conseguir la cuenta de Mercado Pago que va a **cobrar las
      suscripciones** del negocio (distinta de la de cada cliente).
- [ ] Armar base de datos + panel de administración para ver usuarios,
      pagos y suscripciones (pensado con Supabase).
- [ ] Publicar en Google Play Store (cuenta de desarrollador, USD 25 +
      verificación de identidad; conviene registrarse como empresa para
      saltear el requisito de 12 testers).
- [ ] Registrar la marca "FactuAR" (INPI, ~USD 150, 10 años) antes de
      lanzar públicamente.
- [ ] Opcional: registrar el software en la DNDA (derecho de autor, barato).
