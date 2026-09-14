# Cómo vincular la app Vektor Go con tus propios datos

Cada instalación de la app guarda todo **localmente en el teléfono** (base
encriptada con SQLCipher) — no hay ninguna base de datos compartida. Si
instalás la app en tu propio celular, arranca en blanco, sin nada de otro
usuario.

## Pasos

1. **Instalar el APK**
   Habilitá "orígenes desconocidos" si el teléfono lo pide. Si ya tenías
   instalada una versión anterior con otro nombre de paquete (la vieja
   "FactuAR"), esta es una app distinta (`com.vektorgo.app`) — no la
   actualiza ni hereda su certificado, hay que generar uno nuevo (ver
   paso 3).

2. **Cargar tus datos fiscales**
   En la app: Ajustes → completá tu **CUIT** y **Razón Social**.

3. **Generar tu certificado digital**
   - Ajustes → "Generar Nuevo Certificado (CSR)".
   - Copiá el CSR que te da la app.
   - Entrá a `arca.gob.ar` (o `auth.afip.gob.ar`) con **tu propia Clave Fiscal**
     — la app nunca la pide ni la necesita.
   - "Administración de Certificados Digitales" → "Agregar alias" → subí el
     CSR → elegí entorno **Producción** → descargá el `.crt` que te dan.
   - Volvé a la app, pegá el contenido del `.crt` en el campo correspondiente
     (NO uses el botón "autofirmado", ese es solo para pruebas de
     Homologación) → "Guardar Certificado".

4. **Elegir el entorno correcto en la app**
   - Ajustes → "Parámetros Fiscales del Emisor" → "Entorno Web Services ARCA"
     → seleccioná **Producción (Fiscal)**.
   - Si queda en "Homologación (Test)" con un certificado de Producción, el
     "Probar WSAA" del paso 6 falla con "Certificado no emitido por AC de
     confianza" — son dos ambientes con distinta CA de confianza, tienen que
     coincidir.

5. **Habilitar el Punto de Venta**
   - En el portal de ARCA: servicio "Puntos de Venta y Domicilios" →
     "Agregar Punto de Venta".
   - Sistema: **"Factura Electrónica - Web Services"** (o el que corresponda
     a tu condición de IVA).
   - Anotá el número que te asigna y cargalo en Ajustes → Parámetros
     Fiscales → "Punto Venta".

6. **Asociar el servicio de Facturación Electrónica**
   - En ARCA: "Administrador de Relaciones de Clave Fiscal" → "Nueva
     Relación" → buscá el servicio "Facturación Electrónica" (WSFE) →
     asignalo al alias del certificado que generaste en el paso 3.

7. **Probar la conexión**
   - Ajustes → "Probar WSAA". Si te dice "Token obtenido" / "Autenticación
     exitosa", quedó todo conectado.

8. **Conectar Mercado Pago**
   - Ajustes → "Conectar con Mercado Pago" (o "Reconectar" si ya había algo
     vinculado) → te abre el navegador con la pantalla real de autorización
     → aprobá con **tu propia cuenta** de Mercado Pago.

## Importante

- **No copies la carpeta de datos** de la app de un teléfono a otro — eso sí
  arrastraría el certificado y la configuración del primero. Instalá el
  APK de cero en cada teléfono para que genere su propio certificado.
- Cada persona necesita su **propio certificado de ARCA** (no se puede
  compartir uno entre distintos CUIT).
- Si desinstalás la app y la volvés a instalar, se pierde el certificado
  guardado localmente y hay que repetir desde el paso 3 — para actualizar a
  una versión nueva, instalá siempre **encima** de la existente, sin
  desinstalar.
