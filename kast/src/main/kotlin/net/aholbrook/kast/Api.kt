package net.aholbrook.kast

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketException
import java.time.Duration

private val logger = KotlinLogging.logger {}

class Sender<T> internal constructor(
    val hosts: Array<String>,
    val port: Int,
    val encoder: NotificationEncoder<T>,
    refreshInterval: Duration = Duration.ofMinutes(1),
    socketFactory: () -> DatagramSocket,
) {
    private val dnsCache = DnsCache(refreshInterval)

    private val mutex = Mutex()
    private val socket = socketFactory().also { socket ->
        Runtime.getRuntime().addShutdownHook(
            Thread {
                runBlocking {
                    logger.debug { "Shutting down socket for port $port..." }
                    withTimeout(60_000L) {
                        mutex.withLock { socket.close() }
                    }
                }
            },
        )
    }

    constructor(
        hosts: Array<String>,
        port: Int,
        encoder: NotificationEncoder<T>,
        refreshInterval: Duration = Duration.ofMinutes(1),
    ) : this(hosts, port, encoder, refreshInterval, { DatagramSocket() })

    fun start() {
        dnsCache.start()
        hosts.forEach { resolve(it) }
    }

    fun stop() {
        dnsCache.stop()
    }

    suspend fun notifyAll(notification: Notification<T>) {
        val encoded = encode(notification, encoder)

        val targets = HashSet<InetAddress>(hosts.size * 2)
        for (host in hosts) {
            targets.addAll(resolve(host))
        }
        val packet = DatagramPacket(encoded, encoded.size)
        packet.port = port

        mutex.withLock {
            targets.forEach {
                logger.debug { "Sending packet $packet to $it" }
                packet.address = it
                socket.send(packet)
            }
        }
    }

    suspend fun notify(notification: Notification<T>) {
        notify(hosts.random(), notification)
    }

    suspend fun notify(host: String, notification: Notification<T>) {
        val encoded = encode(notification, encoder)
        val target = resolve(host).toSet().random()
        val packet = DatagramPacket(encoded, encoded.size, target, port)

        logger.debug { "Sending packet $packet to $target" }
        mutex.withLock { socket.send(packet) }
    }

    private fun resolve(host: String): List<InetAddress> = dnsCache.get(host)
}

class Receiver<_Data>(val port: Int, val decoder: NotificationDecoder<_Data>) {
    private val broadcastManager = BroadcastManager<String, _Data>()
    private var socket: DatagramSocket? = null
    private var job: Job? = null

    fun start(
        dispatcher: CoroutineDispatcher = Dispatchers.Default,
        concurrency: Int = Runtime.getRuntime().availableProcessors(),
    ) {
        if (socket != null) {
            return
        }
        if (job != null) {
            return
        }

        val scope = CoroutineScope(SupervisorJob() + dispatcher.limitedParallelism(concurrency))

        socket = DatagramSocket(port).also { socket ->
            job = CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                val packet = DatagramPacket(ByteArray(FRAME_MAX_BYTES), FRAME_MAX_BYTES)

                while (isActive && !socket.isClosed) {
                    try {
                        socket.receive(packet)
                        logger.debug { "Received packet $packet" }

                        val decoded = decode(packet.data.copyOf(), decoder)
                        scope.launch {
                            runCatching {
                                logger.debug { "Received notification ${decoded.id}: ${decoded.payload}" }
                                broadcastManager.notify(decoded.id, decoded.payload)
                            }
                        }
                    } catch (_: SocketException) {
                        // ignored
                    } catch (ex: Throwable) {
                        logger.error(ex) { "Error while receiving notification" }
                    }
                }
            }.also {
                it.invokeOnCompletion {
                    this@Receiver.socket = null
                    job = null
                }
            }
        }
    }

    suspend fun stop() {
        socket?.close()
        job?.join()
    }

    suspend fun whenNotified(
        id: String,
        duration: Duration = Duration.ofSeconds(30),
        block: suspend (payload: _Data) -> Unit,
    ) {
        broadcastManager.register(id, duration.toMillis())?.let {
            val payload = withTimeout(duration.toMillis()) { it.await() }
            block(payload)
        }
    }
}
