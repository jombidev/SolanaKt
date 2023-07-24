@file:Suppress("unused")

package com.solana.api

import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer

class GetSlotRequest : RpcRequest() {
    override val method: String = "getSlot"
}

internal fun getSlotSerializer() = Long.serializer()

suspend fun Api.getSlot(): Result<Long> =
    router.makeRequestResult(GetSlotRequest(), getSlotSerializer())
        .let { result ->
            @Suppress("UNCHECKED_CAST")
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(SolanaException("Can not be null"))
            else result as Result<Long> // safe cast, null case handled above
        }

fun Api.getSlot(onComplete: (Result<Long>) -> Unit) {
    CoroutineScope(dispatcher).launch {
        onComplete(getSlot())
    }
}