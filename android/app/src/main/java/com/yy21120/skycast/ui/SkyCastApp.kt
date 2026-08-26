package com.yy21120.skycast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yy21120.skycast.data.AssessmentFactor
import com.yy21120.skycast.data.OpportunityDataSource
import com.yy21120.skycast.data.OpportunityResult
import com.yy21120.skycast.data.SunsetOpportunity
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun SkyCastApp(viewModel: OpportunityViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Surface(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            OpportunityUiState.Loading -> LoadingContent()
            is OpportunityUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = viewModel::refresh,
            )
            is OpportunityUiState.Success -> OpportunityList(
                result = state.result,
                onRetry = viewModel::refresh,
            )
        }
    }
}

@Composable
private fun OpportunityList(
    result: OpportunityResult,
    onRetry: () -> Unit,
) {
    val response = result.response
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
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
            item {
                EmptyContent()
            }
        } else {
            items(items = response.opportunities, key = { it.sceneId }) { opportunity ->
                OpportunityCard(opportunity)
            }
        }
    }
}

@Composable
private fun DataStatusBanner(
    result: OpportunityResult,
    onRetry: () -> Unit,
) {
    val label: String
    val contentColor: Color
    val containerColor: Color
    val cityZoneId = zoneIdOrDefault(result.response.city.timezone)

    when (result.source) {
        OpportunityDataSource.ONLINE -> {
            label = "在线数据 · 更新于 ${formatTimestamp(result.response.generatedAt, cityZoneId)}"
            contentColor = Color(0xFF166534)
            containerColor = Color(0xFFEAF7EE)
        }
        OpportunityDataSource.CACHE -> {
            label = if (result.isExpired) {
                "缓存已过期，仅供参考 · 保存于 ${formatCacheTime(result.cachedAtEpochMillis, cityZoneId)}"
            } else {
                "离线缓存 · 保存于 ${formatCacheTime(result.cachedAtEpochMillis, cityZoneId)}"
            }
            contentColor = if (result.isExpired) Color(0xFF9A3412) else Color(0xFF6F5C00)
            containerColor = if (result.isExpired) Color(0xFFFFEDE5) else Color(0xFFFFF8D8)
        }
    }

    Surface(
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            if (result.source == OpportunityDataSource.CACHE) {
                TextButton(onClick = onRetry) {
                    Text("重新获取")
                }
            }
        }
    }
}

@Composable
private fun OpportunityCard(opportunity: SunsetOpportunity) {
    val style = recommendationStyle(opportunity.recommendation)
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                text = "染色时间 ${formatTime(opportunity.coloringWindowStart)}–${formatTime(opportunity.coloringWindowEnd)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = opportunity.summary,
                style = MaterialTheme.typography.bodyMedium,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            opportunity.factors
                .sortedByDescending { kotlin.math.abs(it.contribution) }
                .take(3)
                .forEach { factor -> FactorRow(factor) }

            Text(
                text = "置信度：${confidenceLabel(opportunity.confidence)} · 模型 ${opportunity.modelVersion}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FactorRow(factor: AssessmentFactor) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (factor.effect == "favorable") "+" else "−",
            color = if (factor.effect == "favorable") Color(0xFF167A52) else Color(0xFFB33A3A),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = factor.explanation,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("正在计算武汉晚霞机会…")
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "数据暂不可用",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = message,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onRetry) {
                Text("重新加载")
            }
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

private data class RecommendationStyle(
    val label: String,
    val accentColor: Color,
    val containerColor: Color,
)

private fun recommendationStyle(recommendation: String): RecommendationStyle =
    when (recommendation) {
        "go" -> RecommendationStyle("值得出发", Color(0xFFB54708), Color(0xFFFFF1E8))
        "watch" -> RecommendationStyle("持续关注", Color(0xFF6F5C00), Color(0xFFFFF8D8))
        else -> RecommendationStyle("暂不建议", Color(0xFF53606D), Color(0xFFF0F3F6))
    }

private fun confidenceLabel(confidence: String): String =
    when (confidence) {
        "high" -> "高"
        "medium" -> "中"
        else -> "低"
    }

internal fun formatDate(value: String): String = runCatching {
    java.time.LocalDate.parse(value).format(
        DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE),
    )
}.getOrDefault(value)

private fun formatTime(value: String): String = try {
    OffsetDateTime.parse(value).format(DateTimeFormatter.ofPattern("HH:mm"))
} catch (_: DateTimeParseException) {
    value
}

internal fun formatTimestamp(
    value: String,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String = try {
    OffsetDateTime.parse(value)
        .atZoneSameInstant(zoneId)
        .format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))
} catch (_: DateTimeParseException) {
    value
}

internal fun formatCacheTime(
    epochMillis: Long,
    zoneId: ZoneId = ZoneId.systemDefault(),
): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(zoneId)
        .format(DateTimeFormatter.ofPattern("M月d日 HH:mm"))

private fun zoneIdOrDefault(value: String): ZoneId =
    runCatching { ZoneId.of(value) }.getOrDefault(ZoneId.systemDefault())
