package com.viralcaption.ai.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.viralcaption.ai.data.CaptionRepository
import com.viralcaption.ai.data.GeminiService
import com.viralcaption.ai.model.CaptionResult
import com.viralcaption.ai.model.VideoItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViralCaptionScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Generator, 1 = History, 2 = Pro
    var videoItem by remember { mutableStateOf<VideoItem?>(null) }
    var description by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf("viral") }
    var targetPlatform by remember { mutableStateOf("TikTok") }

    var isGenerating by remember { mutableStateOf(false) }
    var resultTitle by remember { mutableStateOf("") }
    var resultCaption by remember { mutableStateOf("") }
    var resultHashtags by remember { mutableStateOf<List<String>>(emptyList()) }
    var currentResultId by remember { mutableStateOf<String?>(null) }
    var isSaved by remember { mutableStateOf(false) }

    var historyItems by remember { mutableStateOf<List<CaptionResult>>(emptyList()) }
    var remainingGenerations by remember { mutableStateOf(CaptionRepository.DAILY_FREE_LIMIT) }

    // Load initial history and daily usage
    LaunchedEffect(Unit) {
        historyItems = CaptionRepository.getHistory(context)
        remainingGenerations = CaptionRepository.getRemainingDailyGenerations(context)
    }

    // Android Native Video Picker Launcher (Contract for picking visual media / video)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "Selected video"
            var fileSize = "Video attached"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                    if (sizeIndex != -1) {
                        val bytes = cursor.getLong(sizeIndex)
                        fileSize = String.format("%.1f MB", bytes / (1024f * 1024f))
                    }
                }
            }
            videoItem = VideoItem(
                uriString = uri.toString(),
                filename = fileName,
                sizeFormatted = fileSize
            )
            Toast.makeText(context, "Selected: $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyToClipboard(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied $label to clipboard!", Toast.LENGTH_SHORT).show()
    }

    fun shareNative(title: String, caption: String, hashtags: List<String>) {
        val fullContent = "$title\n\n$caption\n\n${hashtags.joinToString(" ")}"
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, fullContent)
            putExtra(Intent.EXTRA_TITLE, title)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Caption via")
        context.startActivity(shareIntent)
    }

    Scaffold(
        containerColor = Color(0xFF09090B),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Viral Caption AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    // Daily limit badge & Go Pro button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (remainingGenerations == 0) Color(0xFF451A1A) else Color(0xFF27272A),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (remainingGenerations == 0) Color(0xFFEF4444) else Color(0xFF3F3F46)
                            ),
                            modifier = Modifier.clickable {
                                selectedTab = 2
                            }
                        ) {
                            Text(
                                text = "$remainingGenerations/${CaptionRepository.DAILY_FREE_LIMIT} left",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingGenerations == 0) Color(0xFFFCA5A5) else Color(0xFFD4D4D8),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = { selectedTab = 2 },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFF09090B),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "PRO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF09090B)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF18181B)
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF18181B)) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Generator") },
                    label = { Text("Generator") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF27272A),
                        unselectedIconColor = Color(0xFF71717A),
                        unselectedTextColor = Color(0xFF71717A)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        historyItems = CaptionRepository.getHistory(context)
                    },
                    icon = {
                        BadgedBox(badge = {
                            if (historyItems.isNotEmpty()) {
                                Badge(containerColor = Color(0xFF2563EB)) {
                                    Text("${historyItems.size}")
                                }
                            }
                        }) {
                            Icon(Icons.Default.History, contentDescription = "History")
                        }
                    },
                    label = { Text("History") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = Color(0xFF27272A),
                        unselectedIconColor = Color(0xFF71717A),
                        unselectedTextColor = Color(0xFF71717A)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = "Pro") },
                    label = { Text("Go Pro") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFFF59E0B),
                        selectedTextColor = Color(0xFFF59E0B),
                        indicatorColor = Color(0xFF451A03),
                        unselectedIconColor = Color(0xFF71717A),
                        unselectedTextColor = Color(0xFF71717A)
                    )
                )
            }
        }
    ) { innerPadding ->
        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Video Picker Card
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "VIDEO INPUT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF71717A),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Select clip from phone gallery",
                                fontSize = 13.sp,
                                color = Color(0xFFD4D4D8)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (videoItem != null) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF09090B), RoundedCornerShape(16.dp))
                                        .border(1.dp, Color(0xFF27272A), RoundedCornerShape(16.dp))
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Videocam,
                                        contentDescription = "Video",
                                        tint = Color(0xFF3B82F6),
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = videoItem!!.filename,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = videoItem!!.sizeFormatted,
                                            color = Color(0xFF71717A),
                                            fontSize = 12.sp
                                        )
                                    }
                                    IconButton(onClick = { videoItem = null }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = Color(0xFFA1A1AA)
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { videoPickerLauncher.launch("video/*") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27272A))
                                ) {
                                    Icon(
                                        Icons.Default.Upload,
                                        contentDescription = "Upload",
                                        tint = Color(0xFF3B82F6)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Select from gallery", color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                // Description Input Card
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "VIDEO CONTEXT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF71717A),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Describe what's happening",
                                fontSize = 13.sp,
                                color = Color(0xFFD4D4D8)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                placeholder = { Text("What is happening in this video? Gemini will use this to generate viral content.") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF3B82F6),
                                    unfocusedBorderColor = Color(0xFF27272A),
                                    focusedContainerColor = Color(0xFF09090B),
                                    unfocusedContainerColor = Color(0xFF09090B),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Platform Selection
                            Text(
                                text = "TARGET PLATFORM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF71717A),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val platforms = listOf(
                                Triple("TikTok", "🎵 TikTok", "FYP & Viral Hook"),
                                Triple("Instagram", "📸 Instagram", "Reels & Aesthetic"),
                                Triple("YouTube", "▶️ YouTube", "High CTR #Shorts"),
                                Triple("Facebook", "👥 Facebook", "Watch & Shares")
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                platforms.take(2).forEach { (id, label, sub) ->
                                    val isSelected = targetPlatform.contains(id, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) Color(0xFF1E293B) else Color(0xFF09090B),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF3B82F6) else Color(0xFF27272A),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable { targetPlatform = id }
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = label,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color(0xFFA1A1AA)
                                            )
                                            Text(
                                                text = sub,
                                                fontSize = 10.sp,
                                                color = Color(0xFF71717A)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                platforms.drop(2).forEach { (id, label, sub) ->
                                    val isSelected = targetPlatform.contains(id, ignoreCase = true)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (isSelected) Color(0xFF1E293B) else Color(0xFF09090B),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF3B82F6) else Color(0xFF27272A),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .clickable { targetPlatform = id }
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = label,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color(0xFFA1A1AA)
                                            )
                                            Text(
                                                text = sub,
                                                fontSize = 10.sp,
                                                color = Color(0xFF71717A)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Tone Selection
                            Text(
                                text = "VIBE / TONE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF71717A),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val toneOptions = listOf(
                                "viral" to "Viral 🔥",
                                "funny" to "Funny 😂",
                                "educational" to "Tips 💡",
                                "dramatic" to "Dramatic ⚡",
                                "inspiring" to "Inspiring ✨"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                toneOptions.forEach { (id, label) ->
                                    val isSelected = tone == id
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) Color(0xFF27272A) else Color(0xFF09090B),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) Color(0xFF52525B) else Color(0xFF27272A),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { tone = id }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.White else Color(0xFFA1A1AA)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (remainingGenerations == 0) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFF271515),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Free Daily Limit Reached (0/5)", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Resets in 24 hours. Unlock Pro for unlimited generations.", color = Color(0xFFA1A1AA), fontSize = 11.sp)
                                        }
                                        Button(
                                            onClick = { selectedTab = 2 },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("Go Pro", fontSize = 11.sp, color = Color(0xFF09090B), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (description.isBlank()) {
                                        Toast.makeText(context, "Please enter a description", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!CaptionRepository.canGenerate(context)) {
                                        Toast.makeText(context, "Daily limit reached (5/5). Upgrade to Pro or wait 24h.", Toast.LENGTH_LONG).show()
                                        selectedTab = 2
                                        return@Button
                                    }
                                    isGenerating = true
                                    scope.launch {
                                        try {
                                            val response = GeminiService.generateCaptions(
                                                description = description,
                                                videoName = videoItem?.filename ?: "Selected video",
                                                tone = tone,
                                                targetPlatform = targetPlatform
                                            )
                                            resultTitle = response.title
                                            resultCaption = response.caption
                                            resultHashtags = response.hashtags
                                            currentResultId = "res_${System.currentTimeMillis()}"
                                            isSaved = false
                                            remainingGenerations = CaptionRepository.incrementDailyUsage(context)
                                            Toast.makeText(context, "Generated! $remainingGenerations free generations left today.", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isGenerating = false
                                        }
                                    }
                                },
                                enabled = !isGenerating && description.isNotBlank(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (remainingGenerations == 0) Color(0xFFD97706) else Color(0xFF2563EB),
                                    disabledContainerColor = Color(0xFF27272A)
                                )
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generating with Gemini AI...")
                                } else if (remainingGenerations == 0) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Daily Limit Reached — View Pro", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generate with Gemini AI", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Generated Results (Editable + Copy/Save/Share)
                if (resultTitle.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Generated Results",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Row {
                                        IconButton(onClick = {
                                            shareNative(resultTitle, resultCaption, resultHashtags)
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                                        }
                                        IconButton(onClick = {
                                            val item = CaptionResult(
                                                id = currentResultId ?: "res_${System.currentTimeMillis()}",
                                                title = resultTitle,
                                                caption = resultCaption,
                                                hashtags = resultHashtags,
                                                videoName = videoItem?.filename,
                                                description = description,
                                                tone = tone,
                                                targetPlatform = targetPlatform
                                            )
                                            CaptionRepository.saveItem(context, item)
                                            isSaved = true
                                            Toast.makeText(context, "Saved to history!", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(
                                                if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = "Save",
                                                tint = if (isSaved) Color(0xFF10B981) else Color.White
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Editable Title
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("SUGGESTED TITLE", fontSize = 11.sp, color = Color(0xFF71717A), fontWeight = FontWeight.Bold)
                                    Text(
                                        "Copy",
                                        color = Color(0xFF3B82F6),
                                        fontSize = 12.sp,
                                        modifier = Modifier.clickable { copyToClipboard("Title", resultTitle) }
                                    )
                                }
                                OutlinedTextField(
                                    value = resultTitle,
                                    onValueChange = { resultTitle = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF09090B),
                                        unfocusedContainerColor = Color(0xFF09090B),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Editable Caption
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("SHORT CAPTION", fontSize = 11.sp, color = Color(0xFF71717A), fontWeight = FontWeight.Bold)
                                    Text(
                                        "Copy",
                                        color = Color(0xFF3B82F6),
                                        fontSize = 12.sp,
                                        modifier = Modifier.clickable { copyToClipboard("Caption", resultCaption) }
                                    )
                                }
                                OutlinedTextField(
                                    value = resultCaption,
                                    onValueChange = { resultCaption = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF09090B),
                                        unfocusedContainerColor = Color(0xFF09090B),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    )
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Hashtags
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("10 HASHTAGS", fontSize = 11.sp, color = Color(0xFF71717A), fontWeight = FontWeight.Bold)
                                    Text(
                                        "Copy All",
                                        color = Color(0xFF3B82F6),
                                        fontSize = 12.sp,
                                        modifier = Modifier.clickable {
                                            copyToClipboard("Hashtags", resultHashtags.joinToString(" "))
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(resultHashtags) { tag ->
                                        Box(
                                            modifier = Modifier
                                                .background(Color(0xFF27272A), CircleShape)
                                                .clickable { copyToClipboard("Hashtag", tag) }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(tag, color = Color(0xFFD4D4D8), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (selectedTab == 1) {
            // History Tab
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (historyItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No saved captions yet.", color = Color(0xFF71717A))
                        }
                    }
                } else {
                    items(historyItems, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.videoName ?: item.title,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row {
                                        IconButton(onClick = {
                                            shareNative(item.title, item.caption, item.hashtags)
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFFA1A1AA))
                                        }
                                        IconButton(onClick = {
                                            val updated = CaptionRepository.deleteItem(context, item.id)
                                            historyItems = updated
                                            Toast.makeText(context, "Deleted item", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444))
                                        }
                                    }
                                }
                                Text(item.title, color = Color(0xFF93C5FD), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(item.caption, color = Color(0xFFD4D4D8), fontSize = 12.sp, maxLines = 3)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    item.hashtags.joinToString(" "),
                                    color = Color(0xFF71717A),
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Pro Monetization Screen (Tab 2)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Header Banner
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B18)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFF59E0B).copy(alpha = 0.2f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.WorkspacePremium,
                                    contentDescription = "Pro",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Viral Caption AI Pro",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Level up your creator workflow with unmetered AI power",
                                fontSize = 12.sp,
                                color = Color(0xFFA1A1AA),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    // Current Free Plan Status
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CURRENT PLAN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF71717A))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF27272A)
                                ) {
                                    Text(
                                        "Free Tier Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF93C5FD),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Daily AI Generations", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("Refreshes every 24 hours automatically", fontSize = 11.sp, color = Color(0xFF71717A))
                                }
                                Text(
                                    text = "$remainingGenerations / ${CaptionRepository.DAILY_FREE_LIMIT}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (remainingGenerations > 0) Color(0xFF3B82F6) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }

                item {
                    // Pro Features List
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF27272A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("PRO INCLUDES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF71717A))
                            Spacer(modifier = Modifier.height(12.dp))

                            listOf(
                                "⚡ Unlimited AI Generations (No 5/day limit)" to "Generate as many captions as you need",
                                "🚫 100% Ad-Free Experience" to "Clean, uninterrupted creator workspace",
                                "🔥 Advanced Title Styles & High-CTR Hooks" to "10+ viral frameworks (Curiosity, Urgency, Story)",
                                "🎯 Platform-Specific Algorithms" to "Tuned for TikTok, Instagram Reels, YouTube Shorts, & Facebook Watch"
                            ).forEach { (title, subtitle) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(subtitle, fontSize = 11.sp, color = Color(0xFF71717A))
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    // Pricing and Purchase Button with Coming Soon
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF18181B)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("ANNUAL ACCESS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("$29.99", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                Text(" / year ($2.49/mo)", fontSize = 12.sp, color = Color(0xFFA1A1AA), modifier = Modifier.padding(bottom = 4.dp, start = 4.dp))
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        "Google Play in-app purchase integration is coming soon! You are currently on the Free plan (5 daily generations).",
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF09090B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Upgrade to Pro — Coming Soon",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF09090B)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Google Play In-App Billing will be enabled in an upcoming release.",
                                fontSize = 11.sp,
                                color = Color(0xFF71717A),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
