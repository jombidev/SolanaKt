package com.solana.api

import com.solana.networking.NetworkingRouter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class Api(
    val router: NetworkingRouter,
    val dispatcher: CoroutineDispatcher = Dispatchers.IO
)