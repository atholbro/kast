package net.aholbrook.kast

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import net.aholbrook.kast.ReferenceStrength.STRONG
import net.aholbrook.kast.ReferenceStrength.WEAK
import java.lang.ref.WeakReference
import java.time.Duration

private val logger = KotlinLogging.logger {}

private sealed interface Ref<T> {
    fun get(): T?
}

@JvmInline
private value class WeakRef<T>(val ref: WeakReference<T>) : Ref<T> {
    override fun get(): T? = ref.get()
}

@JvmInline
private value class StrongRef<T>(val ref: T) : Ref<T> {
    override fun get(): T? = ref
}

enum class ReferenceStrength {
    STRONG,
    WEAK,
}

private fun <T> ReferenceStrength.createRef(ref: T) = when (this) {
    STRONG -> StrongRef(ref)
    WEAK -> WeakRef(WeakReference(ref))
}

internal class BroadcastManager<_Id, _Data> {
    private val mutex = Mutex()
    private val listeners = mutableMapOf<_Id, MutableList<Ref<CompletableDeferred<_Data>>>>()

    suspend fun notify(jobId: _Id, data: _Data, timeout: Duration) {
        notify(jobId, data, timeout.toMillis())
    }

    suspend fun notify(jobId: _Id, data: _Data, timeoutMs: Long = 0) {
        suspend fun notifyLocked() {
            mutex.withLock {
                listeners.remove(jobId)
            }?.forEach {
                it.get()?.complete(data)
            }
        }

        if (timeoutMs > 0) {
            try {
                withTimeout(timeoutMs) {
                    notifyLocked()
                }
            } catch (e: TimeoutCancellationException) {
                logger.debug(e) { "timed out waiting for notification lock" }
            }
        } else {
            notifyLocked()
        }
    }

    @Suppress("UnusedParameter")
    suspend fun register(
        jobId: _Id,
        timeout: Duration,
        referenceStrength: ReferenceStrength = ReferenceStrength.STRONG,
    ): CompletableDeferred<_Data>? = register(jobId, timeout.toMillis())

    suspend fun register(
        jobId: _Id,
        timeoutMs: Long = 0,
        referenceStrength: ReferenceStrength = ReferenceStrength.STRONG,
    ): CompletableDeferred<_Data>? {
        val deferred = CompletableDeferred<_Data>()
        val ref = referenceStrength.createRef(deferred)

        suspend fun registerLocked() {
            mutex.withLock {
                listeners.getOrPut(jobId) { mutableListOf() }.add(ref)
            }
        }

        return if (timeoutMs > 0) {
            try {
                withTimeout(timeoutMs) {
                    registerLocked()
                    deferred
                }
            } catch (e: TimeoutCancellationException) {
                logger.debug(e) { "timed out waiting for notification lock" }
                null
            }
        } else {
            registerLocked()
            deferred
        }
    }
}
