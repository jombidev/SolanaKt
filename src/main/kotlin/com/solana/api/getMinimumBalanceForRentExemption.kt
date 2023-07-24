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


class GetMinimumBalanceForRentExemptionRequest(dataLength: Long) : RpcRequest() {
    override val method: String = "getMinimumBalanceForRentExemption"
    override val params = buildJsonArray {
        add(dataLength)
    }
}

internal fun getMinimumBalanceForRentExemptionSerializer() = Long.serializer()

suspend fun Api.getMinimumBalanceForRentExemption(dataLength: Long): Result<Long> =
    router.makeRequestResult(GetMinimumBalanceForRentExemptionRequest(dataLength), getMinimumBalanceForRentExemptionSerializer())
        .let { result ->
            @Suppress("UNCHECKED_CAST")
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(SolanaException("Can not be null"))
            else result as Result<Long> // safe cast, null case handled above
        }

fun Api.getMinimumBalanceForRentExemption(dataLength: Long,  onComplete: ((Result<Long>) -> Unit)) {
    CoroutineScope(dispatcher).launch {
        onComplete(getMinimumBalanceForRentExemption(dataLength))
    }
}