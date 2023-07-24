@file:Suppress("unused")

package com.solana.api

import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer

class GetSnapshotSlotRequest: RpcRequest() {
    override val method: String = "getSnapshotSlot"
}

internal fun getSnapshotSlotSerializer() = Long.serializer()

fun Api.getSnapshotSlot(onComplete: (Result<Long>) -> Unit) {
    CoroutineScope(dispatcher).launch {
        onComplete(getSnapshotSlot())
    }
}

suspend fun Api.getSnapshotSlot(): Result<Long> =
    router.makeRequestResult(GetSnapshotSlotRequest(), getSnapshotSlotSerializer())
        .let { result ->
            @Suppress("UNCHECKED_CAST")
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(SolanaException("Can not be null"))
            else result as Result<Long> // safe cast, null case handled above
        }