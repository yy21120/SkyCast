package com.yy21120.skycast.ui

import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

internal fun formatDate(value: String): String = runCatching {
    java.time.LocalDate.parse(value).format(
        DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE),
    )
}.getOrDefault(value)

internal fun formatOpportunityTime(value: String, zoneId: ZoneId): String = try {
    OffsetDateTime.parse(value)
        .atZoneSameInstant(zoneId)
        .format(DateTimeFormatter.ofPattern("HH:mm"))
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

internal fun zoneIdOrDefault(value: String): ZoneId =
    runCatching { ZoneId.of(value) }.getOrDefault(ZoneId.systemDefault())

internal fun formatMetricNumber(value: Double): String =
    java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()

internal fun formatContribution(value: Double): String =
    "${if (value >= 0.0) "+" else ""}${formatMetricNumber(value)}"

internal fun formatBaselineValue(value: Double): String =
    String.format(Locale.ROOT, "%.3f", value)

internal fun validHttpUrl(value: String?): String? {
    val candidate = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
    val scheme = uri.scheme?.lowercase(Locale.ROOT)
    return candidate.takeIf {
        scheme in setOf("http", "https") && !uri.host.isNullOrBlank()
    }
}
