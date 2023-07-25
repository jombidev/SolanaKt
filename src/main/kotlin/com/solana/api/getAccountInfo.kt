@file:Suppress("unused")

package com.solana.api

import com.solana.core.PublicKey
import com.solana.models.RpcSendTransactionConfig
import com.solana.models.buffer.AccountInfoData
import com.solana.networking.*
import com.solana.networking.serialization.serializers.base64.BorshAsBase64JsonArraySerializer
import com.solana.networking.serialization.serializers.solana.AnchorAccountSerializer
import com.solana.networking.serialization.serializers.solana.SolanaResponseSerializer
import com.solana.vendor.ResultError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

class AccountRequest(
    accountAddress: String,
    encoding: Encoding = Encoding.base64,
    commitment: String = Commitment.MAX.toString(),
    length: Int? = null,
    offset: Int? = length?.let { 0 }
) : RpcRequest() {

    constructor(account: String, transactionOptions: TransactionOptions) : this(account,
        transactionOptions.encoding, commitment = transactionOptions.commitment.toString())

    override val method = "getAccountInfo"
    override val params = buildJsonArray {
        add(accountAddress)
        addJsonObject {
            put("encoding", encoding.getEncoding())
            put("commitment", commitment)
            length?.let {
                putJsonObject("dataSlice") {
                    put("length", length)
                    put("offset", offset)
                }
            }
        }
    }
}

@Serializable
data class AccountInfo<D>(val data: D?, val executable: Boolean,
                          val lamports: Long, val owner: String?, val rentEpoch: Long)

@OptIn(ExperimentalSerializationApi::class)
fun <A> solanaAccountSerializer(serializer: KSerializer<A>) =
    accountInfoSerializer(
        BorshAsBase64JsonArraySerializer(
            AnchorAccountSerializer(serializer.descriptor.serialName, serializer)
        )
    )

fun <D> accountInfoSerializer(serializer: KSerializer<D>) =
    SolanaResponseSerializer(AccountInfo.serializer(serializer))

inline fun <reified A> solanaAccountSerializer() =
    accountInfoSerializer<A?>(BorshAsBase64JsonArraySerializer(AnchorAccountSerializer()))

inline fun <reified T> Api.getAccountInfo(
    account: PublicKey,
    serializer: KSerializer<T>,
    commitment: Commitment = Commitment.MAX,
    encoding: RpcSendTransactionConfig.Encoding = Encoding.base64,
    length: Int? = null,
    offset: Int? = length?.let { 0 },
    crossinline onComplete: ((Result<T>) -> Unit)
) {
    CoroutineScope(dispatcher).launch {
        onComplete(getAccountInfo(account, serializer, commitment, encoding, length, offset))
    }
}

suspend fun Api.getDefaultInfo(
    account: PublicKey,
    commitment: Commitment = Commitment.MAX,
    encoding: Encoding = Encoding.base64,
    length: Int? = null,
    offset: Int? = length?.let { 0 }
): Result<AccountInfo<AccountInfoData?>> {
    return router.makeRequestResult(
        AccountRequest(
            accountAddress = account.toBase58(),
            encoding = encoding,
            commitment = commitment.toString(),
            length = length,
            offset = offset
        ),
        accountInfoSerializer(BorshAsBase64JsonArraySerializer((AccountInfoData.serializer())))
    ).let { result ->
        @Suppress("UNCHECKED_CAST")
        if (result.isSuccess && result.getOrNull() == null)
            Result.failure(nullValueError)
        else result as Result<AccountInfo<AccountInfoData?>> // safe cast, null case handled above
    }
}

suspend inline fun <reified A> Api.getAccountInfo(
    account: PublicKey,
    serializer: KSerializer<A>,
    commitment: Commitment = Commitment.MAX,
    encoding: Encoding = Encoding.base64,
    length: Int? = null,
    offset: Int? = length?.let { 0 }
): Result<A> {
    return router.makeRequestResult(
        AccountRequest(
            accountAddress = account.toBase58(),
            encoding = encoding,
            commitment = commitment.toString(),
            length = length,
            offset = offset
        ),
        serializer
    ).let { result ->
        @Suppress("UNCHECKED_CAST")
        if (result.isSuccess && result.getOrNull() == null)
            Result.failure(nullValueError)
        else result as Result<A> // safe cast, null case handled above
    }
}
val nullValueError = ResultError("Account return Null")
