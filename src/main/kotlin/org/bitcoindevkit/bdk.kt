package org.bitcoindevkit

/**
 * The cryptocurrency to act on.
 */
enum class Network {
    /** Bitcoin's mainnet */
    BITCOIN,

    /** Bitcoin’s testnet */
    TESTNET,

    /** Bitcoin’s signet */
    SIGNET,

    /** Bitcoin’s regtest */
    REGTEST,
}

/**
 * A derived address and the index it was found at.
 */
data class AddressInfo (
    /** Child index of this address */
    var index: UInt,

    /** Address */
    var address: String
)

/**
 * The address index selection strategy to use to derive an address from the wallet’s external descriptor.
 *
 * If you’re unsure which one to use, use `AddressIndex.NEW`.
 */
enum class AddressIndex {
    /** Return a new address after incrementing the current descriptor index. */
    NEW,

    /**
     * Return the address for the current descriptor index if it has not been used in a received transaction. Otherwise return a new address as with `AddressIndex.NEW`.
     *
     * Use with caution, if the wallet has not yet detected an address has been used it could return an already used address. This function is primarily meant for situations where the caller is untrusted; for example when deriving donation addresses on-demand for a public web page.
     */
    LAST_UNUSED,
}

/**
 * Type that can contain any of the database configurations defined by the library.
 */
sealed class DatabaseConfig {
    object Memory : DatabaseConfig()

    data class Sled(val config: SledDbConfiguration) : DatabaseConfig()

    data class Sqlite(val config: SqliteDbConfiguration) : DatabaseConfig()
}

/**
 * Configuration type for a SQLite database.
 */
data class SqliteDbConfiguration(
    /** Main directory of the db */
    var path: String,
)

/**
 * Configuration type for a SledDB database.
 */
data class SledDbConfiguration(
    /** Main directory of the db */
    var path: String,

    /** Name of the database tree, a separated namespace for the data */
    var treeName: String,
)

/**
 * Configuration for an Electrum blockchain.
 */
data class ElectrumConfig (
    /** URL of the Electrum server (such as ElectrumX, Esplora, BWT) may start with `ssl://` or `tcp://` and include a port, e.g. `ssl://electrum.blockstream.info:60002`. */
    var url: String,

    /** URL of the socks5 proxy server or a Tor service. */
    var socks5: String?,

    /** Request retry count. */
    var retry: UByte,

    /** Request timeout (seconds). */
    var timeout: UByte?,

    /** Stop searching addresses for transactions after finding an unused gap of this length. */
    var stopGap: ULong
)

/**
 * Configuration for an Esplora blockchain.
 */
data class EsploraConfig (
    /** Base URL of the esplora service, e.g. `https://blockstream.info/api/`. */
    var baseUrl: String,

    /** Optional URL of the proxy to use to make requests to the Esplora server. */
    var proxy: String?,

    /** Number of parallel requests sent to the esplora service (default: 4). */
    var concurrency: UByte?,

    /** Stop searching addresses for transactions after finding an unused gap of this length. */
    var stopGap: ULong,

    /** Socket timeout. */
    var timeout: ULong?
)

/**
 * Type that can contain any of the blockchain configurations defined by the library.
 */
sealed class BlockchainConfig {
    /** Electrum client */
    data class Electrum(val config: ElectrumConfig) : BlockchainConfig()

    /** Esplora client */
    data class Esplora(val config: EsploraConfig) : BlockchainConfig()
}

/**
 * TBD
 */
data class TransactionDetails (
    var fee: ULong?,
    var received: ULong,
    var sent: ULong,
    var txid: String
)

/**
 * TBD
 */
class Blockchain(
    config: BlockchainConfig
) {
    fun broadcast(signedPsbt: PartiallySignedBitcoinTransaction): String {
        return "String"
    }
}

/**
 * TBD
 */
class PartiallySignedBitcoinTransaction(psbtBase64: String) {
    fun serialize(): String {}
    fun txid(): String {}
}

/**
 * TBD
 */
data class OutPoint (
    var txid: String,
    var vout: UInt
)

/**
 * TBD
 */
data class TxOut (
    var value: ULong,
    var address: String
)

/**
 * TBD
 */
data class LocalUtxo (
    var outpoint: OutPoint,
    var txout: TxOut,
    var keychain: KeychainKind,
    var isSpent: Boolean
)

/**
 * TBD
 */
enum class KeychainKind {
    EXTERNAL,
    INTERNAL,
}

/**
 * TBD
 */
sealed class Transaction {
    data class Unconfirmed(val details: TransactionDetails) : Transaction()
    data class Confirmed(val details: TransactionDetails, val confirmation: BlockTime) : Transaction()
}

/**
 * TBD
 */
data class BlockTime (
    var height: UInt,
    var timestamp: ULong,
)

/**
 * TBD
 */
class Wallet(
    descriptor: String,
    changeDescriptor: String,
    network: Network,
    databaseConfig: DatabaseConfig,
) {
    /**  */
    fun getAddress(addressIndex: AddressIndex): AddressInfo {}

    /**  */
    fun getBalance(): ULong {}

    /**  */
    fun sign(psbt: PartiallySignedBitcoinTransaction): Boolean {}

    /**  */
    fun getTransactions(): List<Transaction> {}

    /**  */
    fun getNetwork(): Network {}

    /**  */
    fun sync(blockchain: Blockchain, progress: Progress?) {}

    /**  */
    fun listUnspent(): List<LocalUtxo> {}
}

/**
 * TBD
 */
class Progress {
    fun update(progress: Float, message: String?) {}
}

/**
 * TBD
 */
data class ExtendedKeyInfo (
    var mnemonic: String,
    var xprv: String,
    var fingerprint: String
)

/**
 * TBD
 */
class TxBuilder() {
    /**  */
    fun addRecipient(address: String, amount: ULong): TxBuilder {}

    /**  */
    fun addUnspendable(unspendable: OutPoint): TxBuilder {}

    /**  */
    fun addUtxo(outpoint: OutPoint): TxBuilder {}

    /**  */
    fun addUtxos(outpoints: List<OutPoint>): TxBuilder {}

    /**  */
    fun doNotSpendChange(): TxBuilder {}

    /**  */
    fun manuallySelectedOnly(): TxBuilder {}

    /**  */
    fun onlySpendChange(): TxBuilder {}

    /**  */
    fun unspendable(unspendable: List<OutPoint>): TxBuilder {}

    /**  */
    fun feeRate(satPerVbyte: Float): TxBuilder {}

    /**  */
    fun feeAbsolute(feeAmount: ULong): TxBuilder {}

    /**  */
    fun drainWallet(): TxBuilder {}

    /**  */
    fun drainTo(address: String): TxBuilder {}

    /**  */
    fun enableRbf(): TxBuilder {}

    /**  */
    fun enableRbfWithSequence(nsequence: UInt): TxBuilder {}

    /**  */
    fun addData(data: List<UByte>): TxBuilder {}

    /**  */
    fun finish(wallet: Wallet): PartiallySignedBitcoinTransaction {}
}

/**
 * TBD
 */
class BumpFeeTxBuilder() {
    /**  */
    fun allowShrinking(address: String): BumpFeeTxBuilder {}

    /**  */
    fun enableRbf(): BumpFeeTxBuilder {}

    /**  */
    fun enableRbfWithSequence(nsequence: UInt): BumpFeeTxBuilder {}

    /**  */
    fun finish(wallet: Wallet): PartiallySignedBitcoinTransaction {}
}
