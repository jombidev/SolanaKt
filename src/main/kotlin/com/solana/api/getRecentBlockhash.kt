@file:Suppress("unused")

package com.solana.api

import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.SolanaResponseSerializer
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

class RecentBlockhashRequest : RpcRequest() {
    override val method: String = "getRecentBlockhash"
}

@Serializable
internal data class BlockhashResponse(val blockhash: String, val feeCalculator: JsonElement)

internal fun blockhashSerializer() = SolanaResponseSerializer(BlockhashResponse.serializer())

suspend fun Api.getRecentBlockhash(): Result<String> =
    router.makeRequestResult(RecentBlockhashRequest(), blockhashSerializer()).let { result ->
        if (result.isSuccess && result.getOrNull() == null)
            Result.failure(SolanaException("Can not be null"))
        else result.map { it!!.blockhash } // safe cast, null case handled above, and it's safe without cast
    }

fun Api.getRecentBlockhash(onComplete: ((Result<String>) -> Unit)) {
    CoroutineScope(dispatcher).launch {
        onComplete(getRecentBlockhash())
    }
}
