package com.paolo.sidechainbootstrap.command

import com.horizen.block.MainchainBlockReference
import com.horizen.block.Ommer
import com.horizen.block.SidechainBlock
import com.horizen.box.NoncedBox
import com.horizen.companion.SidechainSecretsCompanion
import com.horizen.companion.SidechainTransactionsCompanion
import com.horizen.consensus.ForgingStakeInfo
import com.horizen.params.MainNetParams
import com.horizen.params.NetworkParams
import com.horizen.params.RegTestParams
import com.horizen.params.TestNetParams
import com.horizen.proposition.Proposition
import com.horizen.secret.PrivateKey25519
import com.horizen.secret.VrfSecretKey
import com.horizen.transaction.SidechainTransaction
import com.horizen.transaction.mainchain.SidechainCreation
import com.horizen.utils.BytesUtils
import com.horizen.utils.MerklePath
import org.springframework.stereotype.Service
import scala.Option
import scala.collection.JavaConverters
import java.util.*

@Service
class GenesisInfoGenerator {
    fun generate(genesisInfoInput: GenesisInfoInput): GenesisInfoOutput {
        val secretsCompanion = SidechainSecretsCompanion(HashMap())

        val infoHex: String = genesisInfoInput.info

        val infoBytes = try {
            BytesUtils.fromHexString(infoHex)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("'info' expected to be a hex string.")
        }

        val secretHex: String = genesisInfoInput.secret

        val secretBytes = try {
            BytesUtils.fromHexString(secretHex)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("'secret' expected to be a hex string.")
        }

        val key = try {
            secretsCompanion.parseBytes(secretBytes) as PrivateKey25519
        } catch (e: Exception) {
            throw IllegalArgumentException("'secret' value is broken. Can't deserialize the key.")
        }

        val vrfSecretHex: String = genesisInfoInput.vrfSecret

        val vrfSecretBytes = try {
            BytesUtils.fromHexString(vrfSecretHex)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("'secret' expected to be a hex string.")
        }

        val vrfSecretKey = try {
            secretsCompanion.parseBytes(vrfSecretBytes) as VrfSecretKey
        } catch (e: Exception) {
            throw IllegalArgumentException("'vrfSecret' value is broken. Can't deserialize the key.")
        }

        // Undocumented optional argument, that is used in STF to decrease genesis block timestamps
        // to be able to generate next sc blocks without delays.
        // can be used only in Regtest network

        // Parsing the info: scid, powdata vector, mc block height, mc block hex, mc initial BlockSCTxCommTreeCumulativeHash
        var offset = 0
        try {
            val network = infoBytes[offset]
            offset += 1

            // Keep scId in original LE
            val scId = Arrays.copyOfRange(infoBytes, offset, offset + 32)
            offset += 32
            val powDataLength = BytesUtils.getVarInt(infoBytes, offset)
            offset += powDataLength.size()
            val powData = BytesUtils.toHexString(
                Arrays.copyOfRange(
                    infoBytes,
                    offset,
                    offset + powDataLength.value().toInt() * 8
                )
            )
            offset += (powDataLength.value() * 8).toInt()
            val mcBlockHeight = BytesUtils.getReversedInt(infoBytes, offset)
            offset += 4
            val initialCumulativeCommTreeHashLength = BytesUtils.getReversedVarInt(infoBytes, offset)
            offset += initialCumulativeCommTreeHashLength.size()

            // Note: we keep this value in Little endian as expected by sc-cryptolib
            val initialCumulativeCommTreeHash =
                Arrays.copyOfRange(infoBytes, offset, offset + initialCumulativeCommTreeHashLength.value().toInt())
            offset += initialCumulativeCommTreeHashLength.value().toInt()
            val mcNetworkName: String = getNetworkName(network)
            val params: NetworkParams = getNetworkParams(network, scId)

            val mcRef =
                MainchainBlockReference.create(Arrays.copyOfRange(infoBytes, offset, infoBytes.size), params).get()
            val sidechainTransactionsCompanion = SidechainTransactionsCompanion(HashMap())

            //Find Sidechain creation information
            var sidechainCreation: SidechainCreation? = null
            require(
                !mcRef.data().sidechainRelatedAggregatedTransaction().isEmpty
            ) { "Sidechain related data is not found in genesisinfo mc block." }
            for (output in mcRef.data().sidechainRelatedAggregatedTransaction().get().mc2scTransactionsOutputs()) {
                if (output is SidechainCreation) {
                    sidechainCreation = output
                }
            }
            requireNotNull(sidechainCreation) { "Sidechain creation transaction is not found in genesisinfo mc block." }
            val forgerBox = sidechainCreation.box
            val forgingStakeInfo =
                ForgingStakeInfo(forgerBox.blockSignProposition(), forgerBox.vrfPubKey(), forgerBox.value())
            val vrfMessage = "!SomeVrfMessage1!SomeVrfMessage2".toByteArray()
            val vrfProof = vrfSecretKey.prove(vrfMessage).key
            val mp = MerklePath(ArrayList())
            // In Regtest it possible to set genesis block timestamp to not to have block in future exception during STF tests.
            val currentTimeSeconds = System.currentTimeMillis() / 1000
            val sidechainBlock = SidechainBlock.create(
                params.sidechainGenesisBlockParentId(),
                SidechainBlock.BLOCK_VERSION(),
                currentTimeSeconds,
                JavaConverters.collectionAsScalaIterableConverter(listOf(mcRef.data())).asScala().toSeq(),
                JavaConverters.collectionAsScalaIterableConverter(ArrayList<SidechainTransaction<Proposition, NoncedBox<Proposition>>>())
                    .asScala().toSeq(),
                JavaConverters.collectionAsScalaIterableConverter(listOf(mcRef.header())).asScala().toSeq(),
                JavaConverters.collectionAsScalaIterableConverter(ArrayList<Ommer>()).asScala().toSeq(),
                key,
                forgingStakeInfo,
                vrfProof,
                mp,
                sidechainTransactionsCompanion,
                Option.empty()
            ).get()
            val withdrawalEpochLength: Int = try {
                val creationOutput =
                    sidechainBlock.mainchainBlockReferencesData().head().sidechainRelatedAggregatedTransaction().get()
                        .mc2scTransactionsOutputs()[0] as SidechainCreation
                creationOutput.withdrawalEpochLength()
            } catch (e: Exception) {
                throw IllegalStateException("'info' data is corrupted: MainchainBlock expected to contain a valid Transaction with a Sidechain Creation output.")
            }

            return GenesisInfoOutput(
                BytesUtils.toHexString(BytesUtils.reverseBytes(scId)),
                BytesUtils.toHexString(sidechainBlock.bytes()),
                powData,
                mcBlockHeight,
                mcNetworkName,
                withdrawalEpochLength,
                BytesUtils.toHexString(initialCumulativeCommTreeHash)
            )
        } catch (e: Exception) {
            throw IllegalStateException(String.format("Error: 'info' data is corrupted: %s", e.message))
        }
    }

    private fun getNetworkName(network: Byte): String {
        return when (network) {
            0.toByte() -> "mainnet"
            1.toByte() -> "testnet"
            2.toByte() -> "regtest"
            else -> ""
        }
    }

    private fun getNetworkParams(network: Byte, scId: ByteArray): NetworkParams {
        return when (network) {
            0.toByte() -> MainNetParams(scId, null, null, null, null, 1, 0, 100, 120, 720, null, 0, null, null, null, null, null)
            1.toByte() -> TestNetParams(scId, null, null, null, null, 1, 0, 100, 120, 720, null, 0, null, null, null, null, null)
            2.toByte() -> RegTestParams(scId, null, null, null, null, 1, 0, 100, 120, 720, null, 0, null, null, null, null, null)
            else -> throw IllegalStateException("Unexpected network type: $network")
        }
    }
}

data class GenesisInfoInput(
    val info: String,
    val secret: String,
    val vrfSecret: String
)

data class GenesisInfoOutput(
    val scId: String,
    val scGenesisBlockHex: String,
    val powData: String,
    val mcBlockHeight: Int,
    val mcNetwork: String,
    val withdrawalEpochLength: Int,
    val initialCumulativeCommTreeHash: String
)