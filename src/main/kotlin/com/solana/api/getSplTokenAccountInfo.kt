@file:Suppress("unused")

package com.solana.api

import com.solana.exception.SolanaException
import com.solana.core.PublicKey
import com.solana.models.RpcSendTransactionConfig
import com.solana.networking.serialization.serializers.solana.SolanaResponseSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class SplTokenAccountTokenInfo(
    val isNative: Boolean?,
    val mint: String?,
    val owner: String?,
    val state: String?,
    val tokenAmount: TokenAmountInfo?
)

@Serializable
data class SplTokenAccountInfoParsedData(
    val info: SplTokenAccountTokenInfo,
    val type: String
)

@Serializable
data class SplTokenAccountInfo(
    val parsed: SplTokenAccountInfoParsedData,
    val program: String,
    val space: Int? = null
)

@Serializable
data class SplTokenAccountValue(
    val data: SplTokenAccountInfo
)

suspend fun Api.getSplTokenAccountInfo(account: PublicKey): Result<SplTokenAccountInfo> =
    getAccountInfo(
        account,
        SolanaResponseSerializer(SplTokenAccountValue.serializer()),
        encoding = RpcSendTransactionConfig.Encoding.jsonParsed
    ).map { it?.data ?: throw SolanaException("Can not be null") }

fun Api.getSplTokenAccountInfo(account: PublicKey, onComplete: (Result<SplTokenAccountInfo>) -> Unit) {
    CoroutineScope(dispatcher).launch {
        onComplete(getSplTokenAccountInfo(account))
    }
}