@file:Suppress("unused")

package com.solana.actions

import com.solana.core.Account
import com.solana.core.PublicKey
import com.solana.core.Transaction
import com.solana.programs.AssociatedTokenProgram
import com.solana.programs.TokenProgram
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

suspend fun Action.sendSPLTokens(
    mintAddress: PublicKey,
    fromPublicKey: PublicKey,
    destinationAddress: PublicKey,
    amount: Long,
    account: Account,
    allowUnfundedRecipient: Boolean = false,
): Result<String>{
    val spl = this.findSPLTokenDestinationAddress(
        mintAddress,
        destinationAddress,
        allowUnfundedRecipient
    ).getOrThrows()

    val toPublicKey = spl.first
    val isUnregisteredAsocciatedToken = spl.second

    val transaction = Transaction()

    // create associated token address
    if(isUnregisteredAsocciatedToken) {
        val createATokenInstruction = AssociatedTokenProgram.createAssociatedTokenAccountInstruction(
            mint = mintAddress,
            associatedAccount = toPublicKey,
            owner = destinationAddress,
            payer = account.publicKey
        )
        transaction.add(createATokenInstruction)
    }

    // send instruction
    val sendInstruction = TokenProgram.transfer(fromPublicKey, toPublicKey, amount, account.publicKey)
    transaction.add(sendInstruction)
    val feeVerify = arrayListOf(account)
    return serializeAndSendWithFee(transaction, feeVerify)
}

fun Action.sendSPLTokens(
    mintAddress: PublicKey,
    fromPublicKey: PublicKey,
    destinationAddress: PublicKey,
    amount: Long,
    account: Account,
    allowUnfundedRecipient: Boolean = false,
    onComplete: ((Result<String>) -> Unit)
){
    CoroutineScope(api.dispatcher).launch {
        onComplete(sendSPLTokens(mintAddress, fromPublicKey, destinationAddress, amount, account, allowUnfundedRecipient))
    }
}