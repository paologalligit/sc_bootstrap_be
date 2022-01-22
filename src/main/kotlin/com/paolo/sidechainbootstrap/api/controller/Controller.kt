package com.paolo.sidechainbootstrap.api.controller

import com.paolo.sidechainbootstrap.service.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class Controller(
    private val sidechainBootstrapService: SidechainBootstrapService
) {
    @PostMapping("/generate-config")
    fun generateSidechainBootstrappingConf(
        @RequestBody generateConfigRequest: GenerateConfigRequest
    ): ResponseEntity<GenerateConfigServiceOutput> {
        // TODO: do some validation on input
        val initialSCConfig = sidechainBootstrapService.generateInitialConfig(generateConfigRequest.toServiceInput())

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(initialSCConfig)
    }

    @PostMapping("/genesis-info")
    fun generateGenesisInfo(
        @RequestBody generateGenesisInfoRequest: GenesisInfoRequest
    ): ResponseEntity<GenesisInfoServiceOutput> {
        // TODO: do some validation on input
        val genesisInfoResult = sidechainBootstrapService.generateGenesisInfo(generateGenesisInfoRequest.toServiceInput())

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(genesisInfoResult)
    }
}

data class GenerateConfigRequest(
    val keySeed: String,
    val vfrKeySeed: String,
    val maxPks: String,
    val threshold: String,
    val provingKeyPath: String,
    val verificationKeyPath: String
) {
    fun toServiceInput(): GenerateConfigServiceInput =
        GenerateConfigServiceInput(
            keySeed, vfrKeySeed, maxPks, threshold, provingKeyPath, verificationKeyPath
        )
}

data class GenesisInfoRequest(
    val info: String,
    val secret: String,
    val vrfSecret: String
) {
    fun toServiceInput(): GenesisInfoServiceInput =
        GenesisInfoServiceInput(
            info, secret, vrfSecret
        )
}