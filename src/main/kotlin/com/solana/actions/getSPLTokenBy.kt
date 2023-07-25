package com.solana.actions

import com.solana.exception.SolanaException
import com.solana.models.Token

fun Action.getSPLTokenBy(filter: (Token) -> Boolean) = supportedTokens.find(filter)

fun Action.USDTSOL() = getSPLTokenBy { it.symbol == "USDT" } ?: throw SolanaException("this spl token is only for mainnet")
fun Action.USDCSOL() = getSPLTokenBy { it.address == "EPjFWdd5AufqSSqeM2qN1xzybapC8G4wEGGkZwyTDt1v" }
    ?: throw SolanaException("this spl token is only for mainnet")// this is weird