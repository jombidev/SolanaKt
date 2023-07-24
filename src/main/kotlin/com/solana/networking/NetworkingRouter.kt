package com.solana.networking

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface NetworkingRouter : JsonRpcDriver {
    val endpoint: RPCEndpoint
}

class HttpNetworkingRouter(
    override val endpoint: RPCEndpoint,
) : NetworkingRouter {
    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(HttpNetworkingRouter::class.java)
    }

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }

    override suspend fun <R> makeRequest(
        request: RpcRequest,
        resultSerializer: KSerializer<R>
    ): RpcResponse<R> {
        val req = client.prepareRequest(endpoint.url) {
            method = HttpMethod.Post
            contentType(ContentType.Application.Json.withParameter("charset", "utf-8"))
            setBody(json.encodeToString(RpcRequest.serializer(), request))
        }
        val res = req.execute()
        val body = res.bodyAsText()

        LOGGER.debug("URL: {}", endpoint.url)
        LOGGER.debug("Response code: {}", res.status)
        LOGGER.debug("Body: {}", body)

        return json.decodeFromString(RpcResponse.serializer(resultSerializer), body)
//        return try {
//            val data = json.decodeFromString(RpcResponse.serializer(resultSerializer), body)
//            data
//        } catch (e: SerializationException) {
//            throw e
//        }
    }
}

