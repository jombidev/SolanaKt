package com.solana.vendor.bip32.wallet.key

import com.solana.exception.SolanaException
import com.solana.vendor.bip32.crypto.Hash.SHA256.hashTwice
import java.io.ByteArrayOutputStream
import java.io.IOException

open class HdKey() {
    constructor(
        version: ByteArray,
        depth: Int,
        fingerprint: ByteArray,
        childNumber: ByteArray,
        chainCode: ByteArray,
        keyData: ByteArray
    ) : this() {
        _version = version
        _depth = depth
        _fingerprint = fingerprint
        _childNumber = childNumber
        _chainCode = chainCode
        _keyData = keyData
    }
    private var _version: ByteArray? = null
    private var _depth: Int? = null
    private var _fingerprint: ByteArray? = null
    private var _childNumber: ByteArray? = null
    private var _chainCode: ByteArray? = null
    private var _keyData: ByteArray? = null


    fun setVersion(version: ByteArray) {
        _version = version
    }

    fun setDepth(depth: Int) {
        _depth = depth
    }

    fun setFingerprint(fingerprint: ByteArray) {
        _fingerprint = fingerprint
    }

    fun setChildNumber(childNumber: ByteArray) {
        _childNumber = childNumber
    }

    fun setChainCode(chainCode: ByteArray) {
        _chainCode = chainCode
    }

    fun setKeyData(keyData: ByteArray) {
        _keyData = keyData
    }

    val version: ByteArray get() = _version!!
    val depth: Int get() = _depth!!
    private val fingerprint: ByteArray get() = _fingerprint!!
    private val childNumber: ByteArray get() = _childNumber!!
    val chainCode: ByteArray get() = _chainCode!!
    val keyData: ByteArray get() = _keyData!!


    fun getKey(): ByteArray? {
        val key = ByteArrayOutputStream()
        try {
            key.write(version)
            key.write(byteArrayOf(depth.toByte()))
            key.write(fingerprint)
            key.write(childNumber)
            key.write(chainCode)
            key.write(keyData)
            val checksum = hashTwice(key.toByteArray(), 0, key.size())
            key.write(checksum.copyOfRange(0, 4))
        } catch (e: IOException) {
            throw SolanaException("Unable to write key")
        }
        return key.toByteArray()
    }
}