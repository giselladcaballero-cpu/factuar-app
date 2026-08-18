package com.example.data.arca

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Security

/**
 * Local Cryptographic Key & CSR Generation Service for ARCA (AFIP).
 * Generates 2048-bit RSA keys on-device and a real PKCS#10 Certificate Signing
 * Request. ARCA has no public API to issue a certificate automatically: the
 * user must upload this CSR themselves at "Administración de Certificados
 * Digitales" on afip.gob.ar (logged in with their own Clave Fiscal, in their
 * own browser) and paste back the resulting certificate. This app never asks
 * for or handles the Clave Fiscal.
 */
class CertificateProvisioningService {

    init {
        // Android ships its own stripped-down "BC" provider (from Conscrypt),
        // which doesn't support the signature algorithms we need. It must be
        // removed before installing the full BouncyCastle provider under the
        // same name, otherwise Security.addProvider() is a silent no-op.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME)
        Security.insertProviderAt(BouncyCastleProvider(), 1)
    }

    /**
     * Generates a secure 2048-bit RSA key pair. The private key remains
     * strictly on the client device and is never transmitted anywhere.
     */
    suspend fun generateRsaKeyPair(): KeyPair = withContext(Dispatchers.Default) {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048, SecureRandom())
        keyGen.generateKeyPair()
    }

    /**
     * Converts a PrivateKey to PKCS#8 PEM string.
     */
    fun privateKeyToPem(privateKey: PrivateKey): String {
        val encoded = Base64.encodeToString(privateKey.encoded, Base64.NO_WRAP)
        val chunks = encoded.chunked(64).joinToString("\n")
        return "-----BEGIN PRIVATE KEY-----\n$chunks\n-----END PRIVATE KEY-----"
    }

    /**
     * Converts a PublicKey to X.509 SubjectPublicKeyInfo PEM string.
     */
    fun publicKeyToPem(publicKey: PublicKey): String {
        val encoded = Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
        val chunks = encoded.chunked(64).joinToString("\n")
        return "-----BEGIN PUBLIC KEY-----\n$chunks\n-----END PUBLIC KEY-----"
    }

    /**
     * Generates a real Certificate Signing Request (PKCS#10) for ARCA/AFIP.
     * Subject: C=AR, O=<razonSocial>, CN=<razonSocial>, SERIALNUMBER=CUIT <cuit>
     */
    suspend fun generateCsrPem(cuit: Long, razonSocial: String, keyPair: KeyPair): String = withContext(Dispatchers.Default) {
        val cleanRazon = razonSocial.ifBlank { "Contribuyente" }.replace(",", " ").replace("=", " ")
        val subject = X500Name("C=AR,O=$cleanRazon,CN=$cleanRazon,SERIALNUMBER=CUIT $cuit")

        val csrBuilder = JcaPKCS10CertificationRequestBuilder(subject, keyPair.public)
        val contentSigner = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.private)

        val csr: PKCS10CertificationRequest = csrBuilder.build(contentSigner)
        val encoded = Base64.encodeToString(csr.encoded, Base64.NO_WRAP)
        val chunks = encoded.chunked(64).joinToString("\n")
        "-----BEGIN CERTIFICATE REQUEST-----\n$chunks\n-----END CERTIFICATE REQUEST-----"
    }
}
