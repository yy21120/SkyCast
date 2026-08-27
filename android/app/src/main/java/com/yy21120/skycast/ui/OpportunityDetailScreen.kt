package com.yy21120.skycast.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yy21120.skycast.data.AssessmentFactor
import com.yy21120.skycast.data.OpportunityResult
import com.yy21120.skycast.data.SourceReference
import com.yy21120.skycast.data.SunsetOpportunity
import com.yy21120.skycast.data.SunsetOutcome
import java.time.ZoneId
import kotlin.math.abs

@Composable
internal fun OpportunityDetailScreen(
    result: OpportunityResult,
    opportunity: SunsetOpportunity?,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenSource: (String) -> Unit,
    feedbackState: FeedbackUiState = FeedbackUiState(),
    onFeedbackAction: (FeedbackAction) -> Unit = {},
) {
    if (opportunity == null) {
        MissingOpportunityContent(onBack)
        return
    }

    val response = result.response
    val cityZoneId = zoneIdOrDefault(response.city.timezone)
    val style = recommendationStyle(opportunity.recommendation)
    LaunchedEffect(opportunity.sceneId) {
        onFeedbackAction(FeedbackAction.SceneSelected(opportunity.sceneId))
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("opportunity-detail"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            TextButton(onClick = onBack) { Text("← 返回") }
            Text(
                text = "${response.city.name}晚霞评估详情",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "规则评分基线 · 非官方天气预报",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item { DataStatusBanner(result, onRetry) }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = style.containerColor),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = formatDate(opportunity.date),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = style.label,
                                color = style.accentColor,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Text(
                            text = opportunity.score.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = style.accentColor,
                        )
                    }
                    Text(opportunity.summary)
                }
            }
        }

        item {
            FeedbackSection(
                state = feedbackState,
                onAction = onFeedbackAction,
            )
        }

        item {
            DetailSection(title = "拍摄时间") {
                DetailValueRow(
                    label = "染色开始",
                    value = formatOpportunityTime(opportunity.coloringWindowStart, cityZoneId),
                )
                DetailValueRow(
                    label = "日落时刻",
                    value = formatOpportunityTime(opportunity.sunset, cityZoneId),
                )
                DetailValueRow(
                    label = "染色结束",
                    value = formatOpportunityTime(opportunity.coloringWindowEnd, cityZoneId),
                )
                DetailValueRow(label = "时区", value = "${response.city.timezone}（武汉时区）")
            }
        }

        item {
            DetailSection(title = "模型与可信度") {
                DetailValueRow(
                    label = "未校准规则基线值",
                    value = formatBaselineValue(opportunity.baselineProbability),
                )
                Text(
                    text = "该值仅用于规则排序，不代表事件发生的真实概率。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                DetailValueRow(
                    label = "概率状态",
                    value = probabilityStatusLabel(opportunity.probabilityStatus),
                )
                DetailValueRow(label = "置信度", value = confidenceLabel(opportunity.confidence))
                DetailValueRow(label = "模型版本", value = opportunity.modelVersion)
            }
        }

        item { SectionTitle("完整评分因子") }
        items(
            items = opportunity.factors.sortedByDescending { abs(it.contribution) },
            key = { it.code },
        ) { factor ->
            FactorDetailCard(factor)
        }

        item { SectionTitle("数据来源") }
        if (opportunity.sources.isEmpty()) {
            item { Text("暂无来源信息") }
        } else {
            items(items = opportunity.sources, key = { it.sourceId + it.sampledAt }) { source ->
                SourceCard(source, cityZoneId, onOpenSource)
            }
        }

        item {
            Text(
                text = "SkyCast 当前结果用于拍摄决策辅助，不替代气象部门发布的天气预报和预警。",
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun FeedbackSection(
    state: FeedbackUiState,
    onAction: (FeedbackAction) -> Unit,
) {
    when {
        state.submissionStatus is FeedbackSubmissionStatus.Success -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("反馈已提交", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("感谢你帮助 SkyCast 验证和改进晚霞评估。")
                }
            }
        }

        !state.formVisible -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("实际看到了怎样的晚霞？", fontWeight = FontWeight.Bold)
                    Text(
                        "记录现场结果，帮助后续验证评分并改进模型。",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = { onAction(FeedbackAction.ShowForm) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("记录实况")
                    }
                }
            }
        }

        else -> FeedbackForm(state, onAction)
    }
}

@Composable
private fun FeedbackForm(
    state: FeedbackUiState,
    onAction: (FeedbackAction) -> Unit,
) {
    val isSubmitting = state.submissionStatus is FeedbackSubmissionStatus.Submitting
    Card(
        modifier = Modifier.testTag("feedback-form"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("记录晚霞实况", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("晚霞表现", style = MaterialTheme.typography.labelLarge)
            FeedbackOutcomeChip(
                label = "明显晚霞",
                outcome = SunsetOutcome.VIVID,
                selected = state.outcome,
                enabled = !isSubmitting,
                onAction = onAction,
            )
            FeedbackOutcomeChip(
                label = "普通晚霞",
                outcome = SunsetOutcome.VISIBLE,
                selected = state.outcome,
                enabled = !isSubmitting,
                onAction = onAction,
            )
            FeedbackOutcomeChip(
                label = "未出现",
                outcome = SunsetOutcome.NOT_VISIBLE,
                selected = state.outcome,
                enabled = !isSubmitting,
                onAction = onAction,
            )

            Text("拍摄质量（1 低—5 高）", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                (1..5).forEach { quality ->
                    FilterChip(
                        selected = state.shootingQuality == quality,
                        onClick = { onAction(FeedbackAction.QualitySelected(quality)) },
                        label = { Text(quality.toString()) },
                        enabled = !isSubmitting,
                    )
                }
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = { onAction(FeedbackAction.NotesChanged(it)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
                label = { Text("现场备注（可选）") },
                supportingText = { Text("${state.notes.length}/200") },
                minLines = 3,
                maxLines = 5,
            )
            Text(
                "请勿填写姓名、手机号、精确位置或其他个人敏感信息。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val error = state.submissionStatus as? FeedbackSubmissionStatus.Error
            if (error != null) {
                Text(
                    error.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = { onAction(FeedbackAction.HideForm) },
                    enabled = !isSubmitting,
                ) {
                    Text("取消")
                }
                Button(
                    onClick = { onAction(FeedbackAction.Submit) },
                    enabled = state.canSubmit,
                ) {
                    Text(
                        when {
                            isSubmitting -> "提交中…"
                            error != null -> "重新提交"
                            else -> "提交反馈"
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackOutcomeChip(
    label: String,
    outcome: SunsetOutcome,
    selected: SunsetOutcome?,
    enabled: Boolean,
    onAction: (FeedbackAction) -> Unit,
) {
    FilterChip(
        selected = selected == outcome,
        onClick = { onAction(FeedbackAction.OutcomeSelected(outcome)) },
        label = { Text(label) },
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle(title)
            content()
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun DetailValueRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1.4f),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun FactorDetailCard(factor: AssessmentFactor) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(factor.label, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "贡献 ${formatContribution(factor.contribution)}",
                    color = factorColor(factor),
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = "观测值 ${formatMetricNumber(factor.value)}${factor.unit}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(factor.explanation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SourceCard(
    source: SourceReference,
    cityZoneId: ZoneId,
    onOpenSource: (String) -> Unit,
) {
    val validUrl = validHttpUrl(source.sourceUrl)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(source.sourceId, fontWeight = FontWeight.SemiBold)
            DetailValueRow("数据时刻", formatTimestamp(source.sampledAt, cityZoneId))
            DetailValueRow("获取时刻", formatTimestamp(source.retrievedAt, cityZoneId))
            if (validUrl == null) {
                Text(
                    text = "暂无可打开的公开来源链接",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                TextButton(onClick = { onOpenSource(validUrl) }) { Text("打开来源") }
            }
        }
    }
}

@Composable
private fun MissingOpportunityContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp)
            .testTag("missing-opportunity"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "评估不存在",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "这条机会可能已更新或从缓存中移除，请返回列表重新选择。",
            modifier = Modifier.padding(vertical = 14.dp),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onBack) { Text("返回机会列表") }
    }
}
