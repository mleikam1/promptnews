package com.digitalturbine.promptnews.util

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

object TimeLabelFormatter {
    private const val popularLabel = "Popular"
    private const val millisThresholdForSeconds = 1_000_000_000_000L
    private val relativePattern = Regex(
        """^\s*(\d+)\s*(minute|minutes|hour|hours)\s*ago\s*$""",
        RegexOption.IGNORE_CASE
    )

    fun formatTimeLabel(
        publishedAt: String?,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        if (publishedAt.isNullOrBlank()) return popularLabel
        val trimmed = publishedAt.trim()

        val relativeMatch = relativePattern.matchEntire(trimmed)
        if (relativeMatch != null) {
            val amount = relativeMatch.groupValues[1].toLongOrNull() ?: return popularLabel
            val unit = relativeMatch.groupValues[2].lowercase()
            val instant = when {
                unit.startsWith("hour") -> now.minus(Duration.ofHours(amount))
                else -> now.minus(Duration.ofMinutes(amount))
            }
            return formatTimeLabel(instant, now, zoneId)
        }

        trimmed.toLongOrNull()?.let { epoch ->
            return formatTimeLabel(epochToMillis(epoch), now, zoneId)
        }

        val instant = parseInstant(trimmed) ?: return popularLabel
        return formatTimeLabel(instant, now, zoneId)
    }

    fun formatTimeLabel(
        publishedAtMs: Long?,
        now: Instant = Instant.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): String {
        if (publishedAtMs == null) return popularLabel
        return formatTimeLabel(Instant.ofEpochMilli(epochToMillis(publishedAtMs)), now, zoneId)
    }

    private fun formatTimeLabel(publishedAt: Instant, now: Instant, zoneId: ZoneId): String {
        val nowZoned = now.atZone(zoneId)
        val publishedZoned = publishedAt.atZone(zoneId)
        // If the timestamp is on the user's local calendar day, show recency; otherwise show Popular.
        if (publishedZoned.toLocalDate() != nowZoned.toLocalDate()) return popularLabel

        val minutes = Duration.between(publishedAt, now).toMinutes().coerceAtLeast(0)
        return if (minutes < 60) {
            "$minutes minutes ago"
        } else {
            "${minutes / 60} hours ago"
        }
    }

    private fun parseInstant(raw: String): Instant? {
        return runCatching { Instant.parse(raw) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(raw, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant() }
                .getOrNull()
            ?: runCatching { ZonedDateTime.parse(raw, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant() }
                .getOrNull()
    }

    private fun epochToMillis(value: Long): Long =
        if (value < millisThresholdForSeconds) value * 1000 else value
}
