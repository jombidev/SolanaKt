@file:Suppress("unused")

package com.solana.api

import com.solana.exception.SolanaException
import com.solana.networking.RpcRequest
import com.solana.networking.makeRequestResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

class GetEpochInfoRequest : RpcRequest() {
    override val method: String = "getEpochInfo"
}

@Serializable
data class EpochInfo (
    val absoluteSlot: Long,
    val blockHeight: Long,
    val epoch: Long,
    val slotIndex: Long,
    val slotsInEpoch: Long
)

internal fun getEpochInfoSerializer() = EpochInfo.serializer()

fun Api.getEpochInfo(onComplete: ((Result<EpochInfo>) -> Unit)) {
    CoroutineScope(dispatcher).launch {
        onComplete(getEpochInfo())
    }
}

suspend fun Api.getEpochInfo(): Result<EpochInfo> =
    router.makeRequestResult(GetEpochInfoRequest(), getEpochInfoSerializer())
        .let { result ->
            @Suppress("UNCHECKED_CAST")
            if (result.isSuccess && result.getOrNull() == null)
                Result.failure(SolanaException("Can not be null"))
            else result as Result<EpochInfo> // safe cast, null case handled above
        }