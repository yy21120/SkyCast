package com.yy21120.skycast.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AgentChatMessage(
    val role: String,
    val content: String,
)

@Serializable
data class AgentChatRequest(
    @SerialName("city_id") val cityId: String = "wuhan",
    val message: String,
    @SerialName("selected_spot_id") val selectedSpotId: String? = null,
    val history: List<AgentChatMessage> = emptyList(),
    val mode: String = "live",
)

@Serializable
data class ShootingSpot(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val direction: String,
    val description: String,
)

@Serializable
data class AgentChatResponse(
    val reply: String,
    @SerialName("scene_id") val sceneId: String,
    val score: Int,
    val confidence: String,
    @SerialName("recommended_spots") val recommendedSpots: List<ShootingSpot>,
    @SerialName("used_tools") val usedTools: List<String>,
    val provider: String,
    val model: String,
    @SerialName("generated_at") val generatedAt: String,
    @SerialName("safety_notice") val safetyNotice: String,
    val fallback: Boolean = false,
)

fun defaultWuhanShootingSpots(): List<ShootingSpot> = listOf(
    ShootingSpot(
        id = "east-lake-lingbo-gate",
        name = "东湖凌波门",
        latitude = 30.5436,
        longitude = 114.3661,
        direction = "向西拍摄湖面与城市天际线",
        description = "水面开阔，适合拍摄晚霞倒影。",
    ),
    ShootingSpot(
        id = "hankou-river-beach",
        name = "汉口江滩",
        latitude = 30.5915,
        longitude = 114.3008,
        direction = "沿江向西南取景",
        description = "江面和桥梁元素丰富，注意临江安全。",
    ),
    ShootingSpot(
        id = "shahu-park",
        name = "沙湖公园",
        latitude = 30.5718,
        longitude = 114.3387,
        direction = "向西拍摄湖面与建筑剪影",
        description = "交通便利，适合轻量化拍摄。",
    ),
)
