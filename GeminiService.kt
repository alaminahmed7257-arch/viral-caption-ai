package com.viralcaption.ai.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

data class GeneratedCaptionResponse(
    val title: String,
    val caption: String,
    val hashtags: List<String>,
    val hookAlternatives: List<String> = emptyList()
)

/**
 * Clean Gemini AI Service interface for Android
 */
interface GeminiCaptionService {
    suspend fun generateCaptions(
        description: String,
        videoName: String?,
        tone: String,
        targetPlatform: String
    ): GeneratedCaptionResponse
}

/**
 * Local generator implementation that processes the user's video description,
 * tone, and video context directly on the Android device without requiring
 * an external server or exposing secret API keys.
 */
class LocalGeminiDemoService : GeminiCaptionService {

    override suspend fun generateCaptions(
        description: String,
        videoName: String?,
        tone: String,
        targetPlatform: String
    ): GeneratedCaptionResponse = withContext(Dispatchers.Default) {
        // Simulate AI reasoning delay for realistic UX
        delay(1200)

        val cleanDesc = description.trim()
        val words = cleanDesc.split("\\s+".toRegex()).filter { it.length > 2 }
        val primarySubject = words.take(4).joinToString(" ").replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }.ifBlank { "This Secret Trick" }

        // Extract key topics for hashtags
        val extractedKeywords = words
            .map { it.lowercase().replace(Regex("[^a-z0-9]"), "") }
            .filter { it.length in 4..14 && it !in setOf("this", "that", "with", "from", "video", "about", "your", "make", "will", "have", "look", "what") }
            .distinct()

        val dynamicHashtags = mutableListOf<String>()
        // Add topic-specific hashtags from description
        for (kw in extractedKeywords.take(5)) {
            dynamicHashtags.add("#$kw")
        }

        // Platform specific tags
        val platformLower = targetPlatform.lowercase()
        val isTikTok = platformLower.contains("tiktok")
        val isInstagram = platformLower.contains("instagram")
        val isYouTube = platformLower.contains("youtube")
        val isFacebook = platformLower.contains("facebook")

        val platformTags = when {
            isTikTok -> listOf("#fyp", "#foryoupage", "#tiktokviral", "#tiktoktrending", "#xyzbca", "#learnontiktok", "#viralvideo")
            isInstagram -> listOf("#reelsinstagram", "#reelsviral", "#explorepage", "#instareels", "#creatorsofinstagram", "#trendingreels")
            isYouTube -> listOf("#shorts", "#youtubeshorts", "#ytshorts", "#viralshorts", "#youtubecreators", "#trending")
            isFacebook -> listOf("#facebookreels", "#fbwatch", "#viralpost", "#facebookcreators", "#trendingnow", "#fbreels")
            else -> listOf("#viral", "#trending", "#reels", "#shorts", "#fyp", "#explorepage")
        }

        val toneTags = when (tone) {
            "educational" -> listOf("#dailyhacks", "#protips", "#mindblowing", "#creatorguide")
            "funny", "humorous" -> listOf("#comedyvideos", "#relatable", "#laughoutloud", "#viralhumor")
            "aesthetic" -> listOf("#aestheticvibes", "#moodyvideo", "#visuals", "#cinematic")
            "storytelling" -> listOf("#storytime", "#truestory", "#pov", "#deepthoughts")
            "dramatic" -> listOf("#shocking", "#unexpected", "#plottwist")
            "inspiring" -> listOf("#motivation", "#mindset", "#growth", "#inspiration")
            else -> listOf("#viralreels", "#trendingnow", "#mustwatch", "#foryou")
        }

        for (tag in platformTags + toneTags) {
            if (!dynamicHashtags.contains(tag) && dynamicHashtags.size < 10) {
                dynamicHashtags.add(tag)
            }
        }

        // Generate platform-optimized title
        val title = when {
            isTikTok -> when (tone) {
                "educational" -> "Nobody is talking about this $primarySubject hack 🤯"
                "funny", "humorous" -> "Wait till the end of this $primarySubject 😂💀"
                "dramatic" -> "POV: You just discovered $primarySubject ⚡"
                "inspiring" -> "This will completely change how you see $primarySubject ✨"
                else -> "Secret $primarySubject you NEED to know! 🚀"
            }
            isInstagram -> when (tone) {
                "educational" -> "The Complete Guide to $primarySubject (Save for later!) 💡"
                "funny", "humorous" -> "I wasn't ready for this: $primarySubject 😭"
                "dramatic" -> "The Untold Truth About $primarySubject ⚡"
                "inspiring" -> "Golden Hour Glow: Mastering $primarySubject ✨"
                else -> "Stop Scrolling: The Ultimate $primarySubject Breakdown 🔥"
            }
            isYouTube -> when (tone) {
                "educational" -> "How To Master $primarySubject in 60 Seconds! (Easy Guide) 🚀 #Shorts"
                "funny", "humorous" -> "I Tried $primarySubject and THIS Happened... 😱 #Shorts"
                "dramatic" -> "DO NOT Make This $primarySubject Mistake ❌ #Shorts"
                "inspiring" -> "How $primarySubject Changed Everything (Step-by-Step) ✨ #Shorts"
                else -> "$primarySubject Explained in 30 Seconds! ⚡ #Shorts"
            }
            isFacebook -> when (tone) {
                "educational" -> "Everyone needs to know this simple trick for $primarySubject! 💡"
                "funny", "humorous" -> "This made my whole day: $primarySubject 😂"
                "dramatic" -> "You won't believe what happened next with $primarySubject! ⚡"
                "inspiring" -> "A heartwarming reminder about $primarySubject to brighten your day ❤️"
                else -> "Share this with someone who loves $primarySubject! 🔥"
            }
            else -> "Stop Scrolling! $primarySubject 🚀"
        }

        // Generate platform-optimized caption
        val caption = when {
            isTikTok -> "$cleanDesc\n\nDid you know this already? Drop a comment below 👇\n\n📌 Save this video or share with a friend who needs it!"
            isInstagram -> "$cleanDesc\n\n✨ Key takeaways you don't want to miss!\n\n📌 SAVE this reel for later\n↗️ SHARE with your circle\n💬 Drop your thoughts or questions in the comments!"
            isYouTube -> "$cleanDesc\n\n🔔 SUBSCRIBE for new daily videos & tutorials!\n👍 Smash that LIKE button if this helped you!\n💬 What topic should we cover next? Let me know below!"
            isFacebook -> "$cleanDesc\n\nWhat are your thoughts on this? Let's discuss in the comments! 💬\n\n👉 Tag a friend who needs to see this today!\n👍 Like and follow for more daily updates!"
            else -> "$cleanDesc\n\n🔥 Which part surprised you the most? Double tap & share with a friend!"
        }

        val hookAlternatives = when {
            isTikTok -> listOf(
                "TikTok made me try this $primarySubject...",
                "3 reasons why $primarySubject is going viral.",
                "Watch until the end if you want to know the secret."
            )
            isInstagram -> listOf(
                "Nobody is talking about this $primarySubject...",
                "Save this reel before it gets lost in your feed!",
                "3 simple steps to transform your $primarySubject."
            )
            isYouTube -> listOf(
                "Wait till you see what happens next!",
                "The biggest mistake people make with $primarySubject.",
                "Watch this before you start $primarySubject!"
            )
            else -> listOf(
                "You won't believe how simple this is...",
                "Share this with someone who needs this today!",
                "The real story behind $primarySubject."
            )
        }

        GeneratedCaptionResponse(
            title = title,
            caption = caption,
            hashtags = dynamicHashtags,
            hookAlternatives = hookAlternatives
        )
    }
}

object GeminiService {
    private val service: GeminiCaptionService = LocalGeminiDemoService()

    suspend fun generateCaptions(
        description: String,
        videoName: String?,
        tone: String,
        targetPlatform: String
    ): GeneratedCaptionResponse {
        return service.generateCaptions(description, videoName, tone, targetPlatform)
    }
}

