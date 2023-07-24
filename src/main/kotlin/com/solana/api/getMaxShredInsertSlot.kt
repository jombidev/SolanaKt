@file:Suppress("unused")

package com.solana.api

import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer

class GetMaxShredInsertSlotRequest : RpcRequest() {
    override val method: String = "getMaxShredInsertSlot"
}

internal fun getMaxShredInsertSlotSerializer() = Long.serializer()

suspend fun Api.getMaxShredInsertSlot(): Result<Long> =
    router.makeRequestResult(GetMaxShredInsertSlotRequest(), getMaxShredInsertSlotSerializer())
        .let { result ->
            @Suppress("UNCHECKED_CAST")
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(SolanaException("Can not be null"))
            else result as Result<Long> // safe cast, null case handled above
        }

fun Api.getMaxShredInsertSlot(onComplete: (Result<Long>) -> Unit) {
    CoroutineScope(dispatcher).launch {
        onComplete(getMaxShredInsertSlot())
    }
}