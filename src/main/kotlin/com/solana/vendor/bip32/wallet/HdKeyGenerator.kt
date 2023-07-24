package com.solana.vendor.bip32.wallet

import com.solana.exception.SolanaException
import com.solana.vendor.bip32.crypto.Hash
import com.solana.vendor.bip32.crypto.HdUtil
import com.solana.vendor.bip32.crypto.HdUtil.append
import com.solana.vendor.bip32.crypto.HdUtil.getFingerprint
import com.solana.vendor.bip32.crypto.HdUtil.parseBigInteger
import com.solana.vendor.bip32.crypto.HmacSha512.hmac512
import com.solana.vendor.bip32.crypto.Secp256k1.deserializePoint
import com.solana.vendor.bip32.crypto.Secp256k1.n
import com.solana.vendor.bip32.crypto.Secp256k1.point
import com.solana.vendor.bip32.crypto.Secp256k1.serializePoint
import com.solana.vendor.bip32.wallet.key.HdPrivateKey
import com.solana.vendor.bip32.wallet.key.HdPublicKey
import net.i2p.crypto.eddsa.EdDSAPrivateKey
import net.i2p.crypto.eddsa.EdDSAPublicKey
import net.i2p.crypto.eddsa.spec.EdDSANamedCurveTable
import net.i2p.crypto.eddsa.spec.EdDSAParameterSpec
import net.i2p.crypto.eddsa.spec.EdDSAPrivateKeySpec
import net.i2p.crypto.eddsa.spec.EdDSAPublicKeySpec
import java.math.BigInteger
import java.nio.charset.StandardCharsets

class HdKeyGenerator {
    companion object {
        @JvmStatic
        private val ED25519SPEC: EdDSAParameterSpec = EdDSANamedCurveTable.getByName("ed25519")
        const val MASTER_PATH = "m"
    }

    fun getAddressFromSeed(seed: ByteArray?, solanaCoin: SolanaCoin): HdAddress {
        val curve = solanaCoin.curve
        val publicKey = HdPublicKey()
        val privateKey = HdPrivateKey()
        val address = HdAddress(privateKey, publicKey, solanaCoin, MASTER_PATH)
        val iv = hmac512(seed!!, curve.seed.toByteArray(StandardCharsets.UTF_8))

        //split into left/right
        val iLeft = iv.copyOfRange(0, 32)
        val iRight = iv.copyOfRange(32, 64)
        val masterSecretKey = parseBigInteger(iLeft)

        //In case IL is 0 or >=n, the master key is invalid.
        if (masterSecretKey.compareTo(BigInteger.ZERO) == 0 || masterSecretKey > n) {
            throw SolanaException("The master key is invalid")
        }
        privateKey.setDepth(0)
        privateKey.setFingerprint(byteArrayOf(0, 0, 0, 0))
        privateKey.setChildNumber(byteArrayOf(0, 0, 0, 0))
        privateKey.setKeyData(append(byteArrayOf(0), iLeft))
        privateKey.setPrivateKey(iLeft)
        privateKey.setChainCode(iRight)
        val point = point(masterSecretKey)

        publicKey.setDepth(0)
        publicKey.setFingerprint(byteArrayOf(0, 0, 0, 0))
        publicKey.setChildNumber(byteArrayOf(0, 0, 0, 0))
        publicKey.setKeyData(serializePoint(point))
        publicKey.setChainCode(iRight)

        val sk = EdDSAPrivateKey(EdDSAPrivateKeySpec(iLeft, ED25519SPEC))
        val pk = EdDSAPublicKey(EdDSAPublicKeySpec(sk.a, sk.params))
        publicKey.setPublicKey(append(byteArrayOf(0), pk.abyte))
        return address
    }

    fun getPublicKey(parent: HdPublicKey, child: Long, isHardened: Boolean): HdPublicKey {
        if (isHardened)
            throw SolanaException("Cannot derive child public keys from hardened keys")
        val key = parent.keyData
        val data = append(key, HdUtil.serializeToByteArray(child))
        val iv = hmac512(data, parent.chainCode)
        val ivLeft = iv.copyOfRange(0, 32)
        val ivRight = iv.copyOfRange(32, 64)
        val publicKey = HdPublicKey()
        publicKey.setVersion(parent.version)
        publicKey.setDepth(parent.depth + 1)
        val pKd = parent.keyData
        val h160 = Hash.H160.hash(pKd)
        val childFingerprint = byteArrayOf(h160[0], h160[1], h160[2], h160[3])
        val ivLeftBigInteger = parseBigInteger(ivLeft)
        var point = point(ivLeftBigInteger)
        point = point.add(deserializePoint(parent.keyData))
        if (ivLeftBigInteger > n || point.isInfinity)
            throw SolanaException("This key is invalid, should proceed to next key")
        val childKey = serializePoint(point)
        publicKey.setFingerprint(childFingerprint)
        publicKey.setChildNumber(HdUtil.serializeToByteArray(child))
        publicKey.setChainCode(ivRight)
        publicKey.setKeyData(childKey)
        return publicKey
    }

    fun getAddress(parent: HdAddress, tmpChild: Long, isHardened: Boolean): HdAddress {
        var child = tmpChild
        val privateKey = HdPrivateKey()
        val publicKey = HdPublicKey()
        val address = HdAddress(
            privateKey, publicKey, parent.coinType,
            getPath(parent.path, child, isHardened)
        )
        if (isHardened) {
            child += 0x80000000L
        }
        val xChain = parent.privateKey.chainCode
        val iv = if (isHardened) {
            val kpar = parseBigInteger(parent.privateKey.keyData)
            val data = append(append(byteArrayOf(0), HdUtil.serializeToByteArray(kpar)),
                HdUtil.serializeToByteArray(child)
            )
            hmac512(data, xChain)
        } else {
            val key = parent.publicKey.keyData
            val xPubKey = append(key, HdUtil.serializeToByteArray(child))
            hmac512(xPubKey, xChain)
        }

        val ivLeft = iv.copyOfRange(0, 32)
        val ivRight = iv.copyOfRange(32, 64)
        val parse256 = parseBigInteger(ivLeft)
        val kParam = parseBigInteger(parent.privateKey.keyData)
        val childSecretKey = parse256.add(kParam).mod(n)
        val childNumber = HdUtil.serializeToByteArray(child)
        val fingerprint = getFingerprint(parent.privateKey.keyData)
        privateKey.setVersion(parent.privateKey.version)
        privateKey.setDepth(parent.privateKey.depth + 1)
        privateKey.setFingerprint(fingerprint)
        privateKey.setChildNumber(childNumber)
        privateKey.setChainCode(ivRight)
        privateKey.setKeyData(append(byteArrayOf(0), HdUtil.serializeToByteArray(childSecretKey)))
        val point = point(childSecretKey)
        publicKey.setVersion(parent.publicKey.version)
        publicKey.setDepth(parent.publicKey.depth + 1)

        val pKd = parent.publicKey.keyData
        var h160 = Hash.H160.hash(pKd)
        var childFingerprint = byteArrayOf(h160[0], h160[1], h160[2], h160[3])
        publicKey.setFingerprint(childFingerprint)
        publicKey.setChildNumber(childNumber)
        publicKey.setChainCode(ivRight)
        publicKey.setKeyData(serializePoint(point))
        privateKey.setPrivateKey(ivLeft)
        h160 = Hash.H160.hash(parent.publicKey.publicKey)
        childFingerprint = byteArrayOf(h160[0], h160[1], h160[2], h160[3])
        publicKey.setFingerprint(childFingerprint)
        privateKey.setFingerprint(childFingerprint)
        privateKey.setKeyData(append(byteArrayOf(0), ivLeft))
        val sk = EdDSAPrivateKey(EdDSAPrivateKeySpec(ivLeft, ED25519SPEC))
        val pk = EdDSAPublicKey(EdDSAPublicKeySpec(sk.a, sk.params))
        publicKey.setPublicKey(append(byteArrayOf(0), pk.abyte))
        return address
    }

    private fun getPath(parentPath: String = MASTER_PATH, child: Long, isHardened: Boolean): String =
        parentPath + "/" + child + if (isHardened) "H" else ""
}