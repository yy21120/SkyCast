package com.yy21120.skycast.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SunsetOutcome {
    @SerialName("vivid")
    VIVID,

    @SerialName("visible")
    VISIBLE,

    @SerialName("not_visible")
    NOT_VISIBLE,
}

@Serializable
data class SunsetFeedbackRequest(
    @SerialName("client_feedback_id") val clientFeedbackId: String,
    @SerialName("scene_id") val sceneId: String,
    val outcome: SunsetOutcome,
    @SerialName("shooting_quality") val shootingQuality: Int,
    val notes: String? = null,
    @SerialName("submitted_at") val submittedAt: String,
)

@Serializable
data class SunsetFeedbackRecord(
    @SerialName("client_feedback_id") val clientFeedbackId: String,
    @SerialName("scene_id") val sceneId: String,
    val outcome: SunsetOutcome,
    @SerialName("shooting_quality") val shootingQuality: Int,
    val notes: String? = null,
    @SerialName("submitted_at") val submittedAt: String,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
data class SunsetFeedbackResponse(
    val status: String,
    val duplicate: Boolean,
    val feedback: SunsetFeedbackRecord,
)
