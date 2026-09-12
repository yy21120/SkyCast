package com.yy21120.skycast.data

interface AgentRepository {
    suspend fun chat(request: AgentChatRequest): AgentChatResponse
}

class AgentNetworkException(message: String) : RuntimeException(message)
