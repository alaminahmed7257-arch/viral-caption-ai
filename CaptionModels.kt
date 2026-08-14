package com.viralcaption.ai.model

data class CaptionResult(
    val id: String,
    val title: String,
    val caption: String,
    val hashtags: List<String>,
    val hookAlternatives: List<String> = emptyList(),
    val videoName: String? = null,
    val videoUriString: String? = null,
    val description: String = "",
    val tone: String = "viral",
    val targetPlatform: String = "All Platforms",
    val createdAt: Long = System.currentTimeMillis()
)

data class VideoItem(
    val uriString: String,
    val filename: String,
    val sizeFormatted: String,
    val durationFormatted: String? = null
)
