package net.aholbrook.kast

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import mu.KotlinLogging
import org.xbill.DNS.AAAARecord
import org.xbill.DNS.ARecord
import org.xbill.DNS.Lookup
import org.xbill.DNS.Type
import java.net.InetAddress
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

private val ipv4Regex = Regex("^\\d+\\.\\d+\\.\\d+\\.\\d+$")
private val ipv6Regex = Regex("^[0-9A-Fa-f:]+:[0-9A-Fa-f:]+$")

internal class DnsCache(private val refreshInterval: Duration = Duration.ofSeconds(15)) {
    private val cache = AtomicReference<Map<String, List<InetAddress>>>(emptyMap())

    private val refreshJob: Job by lazy {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    delay(refreshInterval)
                    refresh()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.warn(e) { "Ignoring uncaught exception in refresh loop" }
                }
            }
        }
    }

    fun start() {
        refreshJob.start()
        Runtime.getRuntime().addShutdownHook(thread(start = false) { stop() })
    }

    fun stop() {
        runBlocking { refreshJob.cancelAndJoin() }
    }

    fun get(name: String): List<InetAddress> {
        val current = cache.get()
        current[name]?.let {
            logger.debug { "cache hit $name -> $it" }
            return it
        }

        val resolved = resolve(name)
        logger.debug { "cache miss $name -> $resolved" }
        val updated = cache.updateAndGet { current ->
            if (current[name] != null) {
                return@updateAndGet current
            }

            current + (name to resolved)
        }

        return updated[name] ?: resolved
    }

    private fun isIpAddress(host: String) = ipv4Regex.matches(host) || ipv6Regex.matches(host)

    private fun resolve(name: String): List<InetAddress> {
        if (isIpAddress(name)) {
            return InetAddress.getAllByName(name).toList()
        }

        val aRecords = Lookup(name, Type.A).let { lookup ->
            lookup.setCache(null)
            (lookup.run() ?: arrayOf()).mapNotNull { record ->
                (record as? ARecord)?.address?.address?.let { address ->
                    InetAddress.getByAddress(address)
                }
            }
        }

        val aaaaRecords = Lookup(name, Type.AAAA).let { lookup ->
            lookup.setCache(null)
            (lookup.run() ?: arrayOf()).mapNotNull { record ->
                (record as? AAAARecord)?.address?.address?.let { address ->
                    InetAddress.getByAddress(address)
                }
            }
        }

        return aRecords + aaaaRecords
    }

    private fun refresh() {
        val updated = mutableMapOf<String, List<InetAddress>>()

        logger.debug("Refreshing DNS cache")
        cache.updateAndGet { current ->
            updated += current.filterNot { updated.containsKey(it.key) }
                .mapValues { (k, _) -> resolve(k) }
            if (updated != current) {
                updated
            } else {
                current
            }
        }
    }
}
