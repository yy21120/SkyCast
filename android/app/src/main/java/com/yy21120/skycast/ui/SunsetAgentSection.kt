package com.yy21120.skycast.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import com.yy21120.skycast.data.AssessmentFactor
import com.yy21120.skycast.data.OpportunityDataSource
import com.yy21120.skycast.data.OpportunityResult
import com.yy21120.skycast.data.SunsetOpportunity
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

private val NightPurple = Color(0xFF38244F)
private val Plum = Color(0xFF704463)
private val SunsetCoral = Color(0xFFE86F4D)
private val SunsetOrange = Color(0xFFFFA457)
private val SunsetYellow = Color(0xFFFFD77D)
private val GlassHighlight = Color.White.copy(alpha = 0.55f)
private val GlassModuleSurface = Color(0xFFF8EEF1)
private val UserMessageSurface = Color(0xFFFFE1D2)
private val BestMomentMint = Color(0xFF8EF0C8)
private val WuhanZone = ZoneId.of("Asia/Shanghai")

@Composable
internal fun SunsetAgentSection(
    modifier: Modifier = Modifier,
    result: OpportunityResult,
    opportunity: SunsetOpportunity,
    forecasts: List<SunsetOpportunity>,
    state: AgentUiState,
    onRetry: () -> Unit,
    onOpportunityClick: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onSpotSelected: (String) -> Unit,
) {
    val conversationActive = state.messages.size > 1 || state.isLoading || state.revealedModules.isNotEmpty()
    var heroExpanded by rememberSaveable(opportunity.sceneId) { mutableStateOf(!conversationActive) }
    LaunchedEffect(state.messages.size, state.revealedModules, state.isLoading) {
        if (conversationActive) heroExpanded = false
    }
    val heroCollapsed = !heroExpanded
    val heroWeight by animateFloatAsState(
        targetValue = if (heroCollapsed) 0.16f else 0.58f,
        animationSpec = tween(durationMillis = 420, easing = LinearEasing),
        label = "hero-weight",
    )

    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SunsetHeroCard(
            result = result,
            opportunity = opportunity,
            onRetry = onRetry,
            compact = heroCollapsed,
            onExpand = { heroExpanded = true },
            hasConversation = conversationActive,
            onCollapse = { heroExpanded = false },
            modifier = Modifier.weight(heroWeight),
        )
        AgentConversation(
            modifier = Modifier.weight(1f - heroWeight),
            state = state,
            opportunity = opportunity,
            forecasts = forecasts,
            onOpportunityClick = onOpportunityClick,
            onInputChange = onInputChange,
            onSend = onSend,
            onQuickPrompt = onQuickPrompt,
            onSpotSelected = onSpotSelected,
            heroCollapsed = heroCollapsed,
            onExpandHero = { heroExpanded = true },
        )
    }
}

@Composable
private fun SunsetHeroCard(
    result: OpportunityResult,
    opportunity: SunsetOpportunity,
    onRetry: () -> Unit,
    compact: Boolean,
    onExpand: () -> Unit,
    hasConversation: Boolean,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = recommendationStyle(opportunity.recommendation)
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var dragDirection by remember { mutableStateOf(0) }
    var armedDirection by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(compact) {
        armedDirection = null
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(compact, hasConversation) {
                detectVerticalDragGestures(
                    onDragStart = {
                        dragDistance = 0f
                        dragDirection = 0
                    },
                    onVerticalDrag = { _, dragAmount ->
                        val direction = if (dragAmount > 0f) 1 else -1
                        if (dragDirection == 0 || dragDirection == direction) {
                            dragDirection = direction
                            dragDistance += kotlin.math.abs(dragAmount)
                        } else {
                            dragDistance = 0f
                            dragDirection = direction
                        }
                    },
                    onDragEnd = {
                        val isExpandMainGesture = compact && dragDirection > 0
                        val isExpandAgentGesture = !compact && (
                            dragDirection < 0 || (!hasConversation && dragDirection > 0)
                        )
                        if (dragDistance >= 36.dp.toPx() && (isExpandMainGesture || isExpandAgentGesture)) {
                            if (armedDirection == dragDirection) {
                                if (isExpandMainGesture) onExpand() else onCollapse()
                                armedDirection = null
                            } else {
                                armedDirection = dragDirection
                            }
                        }
                        dragDistance = 0f
                        dragDirection = 0
                    },
                    onDragCancel = {
                        dragDistance = 0f
                        dragDirection = 0
                    },
                )
            }
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(NightPurple, Plum, SunsetCoral)))
            .testTag("sunset-hero-card"),
    ) {
        Crossfade(
            targetState = compact,
            modifier = Modifier.fillMaxSize(),
            animationSpec = tween(durationMillis = 220, easing = LinearEasing),
            label = "hero-content",
        ) { compactContent ->
            if (compactContent) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    SunPathVisualization(opportunity, compact = true)
                }
            } else {
                FullSunsetHeroContent(
                    result = result,
                    opportunity = opportunity,
                    style = style,
                    onRetry = onRetry,
                )
            }
        }
    }
}

@Composable
private fun FullSunsetHeroContent(
    result: OpportunityResult,
    opportunity: SunsetOpportunity,
    style: RecommendationStyle,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "SKYCAST · 逐光",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "AI WEATHER PHOTOGRAPHY",
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                Text(
                    style.label,
                    color = SunsetYellow,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Surface(
                color = Color.White.copy(alpha = 0.13f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = compactDataStatus(result),
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelSmall,
                    )
                    if (result.source != OpportunityDataSource.ONLINE) {
                        TextButton(onClick = onRetry) {
                            Text("重新获取", color = SunsetYellow)
                        }
                    }
                }
            }

            SunPathVisualization(opportunity, compact = false)

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${opportunity.score}%",
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "晚霞机会 · ${formatWindow(opportunity)}",
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            LinearProgressIndicator(
                progress = { opportunity.score / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = SunsetYellow,
                trackColor = Color.White.copy(alpha = 0.28f),
            )

            opportunity.factors.take(4).chunked(2).forEach { rowFactors ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowFactors.forEach { factor -> HeroFact(factor, Modifier.weight(1f)) }
                    if (rowFactors.size == 1) Box(Modifier.weight(1f))
                }
            }
    }
}

private fun compactDataStatus(result: OpportunityResult): String = when (result.source) {
    OpportunityDataSource.ONLINE -> "在线天气·${result.response.generatedAt.substringAfter('T').take(5)} 更新"
    OpportunityDataSource.CACHE -> if (result.isExpired) "缓存已过期·仅供参考" else "离线缓存·可重新获取"
    OpportunityDataSource.DEMO -> "离线演示数据·连接本地服务后更新"
}

@Composable
private fun SunPathVisualization(opportunity: SunsetOpportunity, compact: Boolean) {
    var now by remember(opportunity.sceneId) { mutableStateOf(ZonedDateTime.now(WuhanZone)) }
    LaunchedEffect(opportunity.sceneId) {
        while (true) {
            now = ZonedDateTime.now(WuhanZone)
            delay(60_000)
        }
    }

    val currentProgress = trajectoryProgress(now, opportunity)
    val bestMoment = bestShootingMoment(opportunity)
    val bestProgress = trajectoryProgress(bestMoment, opportunity)
    val bestWindowStart = trajectoryProgress(
        OffsetDateTime.parse(opportunity.coloringWindowStart).atZoneSameInstant(WuhanZone),
        opportunity,
    )
    val bestWindowEnd = trajectoryProgress(
        OffsetDateTime.parse(opportunity.coloringWindowEnd).atZoneSameInstant(WuhanZone),
        opportunity,
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 112.dp else 142.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF352D5C), Color(0xFF9F5264), SunsetOrange, SunsetYellow),
                ),
            )
            .testTag("sun-path-visualization"),
    ) {
        Canvas(modifier = Modifier.matchParentSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
            val left = 14.dp.toPx()
            val right = size.width - 14.dp.toPx()
            val horizon = size.height * 0.78f
            val arcHeight = size.height * 0.52f

            fun point(progress: Float): Offset {
                val normalized = progress.coerceIn(0f, 1f)
                return Offset(
                    x = left + (right - left) * normalized,
                    y = horizon - sin(PI.toFloat() * normalized) * arcHeight,
                )
            }

            val path = Path()
            repeat(49) { index ->
                val position = point(index / 48f)
                if (index == 0) path.moveTo(position.x, position.y) else path.lineTo(position.x, position.y)
            }
            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.68f),
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 6.dp.toPx())),
                ),
            )

            val bestArc = Path()
            repeat(17) { index ->
                val progress = bestWindowStart + (bestWindowEnd - bestWindowStart) * (index / 16f)
                val position = point(progress)
                if (index == 0) bestArc.moveTo(position.x, position.y) else bestArc.lineTo(position.x, position.y)
            }
            drawPath(
                path = bestArc,
                color = BestMomentMint,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )

            val bestPoint = point(bestProgress)
            drawCircle(BestMomentMint.copy(alpha = 0.22f), 16.dp.toPx(), bestPoint)
            drawCircle(Color.White.copy(alpha = 0.92f), 9.dp.toPx(), bestPoint, style = Stroke(1.5.dp.toPx()))
            drawCircle(BestMomentMint, 5.dp.toPx(), bestPoint)

            val sunPoint = point(currentProgress)
            drawCircle(SunsetYellow.copy(alpha = 0.20f), 25.dp.toPx(), sunPoint)
            drawCircle(Color(0xFFFFDA7D), 15.dp.toPx(), sunPoint)
            drawCircle(Color.White.copy(alpha = 0.34f), 15.dp.toPx(), sunPoint, style = Stroke(1.dp.toPx()))

            drawLine(
                color = Color.White.copy(alpha = 0.30f),
                start = Offset(left, horizon),
                end = Offset(right, horizon),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}

private fun trajectoryProgress(moment: ZonedDateTime, opportunity: SunsetOpportunity): Float {
    val sunset = OffsetDateTime.parse(opportunity.sunset).atZoneSameInstant(WuhanZone)
    val sunriseEstimate = sunset.toLocalDate().atTime(6, 0).atZone(WuhanZone)
    val end = OffsetDateTime.parse(opportunity.coloringWindowEnd).atZoneSameInstant(WuhanZone)
    val totalMinutes = Duration.between(sunriseEstimate, end).toMinutes().coerceAtLeast(1)
    val elapsedMinutes = Duration.between(sunriseEstimate, moment).toMinutes()
    return (elapsedMinutes.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f)
}

private fun bestShootingMoment(opportunity: SunsetOpportunity): ZonedDateTime {
    val start = OffsetDateTime.parse(opportunity.coloringWindowStart).atZoneSameInstant(WuhanZone)
    val end = OffsetDateTime.parse(opportunity.coloringWindowEnd).atZoneSameInstant(WuhanZone)
    return start.plusSeconds(Duration.between(start, end).seconds / 2)
}

@Composable
private fun FutureForecastCard(
    forecasts: List<SunsetOpportunity>,
    onOpportunityClick: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("agent-forecast-module"),
        colors = CardDefaults.cardColors(containerColor = GlassModuleSurface),
        border = BorderStroke(1.dp, GlassHighlight),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("未来几天晚霞", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "评分用于趋势排序，点击日期查看完整依据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            forecasts.take(3).forEach { forecast ->
                val style = recommendationStyle(forecast.recommendation)
                Card(
                    onClick = { onOpportunityClick(forecast.sceneId) },
                    modifier = Modifier.fillMaxWidth().testTag("opportunity-card-${forecast.sceneId}"),
                    colors = CardDefaults.cardColors(containerColor = style.containerColor),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(formatDate(forecast.date), fontWeight = FontWeight.SemiBold)
                            Text(
                                "${style.label}·${formatWindow(forecast)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = style.accentColor,
                            )
                        }
                        Text(
                            "${forecast.score}%",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = style.accentColor,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroFact(factor: AssessmentFactor, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(factor.label, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelMedium)
            Text(
                "${formatWeatherValue(factor.value)}${factor.unit}",
                color = if (factor.effect == "favorable") Color(0xFFA8F5B5) else SunsetYellow,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DetailedWeatherCard(opportunity: SunsetOpportunity) {
    Card(
        modifier = Modifier.testTag("agent-detail-module"),
        colors = CardDefaults.cardColors(containerColor = GlassModuleSurface),
        border = BorderStroke(1.dp, GlassHighlight),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("详细晚霞数据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(color = Color(0xFFE8CFC2))
            opportunity.factors.take(4).forEach { factor ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(factor.label, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${formatWeatherValue(factor.value)}${factor.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Text(
                "规则评分基线 · 置信度 ${confidenceLabel(opportunity.confidence)} · 非官方天气预报",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatWindow(opportunity: SunsetOpportunity): String =
    "${opportunity.coloringWindowStart.substringAfter('T').take(5)}–" +
        opportunity.coloringWindowEnd.substringAfter('T').take(5)

private fun formatWeatherValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

@Composable
private fun AgentConversation(
    modifier: Modifier = Modifier,
    state: AgentUiState,
    opportunity: SunsetOpportunity,
    forecasts: List<SunsetOpportunity>,
    onOpportunityClick: (String) -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onSpotSelected: (String) -> Unit,
    heroCollapsed: Boolean,
    onExpandHero: () -> Unit,
) {
    val conversationScroll = rememberScrollState()
    val latestMessageRequester = remember { BringIntoViewRequester() }
    val moduleRequester = remember { BringIntoViewRequester() }
    val expandThreshold = with(LocalDensity.current) { 36.dp.toPx() }
    var pullPastTop by remember { mutableFloatStateOf(0f) }
    var reachedTopDuringGesture by remember { mutableStateOf(false) }
    var topPullArmed by remember { mutableStateOf(false) }
    LaunchedEffect(heroCollapsed) {
        if (!heroCollapsed) topPullArmed = false
    }
    val expandOnPullConnection = remember(conversationScroll, heroCollapsed, expandThreshold) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (
                    heroCollapsed &&
                    source == NestedScrollSource.UserInput &&
                    conversationScroll.value == 0 &&
                    available.y > 0f
                ) {
                    pullPastTop += available.y
                }
                if (
                    heroCollapsed &&
                    source == NestedScrollSource.UserInput &&
                    conversationScroll.value == 0 &&
                    consumed.y > 0f
                ) {
                    reachedTopDuringGesture = true
                }
                if (conversationScroll.value > 0) {
                    topPullArmed = false
                    pullPastTop = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (heroCollapsed && (pullPastTop >= expandThreshold || reachedTopDuringGesture)) {
                    if (topPullArmed && pullPastTop >= expandThreshold) {
                        onExpandHero()
                        topPullArmed = false
                    } else {
                        topPullArmed = true
                    }
                }
                pullPastTop = 0f
                reachedTopDuringGesture = false
                return Velocity.Zero
            }
        }
    }
    LaunchedEffect(state.messages.lastOrNull()?.id) {
        delay(160)
        latestMessageRequester.bringIntoView()
    }
    LaunchedEffect(state.revealedModules) {
        if (state.revealedModules.isNotEmpty() && !state.isLoading) {
            delay(460)
            moduleRequester.bringIntoView()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 20.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color.White.copy(alpha = 0.34f),
                spotColor = NightPurple.copy(alpha = 0.26f),
            )
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.24f))
            .border(
                BorderStroke(
                    1.4.dp,
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.96f),
                            Color.White.copy(alpha = 0.42f),
                            NightPurple.copy(alpha = 0.34f),
                        ),
                    ),
                ),
                RoundedCornerShape(28.dp),
            )
            .testTag("agent-glass-panel"),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(3.dp)
                .border(
                    BorderStroke(
                        0.8.dp,
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.62f), Color.Transparent),
                        ),
                    ),
                    RoundedCornerShape(25.dp),
                ),
        )
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("晚霞摄影 Agent", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Surface(
                    color = Color.White.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(99.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.62f)),
                ) {
                    Text(
                        "LIVE",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = NightPurple,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Text(
                "问得越具体，越容易得到可操作的时间、数据和机位建议",
                style = MaterialTheme.typography.bodySmall,
                color = NightPurple.copy(alpha = 0.78f),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.72f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(expandOnPullConnection)
                    .verticalScroll(conversationScroll)
                    .testTag("agent-conversation-scroll"),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.messages.forEachIndexed { index, message ->
                    AgentMessageBubble(
                        message = message,
                        modifier = if (index == state.messages.lastIndex) {
                            Modifier.bringIntoViewRequester(latestMessageRequester)
                        } else {
                            Modifier
                        },
                    )
                }
                if (state.isLoading) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.38f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("  正在整理建议…", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(moduleRequester)
                        .testTag("agent-results"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AnimatedVisibility(
                        visible = AgentModule.FORECAST in state.revealedModules,
                        enter = moduleEnterTransition(),
                        exit = moduleExitTransition(),
                    ) {
                        FutureForecastCard(forecasts, onOpportunityClick)
                    }
                    AnimatedVisibility(
                        visible = AgentModule.DETAILS in state.revealedModules,
                        enter = moduleEnterTransition(),
                        exit = moduleExitTransition(),
                    ) {
                        DetailedWeatherCard(opportunity)
                    }
                    AnimatedVisibility(
                        visible = AgentModule.MAP in state.revealedModules,
                        enter = moduleEnterTransition(),
                        exit = moduleExitTransition(),
                    ) {
                        SunsetMapCard(
                            spots = state.spots,
                            selectedSpotId = state.selectedSpotId,
                            onSpotSelected = onSpotSelected,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("今天值得拍吗？", "帮我规划拍摄地图", "未来三天怎么样？", "几点出发？")
                    .forEach { prompt ->
                        AssistChip(
                            onClick = { onQuickPrompt(prompt) },
                            label = { Text(prompt) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = Color(0xFFF8ECEE),
                                labelColor = NightPurple,
                            ),
                            border = BorderStroke(1.dp, NightPurple.copy(alpha = 0.28f)),
                        )
                    }
            }
            OutlinedTextField(
                value = state.input,
                onValueChange = onInputChange,
                modifier = Modifier.fillMaxWidth().testTag("agent-input"),
                label = { Text("问问 SkyCast") },
                placeholder = { Text("例如：今天几点去东湖拍？") },
                minLines = 1,
                maxLines = 3,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF9EFF1),
                    unfocusedContainerColor = Color(0xFFF6E9EC),
                    focusedBorderColor = SunsetCoral,
                    unfocusedBorderColor = NightPurple.copy(alpha = 0.32f),
                ),
                trailingIcon = {
                    Button(
                        onClick = onSend,
                        enabled = state.input.isNotBlank() && !state.isLoading,
                        modifier = Modifier.testTag("agent-send"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NightPurple,
                            contentColor = Color.White,
                            disabledContainerColor = NightPurple.copy(alpha = 0.16f),
                        ),
                    ) { Text("发送") }
                },
            )
            Text(
                "结论来自 SkyCast 结构化天气数据；DeepSeek 只负责解释与摄影建议",
                style = MaterialTheme.typography.labelSmall,
                color = NightPurple.copy(alpha = 0.72f),
            )
        }
    }
}

private fun moduleEnterTransition() =
    fadeIn(tween(durationMillis = 280, easing = LinearEasing)) +
        slideInVertically(
            animationSpec = tween(durationMillis = 380, easing = LinearEasing),
            initialOffsetY = { height -> height / 2 },
        )

private fun moduleExitTransition() =
    fadeOut(tween(durationMillis = 180, easing = LinearEasing)) +
        slideOutVertically(
            animationSpec = tween(durationMillis = 220, easing = LinearEasing),
            targetOffsetY = { height -> height / 3 },
        )

@Composable
private fun AgentMessageBubble(message: AgentMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == AgentMessageRole.USER
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            color = if (isUser) UserMessageSurface else Color(0xFFF8EFF1),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.84f else 0.92f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.62f)),
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(message.content, style = MaterialTheme.typography.bodyMedium)
                message.sourceLabel?.let { sourceLabel ->
                    Text(sourceLabel, style = MaterialTheme.typography.labelSmall, color = NightPurple)
                }
            }
        }
    }
}
