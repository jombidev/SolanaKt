package com.solana.vendor

import com.solana.models.Token
import com.solana.models.TokenTag
import com.solana.models.TokensList
import com.solana.resources.devnet
import com.solana.resources.mainnet_beta
import com.solana.resources.testnet
import kotlinx.serialization.json.Json


class TokensListParser {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    private val tokens by lazy {
        mapOf(
            "devnet" to devnet,
            "mainnet-beta" to mainnet_beta,
            "testnet" to testnet
        )
    }

    fun parse(network: String): Result<List<Token>, ResultError> {
        val jsonContent = tokens[network]

        return try {
            val tokenList = json.decodeFromString(TokensList.serializer(), jsonContent!!)
            tokenList.tokens = tokenList.tokens.map {
                it.tokenTags = it._tags.map { index ->
                    tokenList.tags[index] ?: TokenTag(it.name, it.name)
                }
                it
            }
            val listTokens = tokenList.tokens.fold(listOf()) { list: List<Token>, token: Token ->
                list.toMutableList().apply { if (!contains(token)) add(token) }
            }
            Result.success(listTokens)
        } catch (e: Exception) {
            Result.failure(ResultError(e))
        }
    }
}