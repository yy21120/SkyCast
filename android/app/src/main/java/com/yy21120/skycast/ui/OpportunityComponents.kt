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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yy21120.skycast.data.AssessmentFactor
import com.yy21120.skycast.data.OpportunityDataSource
import com.yy21120.skycast.data.OpportunityResult

@Composable
internal fun DataStatusBanner(
    result: OpportunityResult,
    onRetry: () -> Unit,
) {
    val cityZoneId = zoneIdOrDefault(result.response.city.timezone)
    val status = when (result.source) {
        OpportunityDataSource.ONLINE -> DataStatusStyle(
            label = "在线数据 · 更新于 ${formatTimestamp(result.response.generatedAt, cityZoneId)}",
            contentColor = Color(0xFF166534),
            containerColor = Color(0xFFEAF7EE),
        )
        OpportunityDataSource.CACHE -> if (result.isExpired) {
            DataStatusStyle(
                label = "缓存已过期，仅供参考 · 保存于 " +
                    formatCacheTime(result.cachedAtEpochMillis, cityZoneId),
                contentColor = Color(0xFF9A3412),
                containerColor = Color(0xFFFFEDE5),
            )
        } else {
            DataStatusStyle(
                label = "离线缓存 · 保存于 ${formatCacheTime(result.cachedAtEpochMillis, cityZoneId)}",
                contentColor = Color(0xFF6F5C00),
                containerColor = Color(0xFFFFF8D8),
            )
        }
    }

    Surface(
        color = status.containerColor,
        contentColor = status.contentColor,
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
                text = status.label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
            )
            if (result.source == OpportunityDataSource.CACHE) {
                TextButton(onClick = onRetry) { Text("重新获取") }
            }
        }
    }
}

@Composable
internal fun FactorSummaryRow(factor: AssessmentFactor) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = if (factor.contribution >= 0.0) "+" else "−",
            color = factorColor(factor),
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
internal fun LoadingContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(12.dp))
            Text("正在计算武汉晚霞机会…")
        }
    }
}

@Composable
internal fun ErrorContent(message: String, onRetry: () -> Unit) {
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
            Button(onClick = onRetry) { Text("重新加载") }
        }
    }
}

internal data class RecommendationStyle(
    val label: String,
    val accentColor: Color,
    val containerColor: Color,
)

internal fun recommendationStyle(recommendation: String): RecommendationStyle =
    when (recommendation) {
        "go" -> RecommendationStyle("值得出发", Color(0xFFB54708), Color(0xFFFFF1E8))
        "watch" -> RecommendationStyle("持续关注", Color(0xFF6F5C00), Color(0xFFFFF8D8))
        else -> RecommendationStyle("暂不建议", Color(0xFF53606D), Color(0xFFF0F3F6))
    }

internal fun confidenceLabel(confidence: String): String =
    when (confidence) {
        "high" -> "高"
        "medium" -> "中"
        else -> "低"
    }

internal fun probabilityStatusLabel(status: String): String =
    when (status) {
        "uncalibrated_baseline" -> "未校准规则基线（uncalibrated_baseline）"
        else -> status
    }

internal fun factorColor(factor: AssessmentFactor): Color =
    if (factor.contribution >= 0.0) Color(0xFF167A52) else Color(0xFFB33A3A)

private data class DataStatusStyle(
    val label: String,
    val contentColor: Color,
    val containerColor: Color,
)
