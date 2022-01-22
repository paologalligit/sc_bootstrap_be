package com.paolo.sidechainbootstrap.service

import com.paolo.sidechainbootstrap.command.*
import org.springframework.stereotype.Service

@Service
class SidechainBootstrapService(
    private val command: Command
) {
    fun generateInitialConfig(generateConfigServiceInput: GenerateConfigServiceInput): GenerateConfigServiceOutput {
        // Step 1: generate key seed
        val keySeedResult = command.generateKeySeed(generateConfigServiceInput.keySeed)

        // Step 2: generate vrf key seed
        val vrfKeySeedResult = command.generateVrfKeySeed(generateConfigServiceInput.vfrKeySeed)

        // Step 3: generate proof info
        val proofInfoGeneratorInput = ProofInfoGeneratorInput(
            generateConfigServiceInput.keySeed,
            generateConfigServiceInput.maxPks.toInt(),
            generateConfigServiceInput.threshold.toInt(),
            generateConfigServiceInput.provingKeyPath,
            generateConfigServiceInput.verificationKeyPath
        )
        val proofInfoResult = command.generateProofInfo(proofInfoGeneratorInput)

        return GenerateConfigServiceOutput(
            keySeedResult,
            vrfKeySeedResult,
            proofInfoResult
        )
    }
}

data class GenerateConfigServiceInput(
    val keySeed: String,
    val vfrKeySeed: String,
    val maxPks: String,
    val threshold: String,
    val provingKeyPath: String,
    val verificationKeyPath: String
)

data class GenerateConfigServiceOutput(
    val keySeed: KeySeedResult,
    val vrfKeySeed: VrfKeySeedResult,
    val proofInfoResult: ProofInfoResult
)