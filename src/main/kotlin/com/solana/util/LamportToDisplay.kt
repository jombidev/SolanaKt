package com.solana.util

object LamportToDisplay {
    const val SOLANA_DIVISION_OFFSET = 1_000_000_000
    const val SPL_DIVISION_OFFSET = 1_000_000

    fun toSOL(amount: Long): Double {
        return amount.toDouble() / SOLANA_DIVISION_OFFSET
    }

    fun toUSDT(lamport: Long): Double {
        return lamport.toDouble() / SPL_DIVISION_OFFSET
    }
}