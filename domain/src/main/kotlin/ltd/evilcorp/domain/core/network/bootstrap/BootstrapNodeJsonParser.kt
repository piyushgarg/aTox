// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network.bootstrap

import javax.inject.Inject
import ltd.evilcorp.domain.core.model.PublicKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

private const val TAG = "BootstrapNodeJsonParser"

@Serializable
internal data class BootstrapNodesResponse(
    val nodes: List<JsonBootstrapNode>
)

@Serializable
internal data class JsonBootstrapNode(
    val ipv4: String,
    val port: Int,
    @SerialName("public_key") val publicKey: String,
    @SerialName("status_tcp") val statusTcp: Boolean,
    @SerialName("status_udp") val statusUdp: Boolean
)

/**
 * Parser for the public DHT nodes list in JSON format, typically returned by https://nodes.tox.chat/json.
 * Filters out only active (online) nodes supporting both TCP and UDP.
 */
class BootstrapNodeJsonParser @Inject constructor() {
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parses a JSON string into a list of [BootstrapNode] objects.
     * @param jsonString The raw JSON string to parse.
     * @return A list of valid bootstrap nodes, or an empty list in case of parsing errors.
     */
    fun parse(jsonString: String): List<BootstrapNode> = try {
        val response = json.decodeFromString<BootstrapNodesResponse>(jsonString)
        response.nodes
            .filter { it.statusUdp && it.statusTcp && it.ipv4 != "-" }
            .map {
                BootstrapNode(
                    address = it.ipv4,
                    port = it.port,
                    publicKey = PublicKey(it.publicKey),
                )
            }
    } catch (e: Exception) {
        System.err.println("[$TAG] Error parsing bootstrap nodes: $e")
        listOf()
    }
}
