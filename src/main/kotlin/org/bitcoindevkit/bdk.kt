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
     * Return the address for the current descriptor index if it has not been used in a received transaction. Otherwise return a new address as with `AddressIndex.NEW`. Use with caution, if the wallet has not yet detected an address has been used it could return an already used address. This function is primarily meant for situations where the caller is untrusted; for example when deriving donation addresses on-demand for a public web page.
     */
    LAST_UNUSED,
}

/**
 * Type that can contain any of the database configurations defined by the library.
 */
sealed class DatabaseConfig {
    /** Configuration for an in-memory database */
    object Memory : DatabaseConfig()

    /** Configuration for a Sled database */
    data class Sled(val config: SledDbConfiguration) : DatabaseConfig()

    /** Configuration for a SQLite database */
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
 * A wallet transaction.
 */
data class TransactionDetails (
    /** Fee value (sats) if available. The availability of the fee depends on the backend. It’s never None with an Electrum server backend, but it could be None with a Bitcoin RPC node without txindex that receive funds while offline. */
    var fee: ULong?,

    /** Received value (sats) Sum of owned outputs of this transaction. */
    var received: ULong,

    /** Sent value (sats) Sum of owned inputs of this transaction. */
    var sent: ULong,

    /** Transaction id. */
    var txid: String
)

/**
 * A blockchain backend.
 */
class Blockchain(
    config: BlockchainConfig
) {
    /** Broadcast a transaction */
    fun broadcast(signedPsbt: PartiallySignedBitcoinTransaction): String {}
}

/**
 * A partially signed bitcoin transaction.
 */
class PartiallySignedBitcoinTransaction(psbtBase64: String) {
    /** Return the PSBT in string format, using a base64 encoding. */
    fun serialize(): String {}

    /** Get the txid of the PSBT. */
    fun txid(): String {}
}

/**
 * A reference to a transaction output.
 */
data class OutPoint (
    /** The referenced transaction’s txid. */
    var txid: String,

    /** The index of the referenced output in its transaction’s vout. */
    var vout: UInt
)

/**
 * A transaction output, which defines new coins to be created from old ones.
 */
data class TxOut (
    /** The value of the output, in satoshis. */
    var value: ULong,

    /** The address of the output. */
    var address: String
)

/**
 * An unspent output owned by a [Wallet].
 */
data class LocalUtxo (
    /** Reference to a transaction output. */
    var outpoint: OutPoint,

    /** Transaction output. */
    var txout: TxOut,

    /** Type of keychain. */
    var keychain: KeychainKind,

    /** Whether this UTXO is spent or not. */
    var isSpent: Boolean
)

/**
 * Types of keychains.
 */
enum class KeychainKind {
    /** External */
    EXTERNAL,

    /** Internal, usually used for change outputs. */
    INTERNAL,
}

/**
 * A transaction, either of type [Confirmed] or [Unconfirmed]
 */
sealed class Transaction {
    /** A transaction that has yet to be included in a block. */
    data class Unconfirmed(
        /** The details of wallet transaction. */
        val details: TransactionDetails,
    ) : Transaction()

    /** A transaction that has been mined and is part of a block. */
    data class Confirmed(
        /** The details of wallet transaction. */
        val details: TransactionDetails,

        /** Timestamp and block height of the block in which this transaction was mined. */
        val confirmation: BlockTime,
    ) : Transaction()
}

/**
 * Block height and timestamp of a block.
 */
data class BlockTime (
    /** confirmation block height */
    var height: UInt,

    /** confirmation block timestamp */
    var timestamp: ULong,
)

/**
 * A Bitcoin wallet.
 * The Wallet acts as a way of coherently interfacing with output descriptors and related transactions. Its main components are:
 * 1. Output descriptors from which it can derive addresses.
 * 2. A Database where it tracks transactions and utxos related to the descriptors.
 * 3. Signers that can contribute signatures to addresses instantiated from the descriptors.
 */
class Wallet(
    descriptor: String,
    changeDescriptor: String,
    network: Network,
    databaseConfig: DatabaseConfig,
) {
    /** Return a derived address using the external descriptor, see [AddressIndex] for available address index selection strategies. If none of the keys in the descriptor are derivable (i.e. the descriptor does not end with a * character) then the same address will always be returned for any [AddressIndex]. */
    fun getAddress(addressIndex: AddressIndex): AddressInfo {}

    /** Return the balance, meaning the sum of this wallet’s unspent outputs’ values. Note that this method only operates on the internal database, which first needs to be [Wallet.sync] manually. */
    fun getBalance(): ULong {}

    /** Sign a transaction with all the wallet’s signers. */
    fun sign(psbt: PartiallySignedBitcoinTransaction): Boolean {}

    /** Return the list of transactions made and received by the wallet. Note that this method only operate on the internal database, which first needs to be [Wallet.sync] manually. */
    fun getTransactions(): List<Transaction> {}

    /** Get the Bitcoin network the wallet is using. */
    fun getNetwork(): Network {}

    /** Sync the internal database with the blockchain. */
    fun sync(blockchain: Blockchain, progress: Progress?) {}

    /** Return the list of unspent outputs of this wallet. Note that this method only operates on the internal database, which first needs to be [Wallet.sync] manually. */
    fun listUnspent(): List<LocalUtxo> {}
}

/**
 * Class that logs at level INFO every update received (if any).
 */
class Progress {
    /** Send a new progress update. The progress value should be in the range 0.0 - 100.0, and the message value is an optional text message that can be displayed to the user. */
    fun update(progress: Float, message: String?) {}
}

/**
 * An object containing data on a private extended key (xprv).
 */
data class ExtendedKeyInfo (
    /** The mnemonic. */
    var mnemonic: String,

    /** The xprv. */
    var xprv: String,

    /** The fingerprint (useful in descriptors). */
    var fingerprint: String
)

/**
 * A transaction builder.
 *
 * After creating the TxBuilder, you set options on it until finally calling finish to consume the builder and generate the transaction.
 *
 * Each method on the TxBuilder returns an instance of a new TxBuilder with the option set/added.
 */
class TxBuilder() {
    /** Add data as an output using OP_RETURN. */
    fun addData(data: List<UByte>): TxBuilder {}

    /** Add a recipient to the internal list. */
    fun addRecipient(address: String, amount: ULong): TxBuilder {}

    /** Add a utxo to the internal list of unspendable utxos. It’s important to note that the "must-be-spent" utxos added with [TxBuilder.addUtxo] have priority over this. See the Rust docs of the two linked methods for more details. */
    fun addUnspendable(unspendable: OutPoint): TxBuilder {}

    /** Add an outpoint to the internal list of UTXOs that must be spent. These have priority over the "unspendable" utxos, meaning that if a utxo is present both in the "utxos" and the "unspendable" list, it will be spent. */
    fun addUtxo(outpoint: OutPoint): TxBuilder {}

    /** Add the list of outpoints to the internal list of UTXOs that must be spent. If an error occurs while adding any of the UTXOs then none of them are added and the error is returned. These have priority over the "unspendable" utxos, meaning that if a utxo is present both in the "utxos" and the "unspendable" list, it will be spent. */
    fun addUtxos(outpoints: List<OutPoint>): TxBuilder {}

    /** Do not spend change outputs. This effectively adds all the change outputs to the "unspendable" list. See [TxBuilder.unspendable]. */
    fun doNotSpendChange(): TxBuilder {}

    /** Only spend utxos added by [add_utxo]. The wallet will not add additional utxos to the transaction even if they are needed to make the transaction valid. */
    fun manuallySelectedOnly(): TxBuilder {}

    /** Only spend change outputs. This effectively adds all the non-change outputs to the "unspendable" list. See [TxBuilder.unspendable]. */
    fun onlySpendChange(): TxBuilder {}

    /** Replace the internal list of unspendable utxos with a new list. It’s important to note that the "must-be-spent" utxos added with [TxBuilder.addUtxo] have priority over these. See the Rust docs of the two linked methods for more details. */
    fun unspendable(unspendable: List<OutPoint>): TxBuilder {}

    /** Set a custom fee rate. */
    fun feeRate(satPerVbyte: Float): TxBuilder {}

    /** Set an absolute fee. */
    fun feeAbsolute(feeAmount: ULong): TxBuilder {}

    /** Spend all the available inputs. This respects filters like [TxBuilder.unspendable] and the change policy. */
    fun drainWallet(): TxBuilder {}

    /** Sets the address to drain excess coins to. Usually, when there are excess coins they are sent to a change address generated by the wallet. This option replaces the usual change address with an arbitrary ScriptPubKey of your choosing. Just as with a change output, if the drain output is not needed (the excess coins are too small) it will not be included in the resulting transaction. The only difference is that it is valid to use [drainTo] without setting any ordinary recipients with [addRecipient] (but it is perfectly fine to add recipients as well). If you choose not to set any recipients, you should either provide the utxos that the transaction should spend via [addUtxos], or set [drainWallet] to spend all of them. When bumping the fees of a transaction made with this option, you probably want to use [BumpFeeTxBuilder.allowShrinking] to allow this output to be reduced to pay for the extra fees. */
    fun drainTo(address: String): TxBuilder {}

    /** Enable signaling RBF. This will use the default `nsequence` value of `0xFFFFFFFD`. */
    fun enableRbf(): TxBuilder {}

    /** Enable signaling RBF with a specific nSequence value. This can cause conflicts if the wallet's descriptors contain an "older" (OP_CSV) operator and the given `nsequence` is lower than the CSV value. If the `nsequence` is higher than `0xFFFFFFFD` an error will be thrown, since it would not be a valid nSequence to signal RBF. */
    fun enableRbfWithSequence(nsequence: UInt): TxBuilder {}

    /** Finish building the transaction. Returns the BIP174 PSBT. */
    fun finish(wallet: Wallet): PartiallySignedBitcoinTransaction {}
}

/**
 * The BumpFeeTxBuilder is used to bump the fee on a transaction that has been broadcast and has its RBF flag set to true.
 */
class BumpFeeTxBuilder() {
    /** Explicitly tells the wallet that it is allowed to reduce the amount of the output matching this script_pubkey in order to bump the transaction fee. Without specifying this the wallet will attempt to find a change output to shrink instead. Note that the output may shrink to below the dust limit and therefore be removed. If it is preserved then it is currently not guaranteed to be in the same position as it was originally. Returns an error if script_pubkey can’t be found among the recipients of the transaction we are bumping. */
    fun allowShrinking(address: String): BumpFeeTxBuilder {}

    /** Enable signaling RBF. This will use the default `nsequence` value of `0xFFFFFFFD`. */
    fun enableRbf(): BumpFeeTxBuilder {}

    /** Enable signaling RBF with a specific nSequence value. This can cause conflicts if the wallet's descriptors contain an "older" (OP_CSV) operator and the given `nsequence` is lower than the CSV value. If the `nsequence` is higher than `0xFFFFFFFD` an error will be thrown, since it would not be a valid nSequence to signal RBF. */
    fun enableRbfWithSequence(nsequence: UInt): BumpFeeTxBuilder {}

    /** Finish building the transaction. Returns the BIP174 PSBT. */
    fun finish(wallet: Wallet): PartiallySignedBitcoinTransaction {}
}

/**
 * Create an [ExtendedKeyInfo] object with the given network, word count, and optional password.
 */
fun generateExtendedKey(
    network: Network,
    wordCount: WordCount,
    password: String?,
): ExtendedKeyInfo {}

/**
 * Create an [ExtendedKeyInfo] object from a given BIP39 mnemonic phrase with an optional password.
 */
fun restoreExtendedKey(
    network: Network,
    mnemonic: String,
    password: String?,
): ExtendedKeyInfo {}

/**
 * An enum describing entropy length (aka word count) in the mnemonic.
 */
enum class WordCount {
    /** 12 words mnemonic (128 bits entropy) */
    WORDS12,

    /** 15 words mnemonic (160 bits entropy) */
    WORDS15,

    /** 18 words mnemonic (192 bits entropy) */
    WORDS18,

    /** 21 words mnemonic (224 bits entropy) */
    WORDS21,

    /** 24 words mnemonic (256 bits entropy) */
    WORDS24,
}
