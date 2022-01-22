package com.paolo.sidechainbootstrap.command

import com.google.common.primitives.Bytes
import com.google.common.primitives.Ints
import com.horizen.companion.SidechainSecretsCompanion
import com.horizen.cryptolibprovider.CryptoLibProvider
import com.horizen.secret.SchnorrKeyGenerator
import com.horizen.secret.SchnorrSecret
import com.horizen.utils.BytesUtils
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

@Service
class ProofInfoGenerator {
    fun generate(proofInfoGeneratorInput: ProofInfoGeneratorInput): ProofInfoResult {
        val seed: ByteArray = proofInfoGeneratorInput.seed.toByteArray()
        val maxPks: Int = proofInfoGeneratorInput.maxPks

        if (maxPks <= 0) {
            throw IllegalArgumentException("Wrong max keys number: $maxPks")
        }

        val threshold: Int = proofInfoGeneratorInput.threshold

        if (threshold <= 0 || threshold > maxPks) {
            throw IllegalArgumentException("wrong threshold: $threshold")
        }

        val provingKeyPath: String = proofInfoGeneratorInput.provingKeyPath
        val verificationKeyPath: String = proofInfoGeneratorInput.verificationKeyPath

        val secretsCompanion = SidechainSecretsCompanion(HashMap())

        // Generate all keys only if verification key doesn't exist.
        // Note: we are interested only in verification key raw data.

        // Generate all keys only if verification key doesn't exist.
        // Note: we are interested only in verification key raw data.
        if (!Files.exists(Paths.get(verificationKeyPath))) {
            if (!CryptoLibProvider.sigProofThresholdCircuitFunctions().generateCoboundaryMarlinDLogKeys()) {
                throw IllegalStateException("Error occurred during dlog key generation.")
            }
            if (!CryptoLibProvider.sigProofThresholdCircuitFunctions()
                    .generateCoboundaryMarlinSnarkKeys(maxPks.toLong(), provingKeyPath, verificationKeyPath)
            ) {
                throw IllegalStateException("Error occurred during snark keys generation.")
            }
        }

        // Read verification key from file
        // Read verification key from file
        val verificationKey = CryptoLibProvider.sigProofThresholdCircuitFunctions()
            .getCoboundaryMarlinSnarkVerificationKeyHex(verificationKeyPath)

        if (verificationKey.isEmpty()) {
            throw IllegalStateException("Verification key file is empty or the key is broken.")
        }

        val secretKeys: MutableList<SchnorrSecret> = ArrayList()

        for (i in 0 until maxPks) {
            secretKeys.add(
                SchnorrKeyGenerator.getInstance()
                    .generateSecret(Bytes.concat(seed, Ints.toByteArray(i)))
            )
        }

        val publicKeysBytes =
            secretKeys.stream().map { obj: SchnorrSecret -> obj.publicBytes }.collect(Collectors.toList())
        val genSysConstant = BytesUtils.toHexString(
            CryptoLibProvider.sigProofThresholdCircuitFunctions()
                .generateSysDataConstant(publicKeysBytes, threshold.toLong())
        )

        val schnorrKeys: List<SchnorrKey> = secretKeys.map {
            SchnorrKey(
                BytesUtils.toHexString(it.publicBytes),
                BytesUtils.toHexString(secretsCompanion.toBytes(it))
            )
        }

        return ProofInfoResult(
            maxPks,
            threshold,
            genSysConstant,
            verificationKey,
            schnorrKeys
        )
    }
}

data class ProofInfoGeneratorInput(
    val seed: String,
    val maxPks: Int,
    val threshold: Int,
    val provingKeyPath: String,
    val verificationKeyPath: String
)

data class ProofInfoResult(
    val maxPks: Int,
    val threshold: Int,
    val genSysConstant: String,
    val verificationKey: String,
    val schnorrKeys: List<SchnorrKey>
)

data class SchnorrKey(
    val schnorrPublicKey: String,
    val schnorrSecret: String
)