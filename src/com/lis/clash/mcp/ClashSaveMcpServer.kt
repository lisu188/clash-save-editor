package com.lis.clash.mcp

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PrintWriter

internal class McpStdioServer(
    private val saveTools: SaveMcpTools = SaveMcpTools()
) {
    private val supportedProtocolVersions = listOf(
        "2025-11-25",
        "2025-06-18",
        "2025-03-26",
        "2024-11-05"
    )

    fun serve(
        input: BufferedReader = BufferedReader(InputStreamReader(System.`in`, Charsets.UTF_8)),
        output: PrintWriter = PrintWriter(OutputStreamWriter(System.out, Charsets.UTF_8), true)
    ) {
        while (true) {
            val line = input.readLine() ?: return
            val response = handleMessage(line)
            if (response != null) {
                output.println(response)
                output.flush()
            }
        }
    }

    fun handleMessage(line: String): String? {
        val message = try {
            Json.parse(line)
        } catch (exception: Exception) {
            return Json.stringify(errorResponse(null, -32700, "Parse error: ${exception.message}"))
        }

        if (message !is Map<*, *>) {
            return Json.stringify(errorResponse(null, -32600, "Invalid Request"))
        }

        val id = message["id"]
        val method = message["method"] as? String
        if (method == null) {
            return Json.stringify(errorResponse(id, -32600, "Invalid Request"))
        }

        // Notifications have no id and must not receive a JSON-RPC response.
        if (!message.containsKey("id")) {
            return null
        }

        return try {
            Json.stringify(successResponse(id, handleRequest(method, message["params"])))
        } catch (exception: ProtocolException) {
            Json.stringify(errorResponse(id, exception.code, exception.message ?: "Protocol error"))
        } catch (exception: Exception) {
            Json.stringify(errorResponse(id, -32603, exception.message ?: "internal error"))
        }
    }

    private fun handleRequest(method: String, params: Any?): Map<String, Any?> {
        return when (method) {
            "initialize" -> initialize(params)
            "ping" -> linkedMapOf()
            "tools/list" -> linkedMapOf(
                "tools" to saveTools.listTools().map { it.toProtocolMap() }
            )
            "tools/call" -> callTool(params)
            else -> throw ProtocolException(-32601, "Method not found: $method")
        }
    }

    private fun initialize(params: Any?): Map<String, Any?> {
        val protocolVersion = (params as? Map<*, *>)?.get("protocolVersion") as? String
        val negotiatedVersion = protocolVersion
            ?.takeIf { it in supportedProtocolVersions }
            ?: supportedProtocolVersions.first()

        return linkedMapOf(
            "protocolVersion" to negotiatedVersion,
            "capabilities" to linkedMapOf(
                "tools" to linkedMapOf(
                    "listChanged" to false
                )
            ),
            "serverInfo" to linkedMapOf(
                "name" to "clash-save-editor",
                "version" to "1.0-SNAPSHOT"
            ),
            "instructions" to "Use save_get_schema first, then inspect save files with save_get_overview, save_list_entities, save_read_object, and save_read_bytes. Use save_set_property or save_write_bytes for intentional modifications."
        )
    }

    private fun callTool(params: Any?): Map<String, Any?> {
        val paramsMap = params.asStringKeyMap("params")
        val name = paramsMap["name"] as? String
            ?: throw ProtocolException(-32602, "tools/call requires string param 'name'.")
        val arguments = when (val value = paramsMap["arguments"]) {
            null -> emptyMap()
            else -> value.asStringKeyMap("arguments")
        }

        val result = saveTools.call(name, arguments)
        val protocolResult = linkedMapOf<String, Any?>(
            "content" to listOf(
                linkedMapOf(
                    "type" to "text",
                    "text" to result.text
                )
            ),
            "structuredContent" to result.structuredContent
        )
        if (result.isError) {
            protocolResult["isError"] = true
        }
        return protocolResult
    }

    private fun successResponse(id: Any?, result: Map<String, Any?>): Map<String, Any?> {
        return linkedMapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "result" to result
        )
    }

    private fun errorResponse(id: Any?, code: Int, message: String): Map<String, Any?> {
        return linkedMapOf(
            "jsonrpc" to "2.0",
            "id" to id,
            "error" to linkedMapOf(
                "code" to code,
                "message" to message
            )
        )
    }

    private fun Any?.asStringKeyMap(name: String): Map<String, Any?> {
        if (this !is Map<*, *>) {
            throw ProtocolException(-32602, "$name must be an object.")
        }
        val result = linkedMapOf<String, Any?>()
        for ((key, value) in this) {
            if (key !is String) {
                throw ProtocolException(-32602, "$name contains a non-string key.")
            }
            result[key] = value
        }
        return result
    }

    private class ProtocolException(
        val code: Int,
        override val message: String
    ) : RuntimeException(message)
}

fun main() {
    McpStdioServer().serve()
}
