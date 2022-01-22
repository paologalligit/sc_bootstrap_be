package com.paolo.sidechainbootstrap.command

import com.horizen.companion.SidechainSecretsCompanion
import com.horizen.secret.VrfKeyGenerator
import com.horizen.utils.BytesUtils
import org.springframework.stereotype.Service

@Service
class VrfKeySeedGenerator {
    fun generate(vrfKeySeedInput: String): VrfKeySeedResult {
        val secretsCompanion = SidechainSecretsCompanion(HashMap())

        val vrfSecretKey = VrfKeyGenerator.getInstance().generateSecret(vrfKeySeedInput.toByteArray())

        val vrfPublicKey = BytesUtils.toHexString(vrfSecretKey.publicBytes)
        val vrfSecret = BytesUtils.toHexString(secretsCompanion.toBytes(vrfSecretKey))

        return VrfKeySeedResult(
            vrfPublicKey, vrfSecret
        )
    }
}

data class VrfKeySeedResult(
    val vrfPublicKey: String,
    val vrfSecret: String
)