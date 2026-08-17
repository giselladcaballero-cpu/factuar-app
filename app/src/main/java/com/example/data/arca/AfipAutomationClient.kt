package com.example.data.arca

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class CsrGenerationStatus {
    data class Progress(
        val stepIndex: Int,
        val totalSteps: Int,
        val title: String,
        val description: String
    ) : CsrGenerationStatus()

    data class Ready(
        val cuit: Long,
        val razonSocial: String,
        val csrPem: String,
        val privateKeyPem: String
    ) : CsrGenerationStatus()

    data class Error(
        val errorMessage: String
    ) : CsrGenerationStatus()
}

/**
 * Generates a real RSA key pair and PKCS#10 CSR entirely on-device for ARCA
 * (ex AFIP) WSFE. This never asks for or touches the user's Clave Fiscal:
 * ARCA has no public API to issue a certificate automatically, so the user
 * must upload the resulting CSR themselves at "Administración de
 * Certificados Digitales" on afip.gob.ar and paste back the certificate ARCA
 * gives them.
 */
class CsrGenerationClient(
    private val provisioningService: CertificateProvisioningService = CertificateProvisioningService()
) {

    fun generateCsr(cuit: Long, razonSocial: String): Flow<CsrGenerationStatus> = flow {
        try {
            if (cuit.toString().length != 11) {
                emit(CsrGenerationStatus.Error("El CUIT debe tener exactamente 11 dígitos."))
                return@flow
            }

            emit(
                CsrGenerationStatus.Progress(
                    stepIndex = 1,
                    totalSteps = 2,
                    title = "Generando par de claves RSA 2048-bit",
                    description = "Creando la clave privada en este dispositivo. Nunca sale del teléfono."
                )
            )
            val keyPair = provisioningService.generateRsaKeyPair()
            val privateKeyPem = provisioningService.privateKeyToPem(keyPair.private)
            delay(200)

            emit(
                CsrGenerationStatus.Progress(
                    stepIndex = 2,
                    totalSteps = 2,
                    title = "Generando la Solicitud de Certificado (CSR)",
                    description = "Armando el PKCS#10 para subir a ARCA."
                )
            )
            val csrPem = provisioningService.generateCsrPem(cuit, razonSocial, keyPair)
            delay(150)

            emit(
                CsrGenerationStatus.Ready(
                    cuit = cuit,
                    razonSocial = razonSocial.ifBlank { "Contribuyente CUIT $cuit" },
                    csrPem = csrPem,
                    privateKeyPem = privateKeyPem
                )
            )
        } catch (e: Exception) {
            emit(CsrGenerationStatus.Error("Error generando el CSR: ${e.localizedMessage ?: e.message}"))
        }
    }
}
