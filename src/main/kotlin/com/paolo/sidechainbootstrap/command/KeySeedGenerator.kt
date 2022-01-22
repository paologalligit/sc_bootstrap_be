package com.paolo.sidechainbootstrap.command

import com.horizen.companion.SidechainSecretsCompanion
import com.horizen.secret.PrivateKey25519Creator
import com.horizen.utils.BytesUtils
import org.springframework.stereotype.Service

@Service
class KeySeedGenerator {
    fun generate(generateSeedInput: String): KeySeedResult {
        val key = PrivateKey25519Creator.getInstance().generateSecret(generateSeedInput.toByteArray())

        val secretsCompanion = SidechainSecretsCompanion(HashMap())

        val publicKey = BytesUtils.toHexString(key.publicImage().bytes())
        val secret = BytesUtils.toHexString(secretsCompanion.toBytes(key))

        return KeySeedResult(
            publicKey, secret
        )
    }
}

data class KeySeedResult (
    val publicKey: String,
    val secret: String
)