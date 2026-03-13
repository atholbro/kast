# kast: Kubernetes Asynchronous Signaling Transport

`kast` is a small Kotlin library for DNS-driven asynchronous signaling on Kubernetes over UDP fanout.

## Why this exists

Most Kubernetes environments do not support L2/L3 broadcast semantics across pods, so classic UDP broadcast is usually
unavailable or inconsistent.

`kast` works around this by:

1. resolving peer hostnames via DNS (`A` and `AAAA`),
2. caching and periodically refreshing those addresses,
3. sending a unicast UDP datagram to each resolved peer.

The result is broadcast-like behavior built on top of DNS + UDP unicast.

## Reliability model

This is designed for **mostly reliable event notifications**, not strict guaranteed delivery.

- UDP transport means packets can still be lost, duplicated, or reordered.
- Fanout to all resolved peers increases delivery odds in clustered deployments.
- Notifications are keyed by ID and consumed by listeners waiting on that ID.

Use this for low-latency signals where occasional loss is acceptable.

## Packet sizing and MTU

`kast` caps each frame at `1204` bytes (`4`-byte header + `1200`-byte payload).

This limit is intentional: staying well below typical MTU ceilings reduces the chance of IP fragmentation,
which improves the odds of fast, successful delivery.

- Typical Ethernet MTU is `1500` bytes.

- `1204` leaves margin for IPv4/IPv6 + UDP headers and common network overhead in Kubernetes environments.

## Protocol overview

Each datagram contains a single frame:

### Frame

| Field         |         Size |
|---------------|-------------:|
| Header        |    `4` bytes |
| Payload (max) | `1200` bytes |
| Frame (max)   | `1204` bytes |

### Header

| Offset | Name            |      Size | Notes                  |
|-------:|-----------------|----------:|------------------------|
|    `0` | `version`       |  `1` byte | Currently `1`          |
|    `1` | `messageType`   |  `1` byte | See message type table |
|    `2` | `payloadLength` | `2` bytes | Big-endian             |
|    `4` | `payload`       | `N` bytes | `N = payloadLength`    |

### Message types

| Value | Name        | Purpose                                |
|------:|-------------|----------------------------------------|
|   `0` | `USER_DATA` | Raw user payload                       |
|   `1` | `NOTIFY`    | Notification envelope (`id` + payload) |

### `NOTIFY`, payload layout:

| Offset (within payload) | Name       |             Size | Notes                                 |
|------------------------:|------------|-----------------:|---------------------------------------|
|                     `0` | `idLength` |        `2` bytes | Big-endian                            |
|                     `2` | `id`       | `idLength` bytes | UTF-8                                 |
|          `2 + idLength` | `data`     |  Remaining bytes | Decoded by configured payload decoder |

### Example:

```text
[1, 1, 0, 12, 0, 5, 116, 101, 115, 116, 51, 5, 4, 3, 2, 1]
```

| Bytes                | Meaning                   |
|----------------------|---------------------------|
| `1`                  | Version                   |
| `1`                  | Message type (`NOTIFY`)   |
| `0,12`               | Payload length = `12`     |
| `0,5`                | ID length = `5`           |
| `116,101,115,116,51` | UTF-8 `"test3"`           |
| `5,4,3,2,1`          | Notification payload data |

## Minimal usage

```kotlin
fun main() = runBlocking {
    val sender = Sender(
        hosts = arrayOf("demo.default.svc.cluster.local"),
        port = 9999,
        encoder = NotificationEncoder(StringEncoding),
    )

    val receiver = Receiver(
        port = 9999,
        decoder = NotificationDecoder(StringEncoding),
    )

    receiver.start()
    sender.start()

    sender.notifyAll(Notification("job-123", "done"))

    receiver.whenNotified("job-123", Duration.ofSeconds(5)) { payload ->
        println("received: $payload")
    }

    receiver.stop()
    sender.stop()
}
```

## Kubernetes notes

- Use stable DNS names (often a headless service) for peer discovery.
- Tune DNS refresh interval based on pod churn and DNS load.
- Keep payloads small to avoid fragmentation and dropped packets.
