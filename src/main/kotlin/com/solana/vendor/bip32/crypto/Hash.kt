package com.solana.vendor.bip32.crypto

import org.bouncycastle.crypto.digests.RIPEMD160Digest
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Basic hash functions
 */
sealed interface Hash {
    fun hash(input: ByteArray): ByteArray

    data object SHA256 : Hash {
        /**
         * SHA-256
         *
         * @param input input
         * @return sha256(input)
         */
        override fun hash(input: ByteArray): ByteArray {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                digest.digest(input)
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException("Unable to find SHA-256", e)
            }
        }

        /**
         * sha256(sha256(bytes))
         *
         * @param bytes input
         * @return sha'd twice result
         */
        fun hashTwice(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): ByteArray {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                digest.update(bytes, offset, length)
                digest.update(digest.digest())
                digest.digest()
            } catch (e: NoSuchAlgorithmException) {
                throw RuntimeException("Unable to find SHA-256", e)
            }
        }
    }

    data object H160 : Hash {
        /**
         * H160
         *
         * @param input input
         * @return h160(input)
         */
        override fun hash(input: ByteArray): ByteArray {
            val sha256 = SHA256.hash(input)
            val digest = RIPEMD160Digest()
            digest.update(sha256, 0, sha256.size)
            val out = ByteArray(20)
            digest.doFinal(out, 0)
            return out
        }
    }
}