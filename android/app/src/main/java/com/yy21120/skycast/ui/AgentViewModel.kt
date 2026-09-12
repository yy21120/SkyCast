package com.yy21120.skycast.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yy21120.skycast.data.AgentChatMessage
import com.yy21120.skycast.data.AgentChatRequest
import com.yy21120.skycast.data.AgentRepository
import com.yy21120.skycast.data.HttpAgentRepository
import com.yy21120.skycast.data.ShootingSpot
import com.yy21120.skycast.data.SunsetOpportunity
import com.yy21120.skycast.data.defaultWuhanShootingSpots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AgentMessageRole {
    USER,
    ASSISTANT,
}

enum class AgentModule {
    FORECAST,
    DETAILS,
    MAP,
}

data class AgentMessage(
    val id: Long,
    val role: AgentMessageRole,
    val content: String,
    val isFallback: Boolean = false,
    val sourceLabel: String? = null,
)

data class AgentUiState(
    val input: String = "",
    val messages: List<AgentMessage> = listOf(
        AgentMessage(
            id = 0,
            role = AgentMessageRole.ASSISTANT,
            content = "你好，我是 SkyCast。问我未来几天的晚霞、几点出发，或让我规划拍摄地图。",
        ),
    ),
    val spots: List<ShootingSpot> = defaultWuhanShootingSpots(),
    val selectedSpotId: String? = null,
    val revealedModules: Set<AgentModule> = emptySet(),
    val isLoading: Boolean = false,
)

class AgentViewModel(
    private val repository: AgentRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()
    private var nextMessageId = 1L

    fun updateInput(value: String) {
        _uiState.value = _uiState.value.copy(input = value.take(500))
    }

    fun selectSpot(spotId: String) {
        _uiState.value = _uiState.value.copy(selectedSpotId = spotId)
    }

    fun ask(prompt: String, opportunity: SunsetOpportunity) {
        updateInput(prompt)
        send(opportunity)
    }

    fun send(opportunity: SunsetOpportunity) {
        val state = _uiState.value
        val message = state.input.trim()
        if (message.isEmpty() || state.isLoading) return

        val userMessage = AgentMessage(nextMessageId++, AgentMessageRole.USER, message)
        val previousMessages = state.messages
        val requestedModules = modulesFor(message)
        _uiState.value = state.copy(
            input = "",
            messages = previousMessages + userMessage,
            revealedModules = emptySet(),
            isLoading = true,
        )

        viewModelScope.launch {
            val assistantMessage = try {
                val history = previousMessages
                    .filter { it.id != 0L }
                    .takeLast(8)
                    .map {
                        AgentChatMessage(
                            role = if (it.role == AgentMessageRole.USER) "user" else "assistant",
                            content = it.content,
                        )
                    }
                val response = repository.chat(
                    AgentChatRequest(
                        message = message,
                        selectedSpotId = state.selectedSpotId,
                        history = history,
                    ),
                )
                if (response.recommendedSpots.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(spots = response.recommendedSpots)
                }
                AgentMessage(
                    id = nextMessageId++,
                    role = AgentMessageRole.ASSISTANT,
                    content = response.reply,
                    isFallback = response.fallback,
                    sourceLabel = if (response.fallback) {
                        "基础规则建议"
                    } else {
                        "DeepSeek 辅助解释"
                    },
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                AgentMessage(
                    id = nextMessageId++,
                    role = AgentMessageRole.ASSISTANT,
                    content = localFallbackReply(message, opportunity, state.selectedSpotId),
                    isFallback = true,
                    sourceLabel = "离线规则建议",
                )
            }
            _uiState.value = _uiState.value.copy(
                messages = _uiState.value.messages + assistantMessage,
                revealedModules = requestedModules,
                isLoading = false,
            )
        }
    }

    companion object {
        fun factory(baseUrl: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(AgentViewModel::class.java))
                    return AgentViewModel(HttpAgentRepository(baseUrl)) as T
                }
            }
    }
}

internal fun modulesFor(message: String): Set<AgentModule> {
    val modules = buildSet {
        if (listOf("未来", "后面", "明天", "后天", "三天", "3天", "几天", "趋势").any(message::contains)) {
            add(AgentModule.FORECAST)
        }
        if (listOf("地图", "地点", "哪里", "去哪", "机位", "导航").any(message::contains)) {
            add(AgentModule.MAP)
        }
        if (listOf("数据", "详细", "概率", "云量", "云层", "风向", "风速", "能见度").any(message::contains)) {
            add(AgentModule.DETAILS)
        }
    }
    return modules
}

internal fun localFallbackReply(
    message: String,
    opportunity: SunsetOpportunity,
    selectedSpotId: String?,
): String {
    val start = opportunity.coloringWindowStart.substringAfter('T').take(5)
    val end = opportunity.coloringWindowEnd.substringAfter('T').take(5)
    val selectedSpot = defaultWuhanShootingSpots().firstOrNull { it.id == selectedSpotId }
    val decision = when (opportunity.recommendation) {
        "go" -> "值得出发"
        "watch" -> "建议持续关注"
        else -> "暂不建议专程前往"
    }
    val base = "当前评分 ${opportunity.score}/100，$decision。预计染色时间 $start–$end。"
    return when {
        listOf("未来", "明天", "后天", "三天", "几天").any(message::contains) ->
            "$base 已在对话中展开未来几天趋势，点击日期可查看完整依据。"
        message.contains("哪里") || message.contains("地点") || message.contains("去哪") -> {
            val spot = selectedSpot ?: defaultWuhanShootingSpots().first()
            "$base 推荐${spot.name}，${spot.direction}。${spot.description}"
        }
        message.contains("器材") || message.contains("镜头") ->
            "$base 建议携带广角镜头、备用电池和小型三脚架，并注意保护高光。"
        else -> "$base ${opportunity.summary} 当前使用离线建议，联网后可获取 Agent 补充说明。"
    }
}
