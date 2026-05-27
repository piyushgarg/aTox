package ltd.evilcorp.core.tox

import ltd.evilcorp.domain.model.UserStatus
import ltd.evilcorp.core.tox.listener.ToxEventListener
import ltd.evilcorp.domain.tox.enums.ToxFileControl
import ltd.evilcorp.domain.tox.enums.ToxFileKind
import ltd.evilcorp.domain.tox.enums.ToxMessageType

class NativeTox {
    init {
        System.loadLibrary("nativetox")
    }

    /**
     * Creates and initializes a new native Tox instance.
     * @param savedata Previously saved profile byte data (tox save), or null to create a new account.
     * @return Pointer to the native Tox structure in memory.
     */
    external fun toxNew(savedata: ByteArray?): Long

    /**
     * Creates and initializes a new native Tox instance with a full set of network options and proxy support.
     * @param savedata Previously saved profile byte data (tox save), or null.
     * @param ipv6Enabled true to enable IPv6.
     * @param udpEnabled true to enable UDP (direct connections), false to force TCP-only mode.
     * @param localDiscoveryEnabled true to enable node discovery in local Wi-Fi/LAN networks.
     * @param proxyType Proxy type (0 - no proxy, 1 - HTTP, 2 - SOCKS5).
     * @param proxyHost Proxy server host (e.g., "127.0.0.1" for local Tor daemon), or null.
     * @param proxyPort Proxy server port (e.g., 9050 for Tor).
     * @return Pointer to the native Tox structure in memory, or 0 in case of error.
     */
    external fun toxNewWithOptions(
        savedata: ByteArray?,
        ipv6Enabled: Boolean,
        udpEnabled: Boolean,
        localDiscoveryEnabled: Boolean,
        proxyType: Int,
        proxyHost: String?,
        proxyPort: Int
    ): Long

    /**
     * Destroys the Tox instance and frees all associated native memory.
     * @param tox Pointer to the Tox instance.
     */
    external fun toxKill(tox: Long)
    
    /**
     * Connects the instance to a public DHT bootstrap node of the Tox network.
     * @param tox Pointer to the Tox instance.
     * @param address IP address or domain name of the DHT node.
     * @param port Port of the DHT node.
     * @param publicKey Public key of the DHT node (32 bytes).
     */
    external fun toxBootstrap(tox: Long, address: String, port: Int, publicKey: ByteArray)

    /**
     * Adds a TCP relay to bypass strict NAT and firewalls.
     * @param tox Pointer to the Tox instance.
     * @param address IP address or domain name of the TCP relay.
     * @param port Port of the TCP relay.
     * @param publicKey Public key of the TCP relay (32 bytes).
     */
    external fun toxAddTcpRelay(tox: Long, address: String, port: Int, publicKey: ByteArray)
    
    /**
     * Main work cycle of Tox: processes incoming/outgoing network packets and triggers registered callbacks.
     * Must be called regularly in a loop.
     * @param tox Pointer to the Tox instance.
     * @param listener The event listener implementation to forward events into Kotlin.
     */
    external fun toxIterate(tox: Long, listener: ToxEventListener)

    /**
     * Returns the recommended time interval in milliseconds before the next call to [toxIterate].
     * @param tox Pointer to the Tox instance.
     * @return Interval in milliseconds.
     */
    external fun toxIterationInterval(tox: Long): Int

    /**
     * Returns the current name of our Tox profile.
     * @param tox Pointer to the Tox instance.
     * @return Profile name as a byte array (UTF-8).
     */
    external fun toxGetName(tox: Long): ByteArray

    /**
     * Sets a new name for our Tox profile.
     * @param tox Pointer to the Tox instance.
     * @param name The new name as a byte array (UTF-8).
     */
    external fun toxSetName(tox: Long, name: ByteArray)

    /**
     * Returns the current status message of our profile.
     * @param tox Pointer to the Tox instance.
     * @return Status message text as a byte array (UTF-8).
     */
    external fun toxGetStatusMessage(tox: Long): ByteArray

    /**
     * Sets a new status message for our profile.
     * @param tox Pointer to the Tox instance.
     * @param msg New status message text as a byte array (UTF-8).
     */
    external fun toxSetStatusMessage(tox: Long, msg: ByteArray)
    
    /**
     * Returns the full unique Tox ID of our account (38 bytes: 32 bytes public key + 4 bytes nospam + 2 bytes checksum).
     * @param tox Pointer to the Tox instance.
     * @return Full Tox address as a byte array.
     */
    external fun toxGetAddress(tox: Long): ByteArray

    /**
     * Returns only the public key (Tox Public Key) of our account (32 bytes).
     * @param tox Pointer to the Tox instance.
     * @return Public key as a byte array.
     */
    external fun toxGetPublicKey(tox: Long): ByteArray

    /**
     * Returns the secret (private) key of our account (32 bytes).
     * WARNING: The secret key must be kept strictly confidential!
     * @param tox Pointer to the Tox instance.
     * @return Secret key as a byte array.
     */
    external fun toxSelfGetSecretKey(tox: Long): ByteArray

    /**
     * Returns the active UDP port of our running node.
     * @param tox Pointer to the Tox instance.
     * @return Port number, or 0 in case of error or if UDP is disabled.
     */
    external fun toxSelfGetUdpPort(tox: Long): Int

    /**
     * Returns the active TCP port of our running node.
     * @param tox Pointer to the Tox instance.
     * @return Port number, or 0 in case of error.
     */
    external fun toxSelfGetTcpPort(tox: Long): Int

    /**
     * Returns the temporary DHT key (DHT ID) of our instance (32 bytes).
     * Used for diagnosing connection status with the global network.
     * @param tox Pointer to the Tox instance.
     * @return DHT ID as a byte array.
     */
    external fun toxSelfGetDhtId(tox: Long): ByteArray
    
    /**
     * Returns the current Nospam value of our profile (used to prevent spam).
     * @param tox Pointer to the Tox instance.
     * @return 32-bit nospam number.
     */
    external fun toxGetNospam(tox: Long): Int

    /**
     * Sets a new Nospam value to generate a new Tox ID.
     * @param tox Pointer to the Tox instance.
     * @param nospam The new 32-bit number.
     */
    external fun toxSetNospam(tox: Long, nospam: Int)
    
    /**
     * Serializes the current Tox profile state (friend list, name, settings) into bytes for subsequent saving.
     * @param tox Pointer to the Tox instance.
     * @return Saved state as a byte array.
     */
    external fun toxGetSavedata(tox: Long): ByteArray

    /**
     * Sends a friend request.
     * @param tox Pointer to the Tox instance.
     * @param pubKey Friend's public key (32 bytes).
     * @param message Greeting text message (UTF-8).
     * @return Created friend's ID (friend number), or error code (negative number).
     */
    external fun toxAddFriend(tox: Long, pubKey: ByteArray, message: ByteArray): Int

    /**
     * Adds a friend to the friend list without sending a request (e.g., during confirmation or import).
     * @param tox Pointer to the Tox instance.
     * @param pubKey Friend's public key (32 bytes).
     * @return Created friend's ID (friend number), or error code.
     */
    external fun toxAddFriendNorequest(tox: Long, pubKey: ByteArray): Int

    /**
     * Removes a friend from the contact list.
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number to remove.
     */
    external fun toxDeleteFriend(tox: Long, friendNumber: Int)
    
    /**
     * Returns the list of identifiers (numbers) of all friends in our contact list.
     * @param tox Pointer to the Tox instance.
     * @return Array of friend identifiers.
     */
    external fun toxGetFriendList(tox: Long): IntArray

    /**
     * Returns the public key of a friend by their number.
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @return Friend's public key (32 bytes).
     */
    external fun toxGetFriendPublicKey(tox: Long, friendNumber: Int): ByteArray

    /**
     * Finds a friend's number in the contact list by their public key.
     * @param tox Pointer to the Tox instance.
     * @param pubKey Public key (32 bytes).
     * @return Friend number in the contact list, or -1 if the friend is not found.
     */
    external fun toxFriendByPublicKey(tox: Long, pubKey: ByteArray): Int

    /**
     * Checks if a friend with the specified number exists in our contact list.
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @return true if the friend exists, false otherwise.
     */
    external fun toxFriendExists(tox: Long, friendNumber: Int): Boolean

    /**
     * Returns the current name of a friend by their number (direct query).
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @return Friend's name as a byte array (UTF-8).
     */
    external fun toxFriendGetName(tox: Long, friendNumber: Int): ByteArray

    /**
     * Returns the status message of a friend by their number (direct query).
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @return Status message as a byte array (UTF-8).
     */
    external fun toxFriendGetStatusMessage(tox: Long, friendNumber: Int): ByteArray

    /**
     * Returns the online status of a friend (Away/Busy/Online) by their number (direct query).
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @return 0 - online, 1 - away, 2 - busy.
     */
    external fun toxFriendGetStatus(tox: Long, friendNumber: Int): Int

    /**
     * Returns the current network connection type with a friend (direct query).
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @return 0 - offline, 1 - TCP connection, 2 - UDP connection.
     */
    external fun toxFriendGetConnectionStatus(tox: Long, friendNumber: Int): Int

    /**
     * Checks if a friend is currently typing a message to us (direct query).
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @return true if the friend is typing, false otherwise.
     */
    external fun toxFriendGetTyping(tox: Long, friendNumber: Int): Boolean

    /**
     * Returns the UNIX time of the last recorded online presence of a friend.
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @return UNIX timestamp (in seconds), or 0 if the friend is online right now or has never connected.
     */
    external fun toxFriendGetLastOnline(tox: Long, friendNumber: Int): Long
    
    /**
     * Sends a private text message to a friend.
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @param type Message type (0 - normal, 1 - action).
     * @param message Message text as a byte array (UTF-8).
     * @return Unique ID of the sent message, or 0 in case of error.
     */
    external fun toxFriendSendMessage(tox: Long, friendNumber: Int, type: Int, message: ByteArray): Int

    /**
     * Notifies a friend whether we are currently typing a message or not.
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @param typing true if we are typing, false otherwise.
     */
    external fun toxSetTyping(tox: Long, friendNumber: Int, typing: Boolean)
    
    /**
     * Returns the current online status of our profile (0 - online, 1 - away, 2 - busy).
     * @param tox Pointer to the Tox instance.
     * @return Status code.
     */
    external fun toxGetSelfUserStatus(tox: Long): Int

    /**
     * Sets a new online status for our profile.
     * @param tox Pointer to the Tox instance.
     * @param status Status code (0 - online, 1 - away, 2 - busy).
     */
    external fun toxSetSelfUserStatus(tox: Long, status: Int)
 
    /**
     * Controls the current file transfer (pause, resume, cancel).
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @param fileNumber File number.
     * @param control Control code (e.g., 0 - pause, 1 - resume, 2 - cancel).
     */
    external fun toxFileControl(tox: Long, friendNumber: Int, fileNumber: Int, control: Int)

    /**
     * Starts sending a file to a friend.
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @param kind File kind (0 - regular file, 1 - avatar).
     * @param fileSize Total file size in bytes.
     * @param fileId Unique file ID (32 bytes).
     * @param filename Filename as a byte array.
     * @return Unique file number within the session, or -1 on error.
     */
    external fun toxFileSend(tox: Long, friendNumber: Int, kind: Int, fileSize: Long, fileId: ByteArray, filename: ByteArray): Int

    /**
     * Sends a specific chunk of a file.
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @param fileNumber File number.
     * @param position Chunk position in the file (byte offset).
     * @param data Chunk bytes.
     */
    external fun toxFileSendChunk(tox: Long, friendNumber: Int, fileNumber: Int, position: Long, data: ByteArray)

    /**
     * Returns the unique 32-byte ID of a file being sent or received.
     * @param tox Pointer to the Tox instance.
     * @param friendNumber Friend number.
     * @param fileNumber File number.
     * @return 32-byte file ID, or null in case of error.
     */
    external fun toxFileGetFileId(tox: Long, friendNumber: Int, fileNumber: Int): ByteArray

    /**
     * Sends a custom lossless data packet to a friend (used for service data transmission).
     * @param tox Pointer to the native Tox instance.
     * @param friendNumber Friend number.
     * @param data Packet bytes.
     */
    external fun toxFriendSendLosslessPacket(tox: Long, friendNumber: Int, data: ByteArray)

    /**
     * Sends a custom unreliable lossy data packet to a friend (used for fast, non-critical data).
     * @param tox Pointer to the native Tox instance.
     * @param friendNumber Friend number.
     * @param data Packet bytes.
     */
    external fun toxFriendSendLossyPacket(tox: Long, friendNumber: Int, data: ByteArray)

    /**
     * Creates a new text conference (group chat).
     * @param tox Pointer to the native Tox instance.
     * @return Unique conference number (group ID), or -1 in case of error.
     */
    external fun toxConferenceNew(tox: Long): Int

    /**
     * Deletes an existing conference (leaving the group or closing it).
     * @param tox Pointer to the native Tox instance.
     * @param conferenceNumber Conference number to delete.
     */
    external fun toxConferenceDelete(tox: Long, conferenceNumber: Int)

    /**
     * Sends a conference invitation to a friend to join an existing conference.
     * @param tox Pointer to the native Tox instance.
     * @param friendNumber Friend number we are inviting.
     * @param conferenceNumber Conference number we are inviting them to.
     */
    external fun toxConferenceInvite(tox: Long, friendNumber: Int, conferenceNumber: Int)

    /**
     * Accepts an incoming invitation and joins the conference.
     * @param tox Pointer to the native Tox instance.
     * @param friendNumber Friend number who sent the invitation.
     * @param cookie Binary invitation data (cookie) received in the callback.
     * @return Joined conference number (group ID), or -1 in case of error.
     */
    external fun toxConferenceJoin(tox: Long, friendNumber: Int, cookie: ByteArray): Int

    /**
     * Sends a message to the conference (group chat).
     * @param tox Pointer to the native Tox instance.
     * @param conferenceNumber Conference number.
     * @param type Message type (e.g., normal text).
     * @param message Message text as a byte array (UTF-8).
     * @return 1 on successful send, 0 on error.
     */
    external fun toxConferenceSendMessage(tox: Long, conferenceNumber: Int, type: Int, message: ByteArray): Int

    /**
     * Sets a new title for the group chat.
     * @param tox Pointer to the native Tox instance.
     * @param conferenceNumber Conference number.
     * @param title New title as a byte array (UTF-8).
     */
    external fun toxConferenceSetTitle(tox: Long, conferenceNumber: Int, title: ByteArray)

    /**
     * Returns the current title of the group chat.
     * @param tox Pointer to the native Tox instance.
     * @param conferenceNumber Conference number.
     * @return Title as a byte array (UTF-8).
     */
    external fun toxConferenceGetTitle(tox: Long, conferenceNumber: Int): ByteArray

    /**
     * Checks if the conference peer with the specified number is ourselves.
     * @param tox Pointer to the native Tox instance.
     * @param conferenceNumber Conference number.
     * @param peerNumber Peer number.
     * @return true if the peer is ourselves, false otherwise.
     */
    external fun toxConferencePeerNumberIsOurself(tox: Long, conferenceNumber: Int, peerNumber: Int): Boolean

    /**
     * Returns the number of peers in a specific conference.
     * @param tox Pointer to the native Tox instance.
     * @param conferenceNumber Conference number.
     * @return Number of peers, or -1 on error.
     */
    external fun toxConferenceGetPeerCount(tox: Long, conferenceNumber: Int): Int

    /**
     * Returns the name of a conference peer by their index.
     * @param tox Pointer to the native Tox instance.
     * @param conferenceNumber Conference number.
     * @param peerNumber Peer index in the group.
     * @return Peer name as a byte array (UTF-8).
     */
    external fun toxConferenceGetPeerName(tox: Long, conferenceNumber: Int, peerNumber: Int): ByteArray

    /**
     * Returns the public key (Tox PublicKey) of a conference peer.
     * @param tox Pointer to the native Tox instance.
     * @param conferenceNumber Conference number.
     * @param peerNumber Peer public key (32 bytes).
     * @return Peer public key (32 bytes).
     */
    external fun toxConferenceGetPeerPublicKey(tox: Long, conferenceNumber: Int, peerNumber: Int): ByteArray

    /**
     * Returns the list of identifiers (numbers) of all active conferences the user is in.
     * @param tox Pointer to the native Tox instance.
     * @return Array of conference identifiers.
     */
    external fun toxConferenceGetChatlist(tox: Long): IntArray

    /**
     * Returns the conference type (text or audio/video).
     * @param tox Pointer to the native Tox instance.
     * @param conferenceNumber Conference number.
     * @return 0 for text group, 1 for A/V conference, or -1 in case of error.
     */
    external fun toxConferenceGetType(tox: Long, conferenceNumber: Int): Int

    // ===================================================================================
    // New Group Conferences NGC (Next Generation Conferences / Tox Groups)
    // ===================================================================================

    /**
     * Creates a new group NGC conference.
     * @param tox Pointer to the native Tox instance.
     * @param privacyState Group privacy state (0 - Public, 1 - Private).
     * @param groupName Name of the group being created as a byte array.
     * @param selfName Your name in the group being created.
     * @return Unique group number in Tox (Tox_Group_Number), or -1 in case of error.
     */
    external fun toxGroupNew(tox: Long, privacyState: Int, groupName: ByteArray, selfName: ByteArray): Int

    /**
     * Joins a group NGC conference using the received invitation.
     * @param tox Pointer to the native Tox instance.
     * @param friendNumber Friend number who sent the invitation.
     * @param inviteData Invitation data byte array (invite_data).
     * @param selfName Your name when entering the group.
     * @param password Group password (if any, otherwise null or empty array).
     * @return Joined group number, or -1 in case of error.
     */
    external fun toxGroupJoin(tox: Long, friendNumber: Int, inviteData: ByteArray, selfName: ByteArray, password: ByteArray?): Int

    /**
     * Leaves a group NGC conference.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @return true on success, false on error.
     */
    external fun toxGroupLeave(tox: Long, groupNumber: Int): Boolean

    /**
     * Sends a text message to a group NGC conference.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @param type Message type (0 - Normal, 1 - Action /me).
     * @param message Message text as a byte array.
     * @return Unique ID of the sent message within the group, or -1 in case of error.
     */
    external fun toxGroupSendMessage(tox: Long, groupNumber: Int, type: Int, message: ByteArray): Int

    /**
     * Sets the topic for a group NGC conference.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @param topic New topic as a byte array.
     * @return true on success, false on error.
     */
    external fun toxGroupSetTopic(tox: Long, groupNumber: Int, topic: ByteArray): Boolean

    /**
     * Gets the topic of a group NGC conference.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @return Topic byte array, or null in case of error.
     */
    external fun toxGroupGetTopic(tox: Long, groupNumber: Int): ByteArray?

    /**
     * Gets the name of a group NGC conference.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @return Name byte array, or null in case of error.
     */
    external fun toxGroupGetName(tox: Long, groupNumber: Int): ByteArray?

    /**
     * Returns the unique permanent 32-byte NGC chat identifier (Chat ID).
     * Required to track and sync groups across devices.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @return 32-byte Chat ID, or null in case of error.
     */
    external fun toxGroupGetChatId(tox: Long, groupNumber: Int): ByteArray?

    /**
     * Sets the password for access to a group NGC conference.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @param password Password as a byte array (null to remove password).
     * @return true on success, false on error.
     */
    external fun toxGroupSetPassword(tox: Long, groupNumber: Int, password: ByteArray?): Boolean

    /**
     * Returns the current set password of the group.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @return Password byte array, or null in case of error or if there is no password.
     */
    external fun toxGroupGetPassword(tox: Long, groupNumber: Int): ByteArray?

    /**
     * Gets the name of a group NGC conference peer by their ID.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @param peerId Internal identifier of the peer in the group.
     * @return Peer name byte array, or null in case of error.
     */
    external fun toxGroupPeerGetName(tox: Long, groupNumber: Int, peerId: Int): ByteArray?

    /**
     * Gets the public key of a group NGC conference peer by their ID.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @param peerId Internal identifier of the peer in the group.
     * @return 32-byte public key of the peer, or null in case of error.
     */
    external fun toxGroupPeerGetPublicKey(tox: Long, groupNumber: Int, peerId: Int): ByteArray?

    /**
     * Returns our own Peer ID in the group NGC conference.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @return Our Peer ID in the group, or -1 in case of error.
     */
    external fun toxGroupSelfGetPeerId(tox: Long, groupNumber: Int): Int

    /**
     * Returns our current role in the group NGC conference.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @return Role (0 - Owner, 1 - Moderator, 2 - Participant), or -1 in case of error.
     */
    external fun toxGroupSelfGetRole(tox: Long, groupNumber: Int): Int

    /**
     * Sends an invitation to a group NGC conference to a specific friend.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number.
     * @param friendNumber Friend number we are sending the invitation to.
     * @return true on success, false on error.
     */
    external fun toxGroupInviteSend(tox: Long, groupNumber: Int, friendNumber: Int): Boolean

    /**
     * Joins a group NGC conference directly by Chat ID (without a friend's invitation).
     * @param tox Pointer to the native Tox instance.
     * @param chatId 32-byte group Chat ID.
     * @param selfName Username when entering the group.
     * @param password Group password (if any, otherwise null or empty array).
     * @return Joined group number, or -1 in case of error.
     */
    external fun toxGroupJoinDirect(tox: Long, chatId: ByteArray, selfName: ByteArray, password: ByteArray?): Int

    /**
     * Reconnects to a previously saved group NGC conference.
     * Must be called after loading the saved profile for each group.
     * @param tox Pointer to the native Tox instance.
     * @param groupNumber Group number returned during the initial connection.
     * @return true on success, false on error.
     */
    external fun toxGroupReconnect(tox: Long, groupNumber: Int): Boolean

    // Tox profile encryption section (Tox Encrypt / Decrypt API)

    /**
     * Extracts the salt from the encrypted profile byte data for subsequent key generation.
     * @param data Encrypted profile byte array.
     * @return 32-byte salt, or null in case of error or if the data is not encrypted.
     */
    external fun getSalt(data: ByteArray): ByteArray?

    /**
     * Generates a cryptographic key (passkey) from a password and salt using PBKDF2.
     * @param passphrase Password as a byte array.
     * @param salt Salt as a byte array.
     * @return Generated key (passkey), or null in case of error.
     */
    external fun passKeyDeriveWithSalt(passphrase: ByteArray, salt: ByteArray): ByteArray?

    /**
     * Decrypts profile byte data using the generated key (passkey).
     * @param data Encrypted profile byte array.
     * @param passkey Generated passkey key (from [passKeyDeriveWithSalt]).
     * @return Decrypted profile byte array, or null in case of an invalid key or error.
     */
    external fun passDecrypt(data: ByteArray, passkey: ByteArray): ByteArray?

    /**
     * Encrypts profile byte data using the key (passkey).
     * @param data Original uncompressed profile bytes.
     * @param passkey Generated passkey key.
     * @return Encrypted byte array, ready for saving, or null in case of error.
     */
    external fun passEncrypt(data: ByteArray, passkey: ByteArray): ByteArray?
}
