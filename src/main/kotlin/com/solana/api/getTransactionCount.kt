@file:Suppress("unused")

package com.solana.api

import com.solana.exception.TransactionException
import com.solana.networking.RpcRequest
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer

class GetTransactionCountRequest : RpcRequest() {
    override val method: String = "getTransactionCount"
}

internal fun getTransactionCountSerializer() = Long.serializer()

fun Api.getTransactionCount(onComplete: ((Result<Long>) -> Unit)) {
    CoroutineScope(dispatcher).launch {
        onComplete(getSnapshotSlot())
    }
}

suspend fun Api.getTransactionCount(): Result<Long> =
    router.makeRequestResult(GetTransactionCountRequest(), getTransactionCountSerializer())
        .let { result ->
            @Suppress("UNCHECKED_CAST")
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(TransactionException("Can not be null"))
            else result as Result<Long> // safe cast, null case handled above
        }