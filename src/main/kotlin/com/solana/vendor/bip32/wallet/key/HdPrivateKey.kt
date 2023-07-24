package com.solana.vendor.bip32.wallet.key

class HdPrivateKey : HdKey() {
    private var _privateKey: ByteArray? = null
    val privateKey get() = _privateKey!!
    fun setPrivateKey(pk: ByteArray) {
        _privateKey = pk
    }
}