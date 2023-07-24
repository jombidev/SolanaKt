package com.solana.networking.socket

import com.solana.api.AccountInfo
import com.solana.api.ProgramAccountSerialized
import com.solana.models.buffer.AccountInfoData
import com.solana.networking.*
import com.solana.networking.serialization.serializers.base64.BorshAsBase64JsonArraySerializer
import com.solana.networking.socket.models.LogsNotification
import com.solana.networking.socket.models.SignatureNotification
import com.solana.networking.socket.models.SocketMethod
import com.solana.networking.socket.models.SocketResponse
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.utils.io.CancellationException
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import kotlin.io.encoding.ExperimentalEncodingApi

sealed class SolanaSocketError : Exception() {
    data object Disconnected : SolanaSocketError()
    data object CouldNotSerialize : SolanaSocketError()
    data object CouldNotWrite : SolanaSocketError()
}

abstract class SolanaSocketEventsDelegate {
    open fun start() {}
    open fun stop() {}
    open fun accountNotification(notification: SocketResponse<AccountInfo<AccountInfoData?>>) {}
    open fun programNotification(notification: SocketResponse<ProgramAccountSerialized<AccountInfo<AccountInfoData?>>>) {}
    open fun signatureNotification(notification: SocketResponse<SignatureNotification>) {}
    open fun logsNotification(notification: SocketResponse<LogsNotification>) {}
    open fun unsubscribed(id: String) {}
    open fun subscribed(socketId: Int, id: String) {}
    open fun error(error: Exception) {}
}

private val LOGGER = LoggerFactory.getLogger(SolanaSocket::class.java)


private val json = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

class SolanaSocket(
    private val endpoint: RPCEndpoint,
    private val client: HttpClient = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 30_000
        }
    },
) {
    private var socket: DefaultClientWebSocketSession? = null
    private var delegate: SolanaSocketEventsDelegate? = null
    private val pool = Executors.newCachedThreadPool().asCoroutineDispatcher()
    private var _worker: Deferred<Unit>? = null
    private var running = false

    fun start(delegate: SolanaSocketEventsDelegate) {
        this.delegate = delegate
        _worker = CoroutineScope(pool).async {
            LOGGER.info("worker started")
            while (true) {
                try {
                    socket = client.webSocketSession { url(endpoint.urlWebSocket) }
                    running = true
                    LOGGER.info("connected")
                    delegate.start()

                    try {
                        readSocket()
                    } catch (e: Exception) {
                        LOGGER.error("Error: ", e)
                        delegate.error(e)
                    }
                    running = false
                    delegate.stop()
                    try {
                        handleClose()
                    } catch (e: RuntimeException) {
                        LOGGER.error("Stopped.")
                        break
                    } catch (e: Exception) {
                        LOGGER.error("Error while closing", e)
                        delegate.error(e)
                    }
                    socket = null
                } catch (exception: Exception) {
                    delegate.error(exception)
                    LOGGER.error("error while open connection", exception)
                    delay(10000L)
                    continue
                }

                LOGGER.warn("Socket closed, trying to reconnecting...")
            }
        }
        _worker?.start()
    }

    private suspend fun handleClose() {
        val reason = withTimeoutOrNull(1500) {
            socket!!.closeReason.await()
        } ?: return

        LOGGER.info("Gateway closed: {} {}", reason.code, reason.message)

        when (reason.message) {
            "USER_STOP" -> throw RuntimeException("User stop")
        }
    }

    private fun <T> ReceiveChannel<T>.asFlow() = flow {
        try {
            for (value in this@asFlow) emit(value)
        } catch (ignore: CancellationException) {
            //reading was stopped from somewhere else, ignore
        }
    }

    private suspend fun readSocket() {
        socket!!.incoming.asFlow().buffer(Channel.UNLIMITED).collect {
            when (it) {
                is Frame.Binary, is Frame.Text -> read(it)
                else -> { /*ignore*/
                }
            }
        }
    }

    private fun read(frame: Frame) {
        LOGGER.trace("Received raw frame: {}", frame)
        val text = frame.data.decodeToString()

        try {
            val dictJson = json.decodeFromString(RPCResponseMethond.serializer(), text)
            val methodString = dictJson.method
            methodString?.let {
                when (it) {
                    SocketMethod.accountNotification.string -> {
                        val serializer = SocketResponse.serializer(
                            AccountInfo.serializer(BorshAsBase64JsonArraySerializer(AccountInfoData.serializer()))
                        )
                        json.decodeFromString(serializer, text).let { response ->
                            delegate?.accountNotification(response)
                        }
                    }

                    SocketMethod.signatureNotification.string -> {
                        val serializer = SocketResponse.serializer(
                            (SignatureNotification.serializer())
                        )
                        json.decodeFromString(serializer, text).let { response ->
                            delegate?.signatureNotification(response)
                        }
                    }

                    SocketMethod.logsNotification.string -> {
                        val serializer = SocketResponse.serializer(
                            (LogsNotification.serializer())
                        )
                        json.decodeFromString(serializer, text).let { response ->
                            delegate?.logsNotification(response)
                        }
                    }

                    SocketMethod.programNotification.string -> {
                        val serializer = SocketResponse.serializer(
                            ProgramAccountSerialized.serializer(
                                AccountInfo.serializer(
                                    BorshAsBase64JsonArraySerializer(AccountInfoData.serializer().nullable)
                                )
                            )
                        )

                        json.decodeFromString(serializer, text).let { response ->
                            delegate?.programNotification(response)
                        }
                    }

                    else -> {}
                }
            } ?: run {
                if (dictJson.result?.intOrNull is Int) {
                    val serializer = RpcResponse.serializer(Int.serializer())

                    json.decodeFromString(serializer, text).let { response ->
                        response.result?.let { result ->
                            response.id?.let { id ->
                                delegate?.subscribed(
                                    result,
                                    id
                                )
                            }
                        }
                    }
                }

                if (dictJson.result?.booleanOrNull is Boolean) {
                    val serializer = RpcResponse.serializer(Boolean.serializer())
                    json.decodeFromString(serializer, text).let { response ->
                        response.id?.let { delegate?.unsubscribed(it) }
                    }
                }
            }
        } catch (error: Exception) {
            delegate?.error(error)
        }
    }

    suspend fun stop() {
        socket?.close(CloseReason(1000, "USER_STOP"))
        _worker?.cancelAndJoin()
    }

    suspend fun accountSubscribe(publicKey: String, commitment: Commitment = Commitment.RECENT): Result<String> {
        val params = buildJsonArray {
            add(publicKey)
            add(buildJsonObject {
                put("encoding", Encoding.base64.getEncoding())
                put("commitment", commitment.value)
            })
        }
        val rpcRequest =
            RpcRequest(method = SocketMethod.accountSubscribe.string, params)
        return writeToSocket(rpcRequest)
    }

    suspend fun accountUnSubscribe(socketId: Int): Result<String> {
        val params = buildJsonArray {
            add(socketId)
        }
        val rpcRequest =
            RpcRequest(method = SocketMethod.accountUnsubscribe.string, params)
        return writeToSocket(rpcRequest)
    }

    suspend fun logsSubscribe(mentions: List<String>, commitment: Commitment = Commitment.FINALIZED): Result<String> {
        val params = buildJsonArray {
            add(buildJsonObject {
                put("mentions", buildJsonArray {
                    mentions.forEach {
                        add(it)
                    }
                })
            })
            add(buildJsonObject {
                put("encoding", Encoding.base64.getEncoding())
                put("commitment", commitment.value)
            })
        }
        val rpcRequest = RpcRequest(method = SocketMethod.logsSubscribe.string, params)
        return writeToSocket(rpcRequest)
    }

    suspend fun logsSubscribeAll(commitment: Commitment = Commitment.RECENT): Result<String> {
        val params = buildJsonArray {
            add("all")
            add(buildJsonObject {
                put("encoding", Encoding.base64.getEncoding())
                put("commitment", commitment.value)
            })
        }
        val rpcRequest = RpcRequest(method = SocketMethod.logsSubscribe.string, params)
        return writeToSocket(rpcRequest)
    }

    suspend fun logsUnsubscribe(socketId: Int): Result<String> {
        val params = buildJsonArray {
            add(socketId)
        }
        val rpcRequest =
            RpcRequest(method = SocketMethod.logsUnsubscribe.string, params)
        return writeToSocket(rpcRequest)
    }

    suspend fun programSubscribe(publicKey: String, commitment: Commitment = Commitment.RECENT): Result<String> {
        val params = buildJsonArray {
            add(publicKey)
            add(buildJsonObject {
                put("encoding", Encoding.base64.getEncoding())
                put("commitment", commitment.value)
            })
        }
        val rpcRequest =
            RpcRequest(method = SocketMethod.programSubscribe.string, params)
        return writeToSocket(rpcRequest)
    }

    suspend fun programUnsubscribe(socketId: Int): Result<String> {
        val params = buildJsonArray {
            add(socketId)
        }
        val rpcRequest =
            RpcRequest(method = SocketMethod.programUnsubscribe.string, params)
        return writeToSocket(rpcRequest)
    }

    suspend fun signatureSubscribe(signature: String, commitment: Commitment = Commitment.RECENT): Result<String> {
        val params = buildJsonArray {
            add(signature)
            add(buildJsonObject {
                put("encoding", Encoding.base64.getEncoding())
                put("commitment", commitment.value)
            })
        }
        val rpcRequest =
            RpcRequest(method = SocketMethod.signatureSubscribe.string, params)
        return writeToSocket(rpcRequest)
    }

    suspend fun signatureUnsubscribe(socketId: Int): Result<String> {
        val params = buildJsonArray {
            add(socketId)
        }
        val rpcRequest =
            RpcRequest(method = SocketMethod.signatureUnsubscribe.string, params)
        return writeToSocket(rpcRequest)
    }

    private suspend fun writeToSocket(request: RpcRequest): Result<String> {
        val json = json.encodeToString(RpcRequest.serializer(), request)
        val written = try {
            socket?.send(json)
            true
        } catch (e: CancellationException) {
            LOGGER.error("Cancelled", e)
            false
        }
        if (!written) return Result.failure(SolanaSocketError.CouldNotWrite)
        return Result.success(request.id)
    }
}

@Serializable
data class RPCResponseMethond(val method: String? = null, val result: JsonPrimitive? = null)