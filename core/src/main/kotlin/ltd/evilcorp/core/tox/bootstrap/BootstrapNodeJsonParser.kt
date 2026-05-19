package ltd.evilcorp.core.tox.bootstrap

import android.util.Log
import javax.inject.Inject
import ltd.evilcorp.core.model.PublicKey
import org.json.JSONObject

private const val TAG = "BootstrapNodeJsonParser"

/**
 * Парсер списка публичных DHT-узлов из JSON формата, возвращаемого ресурсом https://nodes.tox.chat/json.
 * Отбирает только активные (online) узлы, поддерживающие как TCP, так и UDP.
 */
class BootstrapNodeJsonParser @Inject constructor() {
    /**
     * Преобразует JSON-строку в список объектов [BootstrapNode].
     * @param jsonString Исходная JSON-строка для парсинга.
     * @return Список валидных узлов для бутстрапа. В случае ошибки возвращает пустой список.
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
        Log.e(TAG, e.toString())
        listOf()
    }
}
