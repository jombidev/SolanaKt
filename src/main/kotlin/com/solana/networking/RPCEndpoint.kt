package com.solana.networking

import java.net.URL

const val MAINNETBETA = "mainnet-beta"
const val DEVNET = "devnet"
const val TESTNET = "testnet"

sealed class Network(val name: String) {
    data object mainnetBeta: Network(MAINNETBETA)
    data object devnet: Network(DEVNET)
    data object testnet: Network(TESTNET)
    var cluster: String = this.name
}


sealed class RPCEndpoint(open val url: URL, open val urlWebSocket: String, open val network: Network) {
    data object MainNetworkBeta: RPCEndpoint(URL("https://api.mainnet-beta.solana.com"), "wss://api.mainnet-beta.solana.com", Network.mainnetBeta)
    data object DevNetwork: RPCEndpoint(URL("https://api.devnet.solana.com"), "wss://api.devnet.solana.com", Network.devnet)
    data object TestNetwork: RPCEndpoint(URL("https://testnet.solana.com"), "wss://testnet.solana.com",Network.testnet)
    data class Custom(override val url: URL, override val urlWebSocket: String, override val network: Network) : RPCEndpoint(url, urlWebSocket, network)
}