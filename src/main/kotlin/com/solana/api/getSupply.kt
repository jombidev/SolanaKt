@file:Suppress("unused")

package com.solana.api

import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.SolanaResponseSerializer
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class GetSupplyRequest : RpcRequest() {
    override val method: String = "getSupply"
}

@Serializable
data class Supply (
    val total: Long,
    val circulating: Long,
    val nonCirculating: Long,
    val nonCirculatingAccounts: List<String>
)

internal fun getSupplySerializer() = SolanaResponseSerializer(Supply.serializer())

suspend fun Api.getSupply(): Result<Supply> =
    router.makeRequestResult(GetSupplyRequest(), getSupplySerializer())
        .let { result ->
            @Suppress("UNCHECKED_CAST")
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(SolanaException("Can not be null"))
            else result as Result<Supply> // safe cast, null case handled above
        }

fun Api.getSupply(onComplete: ((Result<Supply>) -> Unit)){
    CoroutineScope(dispatcher).launch {
        onComplete(getSupply())
    }
}