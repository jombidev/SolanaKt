package com.solana.actions

import com.solana.api.getDefaultInfo
import com.solana.api.sendTransaction
import com.solana.core.*
import com.solana.programs.AssociatedTokenProgram
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

suspend fun Action.tryCreateAssociatedAccount(
    feePayer: HotAccount,
    targetPublicKey: PublicKey,
    mintAddress: PublicKey = PublicKey(USDTSOL().address),
): Result<String?> {
    val associatedTokenTo = PublicKey.associatedTokenAddress(targetPublicKey, mintAddress)
    val transaction = Transaction()
    val transactionInstructions = arrayListOf<TransactionInstruction>()
    if (api.getDefaultInfo(associatedTokenTo.address).getOrNull() != null)
        return Result.success(null)

    transactionInstructions.add(
        AssociatedTokenProgram.createAssociatedTokenAccountInstruction(
            mint = mintAddress,
            associatedAccount = associatedTokenTo.address,
            owner = targetPublicKey,
            payer = feePayer.publicKey,
        )
    )

    transaction.add(*transactionInstructions.toTypedArray())

    return api.sendTransaction(transaction, listOf(feePayer))
}

fun Action.tryCreateAssociatedAccount(
    feePayer: HotAccount,
    targetPublicKey: PublicKey,
    mintAddress: PublicKey = PublicKey(USDTSOL().address),
    onComplete: ((Result<String?>) -> Unit)
){
    CoroutineScope(api.dispatcher).launch {
        onComplete(tryCreateAssociatedAccount(feePayer, targetPublicKey, mintAddress))
    }
}