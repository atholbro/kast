package com.github.atholbro.udpkt

private const val FRAME_VERSION: Byte = 1

private const val NOTIFY_KEY_TYPE_STRING: Byte = 0
private const val NOTIFY_KEY_TYPE_U8: Byte = 1
private const val NOTIFY_KEY_TYPE_U16: Byte = 2
private const val NOTIFY_KEY_TYPE_U32: Byte = 3
private const val NOTIFY_KEY_TYPE_U64: Byte = 4
private const val NOTIFY_KEY_TYPE_S8: Byte = 5
private const val NOTIFY_KEY_TYPE_S16: Byte = 6
private const val NOTIFY_KEY_TYPE_S32: Byte = 7
private const val NOTIFY_KEY_TYPE_S64: Byte = 8
private const val NOTIFY_KEY_TYPE_UUID: Byte = 9

private const val FRAME_HEADER_BYTES = 4
private const val PAYLOAD_MAX_BYTES = 1200

const val FRAME_MAX_BYTES = FRAME_HEADER_BYTES + PAYLOAD_MAX_BYTES

enum class MessageType {
    USER_DATA,
    NOTIFY,
}

interface PayloadEncoder<T> {
    val type: MessageType
    fun encode(payload: T): ByteArray
}

interface PayloadDecoder<T> {
    val type: MessageType
    fun decode(buffer: ByteArray, offset: Int, size: Int): T
}

object ByteArrayEncoding : PayloadEncoder<ByteArray>, PayloadDecoder<ByteArray> {
    override val type: MessageType = MessageType.USER_DATA

    override fun encode(payload: ByteArray): ByteArray = payload

    override fun decode(buffer: ByteArray, offset: Int, size: Int): ByteArray {
        val payload = ByteArray(size)
        System.arraycopy(buffer, offset, payload, 0, size)
        return payload
    }
}

object StringEncoding : PayloadEncoder<String>, PayloadDecoder<String> {
    override val type: MessageType = MessageType.USER_DATA

    override fun encode(payload: String): ByteArray = payload.toByteArray(Charsets.UTF_8)

    override fun decode(buffer: ByteArray, offset: Int, size: Int): String =
        buffer.decodeToString(offset, offset + size)
}

data class Notification<T>(val id: String, val payload: T)

class NotificationEncoder<T>(val payloadEncoder: PayloadEncoder<T>) : PayloadEncoder<Notification<T>> {
    override val type: MessageType get() = MessageType.NOTIFY

    override fun encode(payload: Notification<T>): ByteArray {
        val payloadBytes = payloadEncoder.encode(payload.payload)
        val idBytes = payload.id.encodeToByteArray()
        val total = 2 + idBytes.size + payloadBytes.size
        require(total <= PAYLOAD_MAX_BYTES)

        val out = ByteArray(total)
        // manual write avoids ByteBuffer
        out[0] = ((idBytes.size ushr 8) and 0xFF).toByte()
        out[1] = (idBytes.size and 0xFF).toByte()
        System.arraycopy(idBytes, 0, out, 2, idBytes.size)
        System.arraycopy(payloadBytes, 0, out, 2 + idBytes.size, payloadBytes.size)

        return out
    }
}

class NotificationDecoder<T>(val payloadDecoder: PayloadDecoder<T>) : PayloadDecoder<Notification<T>> {
    override val type: MessageType get() = MessageType.NOTIFY

    override fun decode(buffer: ByteArray, offset: Int, size: Int): Notification<T> {
        var pos = offset
        val idBytes = ((buffer[pos].toInt() and 0xFF) shl 8) or (buffer[++pos].toInt() and 0xFF)
        require(idBytes >= 0 && idBytes <= size - 2)

        val id = buffer.decodeToString(++pos, pos + idBytes)
        pos += idBytes

        val payloadLength = size - pos + offset
        require(payloadLength >= 0)

        return Notification(id, payloadDecoder.decode(buffer, pos, payloadLength))
    }
}

internal fun <T> encode(payload: T, encoder: PayloadEncoder<T>): ByteArray {
    val payloadBytes = encoder.encode(payload)
    val payloadSize = payloadBytes.size
    require(payloadSize <= PAYLOAD_MAX_BYTES)

    val out = ByteArray(FRAME_HEADER_BYTES + payloadSize)
    out[0] = FRAME_VERSION
    out[1] = encoder.type.ordinal.toByte()
    out[2] = ((payloadSize ushr 8) and 0xFF).toByte()
    out[3] = (payloadSize and 0xFF).toByte()
    System.arraycopy(payloadBytes, 0, out, FRAME_HEADER_BYTES, payloadSize)

    return out
}

fun <T> decode(input: ByteArray, decoder: PayloadDecoder<T>): T {
    require(input.size >= FRAME_HEADER_BYTES)

    val version = input[0]
    val type = input[1]
    val length = ((input[2].toInt() and 0xFF) shl 8) or (input[3].toInt() and 0xFF)
    require(version == FRAME_VERSION)
    require(type == decoder.type.ordinal.toByte())
    require(length <= PAYLOAD_MAX_BYTES)

    val payloadOffset = FRAME_HEADER_BYTES
    return decoder.decode(input, payloadOffset, length)
}
