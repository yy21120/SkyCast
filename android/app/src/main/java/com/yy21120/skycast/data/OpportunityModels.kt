package com.yy21120.skycast.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpportunitiesResponse(
    val city: City,
    @SerialName("scene_type") val sceneType: String,
    val mode: String,
    @SerialName("generated_at") val generatedAt: String,
    val opportunities: List<SunsetOpportunity>,
)

@Serializable
data class City(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
)

@Serializable
data class SunsetOpportunity(
    @SerialName("scene_id") val sceneId: String,
    val date: String,
    val sunset: String,
    @SerialName("coloring_window_start") val coloringWindowStart: String,
    @SerialName("coloring_window_end") val coloringWindowEnd: String,
    val score: Int,
    @SerialName("baseline_probability") val baselineProbability: Double,
    @SerialName("probability_status") val probabilityStatus: String,
    val confidence: String,
    val recommendation: String,
    val summary: String,
    val factors: List<AssessmentFactor>,
    val sources: List<SourceReference>,
    @SerialName("model_version") val modelVersion: String,
)

@Serializable
data class AssessmentFactor(
    val code: String,
    val label: String,
    val value: Double,
    val unit: String,
    val contribution: Double,
    val effect: String,
    val explanation: String,
)

@Serializable
data class SourceReference(
    @SerialName("source_id") val sourceId: String,
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("sampled_at") val sampledAt: String,
    @SerialName("retrieved_at") val retrievedAt: String,
)
