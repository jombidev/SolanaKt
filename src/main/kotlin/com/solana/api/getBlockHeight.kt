@file:Suppress("unused")

package com.solana.api

import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer

class GetBlockHeightRequest : RpcRequest() {
    override val method: String = "getBlockHeight"
}

internal fun getBlockHeightSerializer() = Long.serializer()

fun Api.getBlockHeight(onComplete: ((Result<Long>) -> Unit)) {
    CoroutineScope(dispatcher).launch {
        onComplete(getBlockHeight())
    }
}

suspend fun Api.getBlockHeight(): Result<Long> =
    router.makeRequestResult(GetBlockHeightRequest(), getBlockHeightSerializer())
        .let { result ->
            @Suppress("UNCHECKED_CAST")
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(SolanaException("Can not be null"))
            else result as Result<Long> // safe cast, null case handled above
        }