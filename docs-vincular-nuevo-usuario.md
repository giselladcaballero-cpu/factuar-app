# Cómo vincular la app FactuAR con tus propios datos

Cada instalación de la app guarda todo **localmente en el teléfono** — no hay
ninguna base de datos compartida. Si instalás la app en tu propio celular,
arranca en blanco, sin nada de otro usuario.

## Pasos

1. **Instalar el APK**
   Descargalo desde: https://factuar-mp-oauth.vercel.app/factuar-debug.apk
   (habilitá "orígenes desconocidos" si el teléfono lo pide).

2. **Cargar tus datos fiscales**
   En la app: Ajustes → completá tu **CUIT** y **Razón Social**.

3. **Generar tu certificado digital**
   - Ajustes → "Generar Nuevo Certificado (CSR)".
   - Copiá el CSR que te da la app.
   - Entrá a `auth.afip.gob.ar` (o `arca.gob.ar`) con **tu propia Clave Fiscal**
     — la app nunca la pide ni la necesita.
   - "Administración de Certificados Digitales" → "Agregar alias" → subí el
     CSR → elegí entorno **Producción** → descargá el `.crt` que te dan.
   - Volvé a la app, pegá el contenido del `.crt` en el campo correspondiente
     (NO uses el botón "autofirmado", ese es solo para pruebas de
     Homologación) → "Guardar Certificado".

4. **Habilitar el Punto de Venta**
   - En el portal de ARCA: servicio "Puntos de Venta y Domicilios" →
     "Agregar Punto de Venta".
   - Sistema: **"Factura Electrónica - Web Services"** (o el que corresponda
     a tu condición de IVA).
   - Anotá el número que te asigna y cargalo en Ajustes → Parámetros
     Fiscales → "Punto Venta".

5. **Asociar el servicio de Facturación Electrónica**
   - En ARCA: "Administrador de Relaciones de Clave Fiscal" → "Nueva
     Relación" → buscá el servicio "Facturación Electrónica" (WSFE) →
     asignalo al alias del certificado que generaste.

6. **Probar la conexión**
   - Ajustes → "Probar WSAA". Si te dice "Autenticación exitosa", quedó todo
     conectado.

7. **Conectar Mercado Pago**
   - Ajustes → "Conectar con Mercado Pago" → te abre el navegador con la
     pantalla real de autorización → aprobá con **tu propia cuenta** de
     Mercado Pago.

## Importante

- **No copies la carpeta de datos** de la app de un teléfono a otro — eso sí
  arrastraría el certificado y la configuración del primero. Descargá el
  APK de cero e instalalo como una app nueva en cada teléfono.
- Cada persona necesita su **propio certificado de ARCA** (no se puede
  compartir uno entre distintos CUIT).
