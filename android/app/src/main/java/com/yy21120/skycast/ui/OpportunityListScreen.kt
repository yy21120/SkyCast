package com.yy21120.skycast.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.yy21120.skycast.data.OpportunityResult

@Composable
internal fun OpportunityListScreen(
    result: OpportunityResult,
    onRetry: () -> Unit,
    onOpportunityClick: (String) -> Unit,
    agentState: AgentUiState = AgentUiState(),
    onAgentInputChange: (String) -> Unit = {},
    onAgentSend: () -> Unit = {},
    onAgentQuickPrompt: (String) -> Unit = {},
    onSpotSelected: (String) -> Unit = {},
) {
    val opportunities = result.response.opportunities
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF312044),
                        Color(0xFF9A4E5D),
                        Color(0xFFF18A5B),
                        Color(0xFFFFF5EA),
                    ),
                ),
            )
            .testTag("opportunity-list"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        opportunities.firstOrNull()?.let { opportunity ->
            item {
                SunsetAgentSection(
                    modifier = Modifier.fillParentMaxHeight(),
                    result = result,
                    opportunity = opportunity,
                    forecasts = opportunities,
                    state = agentState,
                    onRetry = onRetry,
                    onOpportunityClick = onOpportunityClick,
                    onInputChange = onAgentInputChange,
                    onSend = onAgentSend,
                    onQuickPrompt = onAgentQuickPrompt,
                    onSpotSelected = onSpotSelected,
                )
            }
        } ?: item { EmptyContent() }
    }
}

@Composable
private fun EmptyContent() {
    Text(
        text = "未来三天暂无可用评估，请稍后再试。",
        color = Color.White,
    )
}
