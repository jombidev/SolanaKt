@file:Suppress("unused")

package com.solana.api

import com.solana.core.PublicKey
import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.SolanaResponseSerializer
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*

class GetBalanceRequest(accountAddress: String) : RpcRequest() {
    override val method: String = "getBalance"
    override val params = buildJsonArray {
        add(accountAddress)
    }
}

internal fun getBalanceSerializer() = SolanaResponseSerializer(Long.serializer())

suspend fun Api.getBalance(account: PublicKey): Result<Long> =
    router.makeRequestResult(GetBalanceRequest(account.toBase58()), getBalanceSerializer())
        .let { result ->
            @Suppress("UNCHECKED_CAST")
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(SolanaException("Can not be null"))
            else result as Result<Long> // safe cast, null case handled above
        }

fun Api.getBalance(account: PublicKey, onComplete: ((Result<Long>) -> Unit)) {
    CoroutineScope(dispatcher).launch {
        onComplete(getBalance(account))
    }
}