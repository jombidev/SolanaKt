package com.solana.exception

open class SolanaException : RuntimeException {
    constructor(msg: String) : super(msg)
    constructor(e: Throwable) : super(e)
    constructor() : this("Something is happened, but got no description.")
}
