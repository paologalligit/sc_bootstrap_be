package com.paolo.sidechainbootstrap.command

import org.springframework.stereotype.Service

@Service
class Command(
    private val keySeedGenerator: KeySeedGenerator,
    private val vrfKeySeedGenerator: VrfKeySeedGenerator,
    private val proofInfoGenerator: ProofInfoGenerator,
    private val genesisInfoGenerator: GenesisInfoGenerator
) {
    fun generateKeySeed(keySeed: String): KeySeedResult {
        return keySeedGenerator.generate(keySeed)
    }

    fun generateVrfKeySeed(vrfKeySeed: String): VrfKeySeedResult {
        return vrfKeySeedGenerator.generate(vrfKeySeed)
    }

    fun generateProofInfo(proofInfoGeneratorInput: ProofInfoGeneratorInput): ProofInfoResult {
        return proofInfoGenerator.generate(proofInfoGeneratorInput)
    }

    fun generateGenesisInfo(genesisInfoInput: GenesisInfoInput): GenesisInfoOutput {
        return genesisInfoGenerator.generate(genesisInfoInput)
    }
}

