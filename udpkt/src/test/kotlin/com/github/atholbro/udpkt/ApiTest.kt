package com.github.atholbro.udpkt

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Duration

class ApiTest {
    @Test
    fun `can send and receive a notification via ipv4 loopback`(): Unit = runBlocking {
        val sender = Sender(
            arrayOf("127.0.0.1"),
            9999,
            NotificationEncoder(StringEncoding),
        )
        val receiver = Receiver(
            9999,
            NotificationDecoder(StringEncoding),
        )

        launch {
            delay(50)
            sender.notifyAll(Notification("100", "this is the message"))
        }

        var received: String? = null
        receiver.start()
        receiver.whenNotified("100", Duration.ofMillis(500)) {
            received = it
        }

        delay(50)
        receiver.stop()

        received shouldBe "this is the message"
    }

    @Test
    fun `can send and receive a notification via ipv6 loopback`(): Unit = runBlocking {
        val sender = Sender(
            arrayOf("::1"),
            9991,
            NotificationEncoder(StringEncoding),
        )
        val receiver = Receiver(
            9991,
            NotificationDecoder(StringEncoding),
        )

        launch {
            delay(50)
            sender.notifyAll(Notification("100", "this is the message"))
        }

        var received: String? = null
        receiver.start()
        receiver.whenNotified("100", Duration.ofMillis(500)) {
            received = it
        }

        delay(50)
        receiver.stop()

        received shouldBe "this is the message"
    }

    @Test
    fun `receiver timeout`(): Unit = runBlocking {
        val receiver = Receiver(
            9998,
            NotificationDecoder(StringEncoding),
        )

        receiver.start()

        shouldThrow<TimeoutCancellationException> {
            receiver.whenNotified("101", Duration.ofMillis(500)) { }
        }

        receiver.stop()
    }
}
