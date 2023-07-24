package com.solana.core

import com.solana.vendor.TweetNaclFast
import com.solana.vendor.bip32.wallet.DerivableType
import com.solana.vendor.bip32.wallet.SolanaBip44
import org.bitcoinj.crypto.DeterministicHierarchy
import org.bitcoinj.crypto.HDKeyDerivation
import org.bitcoinj.crypto.HDUtils
import org.bitcoinj.crypto.MnemonicCode
import java.nio.ByteBuffer
import java.util.*


sealed class DerivationPath(val path: String) {
    data object DEPRECATED_M_501H_0H_0_0 : DerivationPath("M/501H/0H/0/0")
    data object BIP44_M_44H_501H_0H : DerivationPath("M/44H/501H/0H")
    data object BIP44_M_44H_501H_0H_OH : DerivationPath("M/44H/501H/0H/0H")

}

interface Account {
    val publicKey: PublicKey
    fun sign(serializedMessage: ByteArray): ByteArray
}

class HotAccount private constructor(private var keyPair: TweetNaclFast.Signature.KeyPair) : Account {
    constructor() : this(TweetNaclFast.Signature.keyPair())

    constructor(secretKey: ByteArray) : this(TweetNaclFast.Signature.keyPair_fromSecretKey(secretKey))

//    private constructor(keyPair: TweetNaclFast.Signature.KeyPair) {
//        this.keyPair = keyPair
//    }

    override val publicKey: PublicKey
        get() = PublicKey(keyPair.publicKey)

    override fun sign(serializedMessage: ByteArray): ByteArray {
        val signatureProvider = TweetNaclFast.Signature(ByteArray(0), secretKey)
        return signatureProvider.detached(serializedMessage)
    }

    private val secretKey: ByteArray
        get() = keyPair.secretKey

    companion object {
        /**
         * Derive a Solana account from a Mnemonic generated by the Solana CLI using bip44 Mnemonic with deviation path of
         * m/55H/501H/0H
         * @param words seed words
         * @param passphrase seed passphrase
         * @return Solana account
         */
        private fun fromBip44Mnemonic(words: List<String>, passphrase: String): HotAccount {
            val solanaBip44 = SolanaBip44()
            val seed = MnemonicCode.toSeed(words, passphrase)
            val privateKey = solanaBip44.getPrivateKeyFromSeed(seed, DerivableType.BIP44)
            return HotAccount(TweetNaclFast.Signature.keyPair_fromSeed(privateKey))
        }

        /**
         * Derive a Solana account from a Mnemonic generated by the Solana CLI using bip44 Mnemonic with deviation path of
         * m/55H/501H/0H/0H
         * @param words seed words
         * @param passphrase seed passphrase
         * @return Solana account
         */
        private fun fromBip44MnemonicWithChange(words: List<String>, passphrase: String): HotAccount {
            val solanaBip44 = SolanaBip44()
            val seed = MnemonicCode.toSeed(words, passphrase)
            val privateKey = solanaBip44.getPrivateKeyFromSeed(seed, DerivableType.BIP44CHANGE)
            return HotAccount(TweetNaclFast.Signature.keyPair_fromSeed(privateKey))
        }


        private fun fromDeprecatedMnemonic(words: List<String>, passphrase: String): HotAccount {
            val seed = MnemonicCode.toSeed(words, passphrase)
            val masterPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed)
            val deterministicHierarchy = DeterministicHierarchy(masterPrivateKey)
            val child = deterministicHierarchy[HDUtils.parsePath(DerivationPath.DEPRECATED_M_501H_0H_0_0.path), true, true]
            val keyPair = TweetNaclFast.Signature.keyPair_fromSeed(child.privKeyBytes)
            return HotAccount(keyPair)
        }

        fun fromMnemonic(words: List<String>, passphrase: String, derivationPath: DerivationPath = DerivationPath.BIP44_M_44H_501H_0H_OH): HotAccount {
            return when (derivationPath){
                is DerivationPath.DEPRECATED_M_501H_0H_0_0 -> fromDeprecatedMnemonic(words, passphrase)
                is DerivationPath.BIP44_M_44H_501H_0H -> fromBip44Mnemonic(words, passphrase)
                is DerivationPath.BIP44_M_44H_501H_0H_OH -> fromBip44MnemonicWithChange(words, passphrase)
            }
        }

        /**
         * Creates an [HotAccount] object from a Sollet-exported JSON string (array)
         * @param json Sollet-exported JSON string (array)
         * @return [HotAccount] built from Sollet-exported private key
         */
        fun fromJson(json: String): HotAccount {
            return HotAccount(convertJsonStringToByteArray(json))
        }

        /**
         * Convert's a Sollet-exported JSON string into a byte array usable for [HotAccount] instantiation
         * @param characters Sollet-exported JSON string
         * @return byte array usable in [HotAccount] instantiation
         */
        private fun convertJsonStringToByteArray(characters: String): ByteArray {
            // Create resulting byte array
            val buffer = ByteBuffer.allocate(64)

            // Convert json array into String array
            val sanitizedJson = characters.replace("\\[".toRegex(), "").replace("]".toRegex(), "")
            val chars = sanitizedJson.split(",").toTypedArray()

            // Convert each String character into byte and put it in the buffer
            Arrays.stream(chars).forEach { character: String ->
                val byteValue = character.toInt().toByte()
                buffer.put(byteValue)
            }
            return buffer.array()
        }
    }
}