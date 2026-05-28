// SPDX-FileCopyrightText: 2026 aTox contributors
//
// SPDX-License-Identifier: GPL-3.0-only

package ltd.evilcorp.domain.core.network.bootstrap

import javax.inject.Inject
import ltd.evilcorp.domain.core.model.PublicKey
import org.json.JSONObject

private const val TAG = "BootstrapNodeJsonParser"

/**
 * Parser for the public DHT nodes list in JSON format, typically returned by https://nodes.tox.chat/json.
 * Filters out only active (online) nodes supporting both TCP and UDP.
 */
class BootstrapNodeJsonParser @Inject constructor() {
    /**
     * Parses a JSON string into a list of [BootstrapNode] objects.
     * @param jsonString The raw JSON string to parse.
     * @return A list of valid bootstrap nodes, or an empty list in case of parsing errors.
     */
    fun parse(jsonString: String): List<BootstrapNode> = try {
        val nodes = mutableListOf<BootstrapNode>()

        val json = JSONObject(jsonString)
        val jsonNodes = json.getJSONArray("nodes")
        for (i in 0 until jsonNodes.length()) {
            val jsonNode = jsonNodes.getJSONObject(i)
            val isOnline = jsonNode.getBoolean("status_udp") && jsonNode.getBoolean("status_tcp")
            val address = jsonNode.getString("ipv4")
            if (isOnline && address != "-") {
                nodes.add(
                    BootstrapNode(
                        address = address,
                        port = jsonNode.getInt("port"),
                        publicKey = PublicKey(jsonNode.getString("public_key")),
                    ),
                )
            }
        }

        nodes
    } catch (e: Exception) {
        System.err.println("[$TAG] Error parsing bootstrap nodes: $e")
        listOf()
    }
}
