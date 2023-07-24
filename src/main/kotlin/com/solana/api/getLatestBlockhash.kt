@file:Suppress("unused")

package com.solana.api

import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.SolanaResponseSerializer
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class LatestBlockhashRequest : RpcRequest() {
    override val method: String = "getLatestBlockhash"
}

@Serializable
internal data class LatestBlockHashResponse(val blockhash: String, val lastValidBlockHeight: Double)

internal fun latestBlockhashSerializer() = SolanaResponseSerializer(LatestBlockHashResponse.serializer())

suspend fun Api.getLatestBlockhash(): Result<String> =
    router.makeRequestResult(LatestBlockhashRequest(), latestBlockhashSerializer()).let { result ->
        if (result.isSuccess && result.getOrNull() == null)
            Result.failure(SolanaException("Can not be null"))
        else result.map { it!!.blockhash }
    }

fun Api.getLatestBlockhash(onComplete: ((Result<String>) -> Unit)) {
    CoroutineScope(dispatcher).launch {
        onComplete(getRecentBlockhash())
    }
}
