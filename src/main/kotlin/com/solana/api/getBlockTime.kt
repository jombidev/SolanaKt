@file:Suppress("unused")

package com.solana.api

import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

class GetBlockTimeRequest(block: Long) : RpcRequest() {
    override val method: String = "getBlockTime"
    override val params = buildJsonArray {
        add(block)
    }
}

internal fun getBlockTimeSerializer() = Long.serializer()

fun Api.getBlockTime(block: Long, onComplete: ((Result<Long>) -> Unit)) {
    CoroutineScope(dispatcher).launch {
        onComplete(getBlockTime(block))
    }
}

suspend fun Api.getBlockTime(block: Long): Result<Long> =
    router.makeRequestResult(GetBlockTimeRequest(block), getBlockTimeSerializer())
        .let { result ->
            @Suppress("UNCHECKED_CAST")
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(SolanaException("Can not be null"))
            else result as Result<Long> // safe cast, null case handled above
        }