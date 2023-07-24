@file:Suppress("unused")

package com.solana.core

import com.solana.exception.ProgramException
import com.solana.programs.AssociatedTokenProgram
import com.solana.programs.TokenProgram
import com.solana.vendor.ByteUtils
import com.solana.vendor.TweetNaclFast
import com.solana.vendor.borshj.*
import org.bitcoinj.core.Base58
import org.bitcoinj.core.Sha256Hash
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*


data class PublicKey(val pubkey: ByteArray) : BorshCodable {
    init{
        require(pubkey.size <= PUBLIC_KEY_LENGTH) { "Invalid public key input" }
    }
    constructor(pubkeyString: String) : this(Base58.decode(pubkeyString))

    fun toByteArray(): ByteArray {
        return pubkey
    }

    fun toBase58(): String {
        return Base58.encode(pubkey)
    }

    fun equals(pubkey: PublicKey): Boolean {
        return this.pubkey.contentEquals(pubkey.toByteArray())
    }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + pubkey.contentHashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null) return false
        if (javaClass != other.javaClass) return false
        val person = other as PublicKey
        return equals(person)
    }

    override fun toString(): String {
        return toBase58()
    }

    class ProgramDerivedAddress(val address: PublicKey, val nonce: Int)
    companion object {
        const val PUBLIC_KEY_LENGTH = 32

        fun readPubkey(bytes: ByteArray, offset: Int): PublicKey {
            val buf = ByteUtils.readBytes(bytes, offset, PUBLIC_KEY_LENGTH)
            return PublicKey(buf)
        }

        fun createProgramAddress(seeds: List<ByteArray>, programId: PublicKey): PublicKey {
            val buffer = ByteArrayOutputStream()
            for (seed in seeds) {
                require(seed.size <= 32) { "Max seed length exceeded" }
                try {
                    buffer.write(seed)
                } catch (e: IOException) {
                    throw ProgramException(e)
                }
            }
            try {
                buffer.write(programId.toByteArray())
                buffer.write("ProgramDerivedAddress".toByteArray())
            } catch (e: IOException) {
                throw ProgramException(e)
            }
            val hash = Sha256Hash.hash(buffer.toByteArray())
            if (TweetNaclFast.is_on_curve(hash) != 0) {
                throw ProgramException("Invalid seeds, address must fall off the curve")
            }
            return PublicKey(hash)
        }

        @Throws(Exception::class)
        fun findProgramAddress(
            seeds: List<ByteArray>,
            programId: PublicKey
        ): ProgramDerivedAddress {
            var nonce = 255
            val address: PublicKey
            val seedsWithNonce: MutableList<ByteArray> = ArrayList()
            seedsWithNonce.addAll(seeds)
            while (nonce != 0) {
                address = try {
                    seedsWithNonce.add(byteArrayOf(nonce.toByte()))
                    createProgramAddress(seedsWithNonce, programId)
                } catch (e: Exception) {
                    seedsWithNonce.removeAt(seedsWithNonce.size - 1)
                    nonce--
                    continue
                }
                return ProgramDerivedAddress(address, nonce)
            }
            throw ProgramException("Unable to find a viable program address nonce")
        }

        @Throws(Exception::class)
        fun associatedTokenAddress(walletAddress: PublicKey, tokenMintAddress: PublicKey) : ProgramDerivedAddress {
            return findProgramAddress(
                listOf(
                    walletAddress.toByteArray(),
                    TokenProgram.PROGRAM_ID.toByteArray(),
                    tokenMintAddress.toByteArray()
                ),
                AssociatedTokenProgram.SPL_ASSOCIATED_TOKEN_ACCOUNT_PROGRAM_ID
            )
        }

        fun valueOf(publicKey: String): PublicKey {
            return PublicKey(publicKey)
        }
    }
}