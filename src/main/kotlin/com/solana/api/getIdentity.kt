@file:Suppress("unused")

package com.solana.api

import com.solana.core.PublicKey
import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.makeRequestResult
import com.solana.networking.serialization.serializers.solana.PublicKeyAsStringSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class GetIdentityBlockRequest : RpcRequest() {
    override val method: String = "getIdentity"
}

@Serializable
data class GetIdentityResponse (
    @Serializable(with = PublicKeyAsStringSerializer::class) val identity: PublicKey
)

internal fun getIdentitySerializer() = GetIdentityResponse.serializer()

fun Api.getIdentity(onComplete: (Result<PublicKey>) -> Unit) {
    CoroutineScope(dispatcher).launch {
        onComplete(getIdentity())
    }
}

suspend fun Api.getIdentity(): Result<PublicKey> =
    router.makeRequestResult(GetIdentityBlockRequest(), getIdentitySerializer())
        .let { result ->
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(SolanaException("Can not be null"))
            else result.map {
                it!!.identity
            } // safe cast, null case handled above, and it's now safe in native
        }