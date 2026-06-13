package ltd.evilcorp.core.tox.runtime

/**
 * Enum of possible errors when sending user P2P packets.
 */
@Suppress("unused")
enum class CustomPacketError {
    /** Successful packet transmission. */
    Success,
    /** Packet is empty. */
    Empty,
    /** Friend is currently offline. */
    FriendNotConnected,
    /** Specified friend is not found in the contact list. */
    FriendNotFound,
    /** Invalid packet parameters. */
    Invalid,
    /** Packet buffer reference is null. */
    Null,
    /** Packet send queue is full. */
    Sendq,
    /** Maximum packet length is exceeded. */
    TooLong,
}
