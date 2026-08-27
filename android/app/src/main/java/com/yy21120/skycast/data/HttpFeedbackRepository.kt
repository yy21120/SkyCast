package com.yy21120.skycast.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class HttpFeedbackRepository(
    baseUrl: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : FeedbackRepository {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun submitSunsetFeedback(
        request: SunsetFeedbackRequest,
    ): SunsetFeedbackResponse = withContext(ioDispatcher) {
        val connection = URL("$normalizedBaseUrl/v1/feedback/sunset")
            .openConnection() as HttpURLConnection

        try {
            val requestBody = json.encodeToString(request).toByteArray(Charsets.UTF_8)
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.doOutput = true
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setFixedLengthStreamingMode(requestBody.size)
            connection.outputStream.use { it.write(requestBody) }

            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                throw FeedbackNetworkException("反馈服务返回 HTTP $statusCode")
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<SunsetFeedbackResponse>(body)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 5_000
        const val READ_TIMEOUT_MILLIS = 10_000
    }
}

class FeedbackNetworkException(message: String) : RuntimeException(message)
