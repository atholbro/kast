package net.aholbrook.kast

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ProtocolTest {
    @Test
    fun `Can decode existing packet`() {
        val encodedBytes = byteArrayOf(1, 1, 0, 12, 0, 5, 116, 101, 115, 116, 51, 5, 4, 3, 2, 1)
        val decoded = decode(encodedBytes, NotificationDecoder(ByteArrayEncoding))

        decoded.id shouldBe "test3"
        decoded.payload shouldBe byteArrayOf(5, 4, 3, 2, 1)
    }

    @Test
    fun `Encoding produces the expected packet bytes`() {
        val n = Notification("test3", byteArrayOf(5, 4, 3, 2, 1))
        val bytes = encode(n, NotificationEncoder(ByteArrayEncoding))

        bytes shouldBe byteArrayOf(1, 1, 0, 12, 0, 5, 116, 101, 115, 116, 51, 5, 4, 3, 2, 1)
    }

    @Test
    fun `String - can encode then decode`() {
        val n = Notification("test1", "here's the data")
        val bytes = encode(n, NotificationEncoder(StringEncoding))
        val decoded = decode(bytes, NotificationDecoder(StringEncoding))

        n.id shouldBe decoded.id
        n.payload shouldBe decoded.payload
    }

    @Test
    fun `ByteArray - can encode then decode`() {
        val data = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
        val n = Notification("test3", data)
        val bytes = encode(n, NotificationEncoder(ByteArrayEncoding))
        val decoded = decode(bytes, NotificationDecoder(ByteArrayEncoding))

        n.id shouldBe decoded.id
        n.payload shouldBe decoded.payload
    }

    @Test
    fun `decode rejects truncated frame payload`() {
        val truncated = byteArrayOf(1, 1, 0, 12, 0, 5, 116, 101, 115, 116)

        shouldThrow<IllegalArgumentException> {
            decode(truncated, NotificationDecoder(ByteArrayEncoding))
        }
    }
}
