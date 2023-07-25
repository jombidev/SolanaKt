@file:Suppress("unused")

package com.solana.actions

import com.solana.api.getDefaultInfo
import com.solana.api.sendTransaction
import com.solana.core.Account
import com.solana.core.PublicKey
import com.solana.core.Transaction
import com.solana.core.TransactionInstruction
import com.solana.programs.AssociatedTokenProgram
import com.solana.programs.TokenProgram
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

suspend fun Action.sendSPLTokens(
    from: Account,
    to: PublicKey,
    amount: Long,
    feePayer: Account? = null,
    allowUnfundedRecipient: Boolean = false,
    mintAddress: PublicKey = PublicKey(USDTSOL().address),
): Result<String> {
    val acc = feePayer ?: from
    val associatedTokenFrom = PublicKey.associatedTokenAddress(from.publicKey, mintAddress)
    val accountFrom = api.getDefaultInfo(to).getOrNull()
    if (accountFrom == null && !allowUnfundedRecipient)
        return Result.failure(IllegalArgumentException("from account don't have spl token for '$mintAddress'."))

    val associatedTokenTo = PublicKey.associatedTokenAddress(to, mintAddress)
    val transaction = Transaction()
    val transactionInstructions = arrayListOf<TransactionInstruction>()

    if (api.getDefaultInfo(to).getOrNull() == null) {
        transactionInstructions.add(
            AssociatedTokenProgram.createAssociatedTokenAccountInstruction(
                mint = mintAddress,
                associatedAccount = associatedTokenTo.address,
                owner = to,
                payer = acc.publicKey,
            )
        )
    }

    transactionInstructions.add(
        TokenProgram.transfer(
            associatedTokenFrom.address,
            associatedTokenTo.address,
            amount,
            from.publicKey,
        )
    )

    transaction.add(*transactionInstructions.toTypedArray())
    return api.sendTransaction(transaction, if (feePayer == null) listOf(acc) else listOf(acc, from))
}

fun Action.sendSPLTokens(
    from: Account,
    to: PublicKey,
    amount: Long,
    feePayer: Account? = null,
    allowUnfundedRecipient: Boolean = false,
    mintAddress: PublicKey = PublicKey(USDTSOL().address),
    onComplete: ((Result<String>) -> Unit)
){
    CoroutineScope(api.dispatcher).launch {
        onComplete(sendSPLTokens(from, to, amount, feePayer, allowUnfundedRecipient, mintAddress))
    }
}