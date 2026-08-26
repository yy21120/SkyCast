package com.yy21120.skycast.data

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class HttpOpportunityDataSource(
    baseUrl: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : OpportunityRemoteDataSource {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun fetchWuhanOpportunities(days: Int): OpportunitiesResponse =
        withContext(ioDispatcher) {
            require(days in 1..7) { "days must be between 1 and 7" }
            val connection = URL(
                "$normalizedBaseUrl/v1/cities/wuhan/opportunities?mode=live&days=$days",
            ).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.setRequestProperty("Accept", "application/json")

                val statusCode = connection.responseCode
                if (statusCode !in 200..299) {
                    throw OpportunityNetworkException("服务返回 HTTP $statusCode")
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString<OpportunitiesResponse>(body)
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 5_000
        const val READ_TIMEOUT_MILLIS = 10_000
    }
}

class OpportunityNetworkException(message: String) : RuntimeException(message)
