package com.solana.vendor

import com.solana.exception.SolanaException
import org.bitcoinj.core.Utils
import java.io.OutputStream
import java.math.BigInteger
import java.util.*
import kotlin.experimental.and

@Suppress("unused")
object ByteUtils {
    private const val UINT_32_LENGTH = 4
    private const val UINT_64_LENGTH = 8
    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
    @JvmStatic
    fun readBytes(buf: ByteArray, offset: Int, length: Int): ByteArray {
        val b = ByteArray(length)
        System.arraycopy(buf, offset, b, 0, length)
        return b
    }

    @JvmStatic
    fun readUint64(buf: ByteArray, offset: Int): BigInteger {
        return BigInteger(Utils.reverseBytes(readBytes(buf, offset, UINT_64_LENGTH)))
    }

    fun readUint64Price(buf: ByteArray, offset: Int): BigInteger {
        return BigInteger(readBytes(buf, offset, UINT_64_LENGTH))
    }

    @JvmStatic
    @Throws(SolanaException::class)
    fun uint64ToByteStreamLE(value: BigInteger, stream: OutputStream) {
        var bytes = value.toByteArray()
        if (bytes.size > 8) {
            bytes = if (bytes[0] == 0.toByte()) { // bruh
                readBytes(bytes, 1, bytes.size - 1)
            } else {
                throw SolanaException("Input too large to encode into a uint64")
            }
        }
        bytes = Utils.reverseBytes(bytes)
        stream.write(bytes)
        if (bytes.size < 8) {
            for (i in 0..<8 - bytes.size) stream.write(0)
        }
    }

    fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }.uppercase() // lol
//        val hexChars = CharArray(bytes.size * 2)
//        for (j in bytes.indices) {
//            val v = (bytes[j] and 0xFF.toByte()).toInt()
//            hexChars[j * 2] = HEX_ARRAY[v ushr 4]
//            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
//        }
//        return String(hexChars)
    }

    fun trim(bytes: ByteArray): ByteArray {
        var i = bytes.size - 1
        while (i >= 0 && bytes[i] == 0.toByte()) {
            --i
        }
        return Arrays.copyOf(bytes, i + 1)
    }

    fun getBit(data: ByteArray, pos: Int): Int {
        val posByte = pos / 8
        val posBit = pos % 8
        val valByte = data[posByte].toInt()
        return (valByte shr posBit) and 1
    }
}
//fun ByteArray.toInt32(): Int {
//    if (this.size != 4) {
//        throw Exception("wrong len")
//    }
//    this.reverse()
//    return ByteBuffer.wrap(this).int
//}
//
//fun ByteArray.toLong(): Long {
//    if (this.size != 8) {
//        throw Exception("wrong len")
//    }
//    this.reverse()
//    return ByteBuffer.wrap(this).long
//}