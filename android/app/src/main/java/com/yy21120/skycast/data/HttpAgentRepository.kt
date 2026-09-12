package com.yy21120.skycast.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HttpAgentRepository(
    baseUrl: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : AgentRepository {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun chat(request: AgentChatRequest): AgentChatResponse =
        withContext(ioDispatcher) {
            val connection = URL("$normalizedBaseUrl/v1/agent/chat")
                .openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { stream ->
                    stream.write(json.encodeToString(request).encodeToByteArray())
                }

                val statusCode = connection.responseCode
                if (statusCode !in 200..299) {
                    throw AgentNetworkException("Agent 服务返回 HTTP $statusCode")
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<AgentChatResponse>(body)
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 35_000
    }
}
