package com.yy21120.skycast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yy21120.skycast.data.OpportunityResult
import com.yy21120.skycast.data.SunsetOpportunity
import java.time.ZoneId
import kotlin.math.abs

@Composable
internal fun OpportunityListScreen(
    result: OpportunityResult,
    onRetry: () -> Unit,
    onOpportunityClick: (String) -> Unit,
) {
    val response = result.response
    val cityZoneId = zoneIdOrDefault(response.city.timezone)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Text(
                text = "${response.city.name}晚霞机会",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "未来三天 · 规则评分基线 · 非官方天气预报",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            DataStatusBanner(result, onRetry)
        }

        if (response.opportunities.isEmpty()) {
            item { EmptyContent() }
        } else {
            items(items = response.opportunities, key = { it.sceneId }) { opportunity ->
                OpportunityCard(
                    opportunity = opportunity,
                    cityZoneId = cityZoneId,
                    onClick = { onOpportunityClick(opportunity.sceneId) },
                )
            }
        }
    }
}

@Composable
private fun OpportunityCard(
    opportunity: SunsetOpportunity,
    cityZoneId: ZoneId,
    onClick: () -> Unit,
) {
    val style = recommendationStyle(opportunity.recommendation)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("opportunity-card-${opportunity.sceneId}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = style.containerColor),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = formatDate(opportunity.date),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = style.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = style.accentColor,
                    )
                }
                Text(
                    text = opportunity.score.toString(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = style.accentColor,
                )
            }

            Text(
                text = "染色时间 ${formatOpportunityTime(opportunity.coloringWindowStart, cityZoneId)}–" +
                    formatOpportunityTime(opportunity.coloringWindowEnd, cityZoneId),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(text = opportunity.summary, style = MaterialTheme.typography.bodyMedium)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            opportunity.factors
                .sortedByDescending { abs(it.contribution) }
                .take(3)
                .forEach { factor -> FactorSummaryRow(factor) }

            Text(
                text = "置信度：${confidenceLabel(opportunity.confidence)} · 模型 ${opportunity.modelVersion}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "查看完整评估依据 →",
                style = MaterialTheme.typography.labelLarge,
                color = style.accentColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun EmptyContent() {
    Text(
        text = "未来三天暂无可用评估，请稍后再试。",
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
