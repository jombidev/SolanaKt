package com.solana.exception

class ProgramException : SolanaException {
    constructor(e: Throwable) : super(e)
    constructor(s: String) : super(s)
}