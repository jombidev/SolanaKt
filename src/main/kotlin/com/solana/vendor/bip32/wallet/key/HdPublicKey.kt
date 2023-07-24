package com.solana.vendor.bip32.wallet.key

class HdPublicKey : HdKey() {
    private var _publicKey: ByteArray? = null
    val publicKey get() = _publicKey!!
    fun setPublicKey(pk: ByteArray) {
        _publicKey = pk
    }
}