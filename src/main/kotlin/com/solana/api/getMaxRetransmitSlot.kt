@file:Suppress("unused")

package com.solana.api

import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer

class GetMaxRetransmitSlotRequest : RpcRequest() {
    override val method: String = "getMaxRetransmitSlot"

}

internal fun getMaxRetransmitSlotSerializer() = Long.serializer()

suspend fun Api.getMaxRetransmitSlot(): Result<Long> =
    router.makeRequestResult(GetMaxRetransmitSlotRequest(), getMaxRetransmitSlotSerializer())
        .let { result ->
            @Suppress("UNCHECKED_CAST")
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(SolanaException("Can not be null"))
            else result as Result<Long> // safe cast, null case handled above
        }

fun Api.getMaxRetransmitSlot(onComplete: ((Result<Long>) -> Unit)) {
    CoroutineScope(dispatcher).launch {
        onComplete(getMaxRetransmitSlot())
    }
}

