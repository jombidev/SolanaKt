@file:Suppress("unused")

package com.solana.api

import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer

class MinimumLedgerSlotRequest : RpcRequest() {
    override val method: String = "minimumLedgerSlot"
}

internal fun minimumLedgerSlotSerializer() = Long.serializer()

fun Api.minimumLedgerSlot(onComplete: ((Result<Long>) -> Unit)) {
    CoroutineScope(dispatcher).launch {
        onComplete(minimumLedgerSlot())
    }
}

suspend fun Api.minimumLedgerSlot(): Result<Long> =
    router.makeRequestResult(MinimumLedgerSlotRequest(), minimumLedgerSlotSerializer())
        .let { result ->
            @Suppress("UNCHECKED_CAST")
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(SolanaException("Can not be null"))
            else result as Result<Long> // safe cast, null case handled above
        }