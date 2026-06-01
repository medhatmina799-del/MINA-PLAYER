package com.example

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.DbMediaTrack
import com.example.database.DbPlaylist
import com.example.ui.theme.*
import com.example.viewmodel.MinaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MinaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isAmoled by viewModel.isAmoledEnabled.collectAsStateWithLifecycle()

            MyApplicationTheme(isAmoled = isAmoled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MinaPlayerApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun MinaPlayerApp(viewModel: MinaViewModel) {
    val tab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isFullPlayerOpen by viewModel.isFullPlayerOpen.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val currentPlayingTrack by viewModel.currentPlayingTrack.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                MinaHeader(
                    onSettingsClick = { viewModel.setSettingsOpen(true) }
                )
            },
            bottomBar = {
                MinaBottomNavigation(
                    activeTab = tab,
                    onTabSelected = { viewModel.switchTab(it) }
                )
            },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.navigationBars
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (tab) {
                    "music" -> MusicTabContent(viewModel = viewModel)
                    "videos" -> VideosTabContent(viewModel = viewModel)
                    "playlist" -> PlaylistTabContent(viewModel = viewModel)
                    "youtube" -> YouTubeTabContent(viewModel = viewModel)
                }
            }
        }

        // Mini player overlays atop screens if track is active
        AnimatedVisibility(
            visible = currentPlayingTrack != null && !isFullPlayerOpen,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 85.dp, start = 16.dp, end = 16.dp)
        ) {
            MinaMiniPlayer(viewModel = viewModel)
        }

        // Full Screen Player Sheet Overlay
        AnimatedVisibility(
            visible = isFullPlayerOpen,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMedium)
            )
        ) {
            MinaFullPlayerPage(viewModel = viewModel)
        }

        // Settings Sidebar overlay Sheet
        AnimatedVisibility(
            visible = isSettingsOpen,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            MinaSettingsSheet(viewModel = viewModel)
        }
    }
}

// 1. Sleek Top Header for Mina Player
@Composable
fun MinaHeader(onSettingsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .semantics { contentDescription = "Header" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = NeonGreen,
                modifier = Modifier
                    .size(24.dp)
                    .drawBehind {
                        drawCircle(color = NeonGreen.copy(alpha = 0.25f), radius = size.minDimension)
                    }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "MINA PLAYER",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                style = TextStyle(
                    shadow = Shadow(color = NeonGreen.copy(alpha = 0.5f), blurRadius = 15f)
                )
            )
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(48.dp)
                .testTag("settings_btn")
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "الإعدادات",
                tint = TextMuted,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

// 2. Interactive Navigation tabs
@Composable
fun MinaBottomNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        color = NavBg.copy(alpha = 0.98f),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.05f)),
        shadowElevation = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MinaNavItem(
                icon = Icons.Default.MusicNote,
                title = "الموسيقى",
                isActive = activeTab == "music",
                onClick = { onTabSelected("music") },
                testTag = "btn_tab_music"
            )
            MinaNavItem(
                icon = Icons.Default.Videocam,
                title = "الفيديوهات",
                isActive = activeTab == "videos",
                onClick = { onTabSelected("videos") },
                testTag = "btn_tab_videos"
            )
            MinaNavItem(
                icon = Icons.AutoMirrored.Filled.PlaylistPlay,
                title = "بلاي ليست",
                isActive = activeTab == "playlist",
                onClick = { onTabSelected("playlist") },
                testTag = "btn_tab_playlist"
            )
            MinaNavItem(
                icon = Icons.Default.Public,
                title = "المنصة",
                isActive = activeTab == "youtube",
                onClick = { onTabSelected("youtube") },
                testTag = "btn_tab_youtube"
            )
        }
    }
}

@Composable
fun RowScope.MinaNavItem(
    icon: ImageVector,
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val activeColor = NeonGreen
    val inactiveColor = TextMuted

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .semantics { contentDescription = title },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) activeColor else inactiveColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            color = if (isActive) activeColor else inactiveColor,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// 3. Tab 1 CONTENT: الموسيقى (Music List + Picking files)
@Composable
fun MusicTabContent(viewModel: MinaViewModel) {
    val context = LocalContext.current
    val tracks by viewModel.audioTracks.collectAsStateWithLifecycle()
    val playingTrack by viewModel.currentPlayingTrack.collectAsStateWithLifecycle()

    val hasAudioPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    var isPermissionGranted by remember { mutableStateOf(hasAudioPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isPermissionGranted = permissions.values.all { it }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val fileName = getFileNameFromUri(context, it)
                viewModel.importTrack(fileName, it.toString(), "AUDIO")
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        if (!isPermissionGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "الوصول لملفات الجهاز",
                        color = NeonGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يسمح هذا الإذن للتطبيق باكتشاف وتشغيل ملفات الصوت والفيديو المخزنة على جهازك مباشرة.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val perms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    android.Manifest.permission.READ_MEDIA_AUDIO,
                                    android.Manifest.permission.READ_MEDIA_VIDEO
                                )
                            } else {
                                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(perms)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("منح الإذن", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "صوتياتك المفضلـة",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Neon glowing file browse button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(NeonGreen, NeonBlue)))
                    .clickable { launcher.launch(arrayOf("audio/*")) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Browse Files",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier
                            .size(70.dp)
                            .padding(bottom = 12.dp)
                    )
                    Text(
                        text = "قائمتك فارغة، اضغط Browse لإضافة نغماتك",
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(tracks) { track ->
                    val isPlayingNow = playingTrack?.id == track.id
                    TrackCard(
                        track = track,
                        isPlaying = isPlayingNow,
                        onPlayClick = { viewModel.playTrack(track) },
                        onDeleteClick = { viewModel.deleteTrack(track) }
                    )
                }
            }
        }
    }
}

// 4. Track Card visual components
@Composable
fun TrackCoverImage(
    thumbnailUrl: String?,
    trackName: String,
    size: androidx.compose.ui.unit.Dp = 44.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!thumbnailUrl.isNullOrEmpty()) {
            androidx.compose.foundation.Image(
                painter = coil.compose.rememberAsyncImagePainter(thumbnailUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            val angleColors = remember(trackName) {
                val seed = java.lang.Math.abs(trackName.hashCode())
                val choice = seed % 4
                when (choice) {
                    0 -> listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)) // Purple/Pink
                    1 -> listOf(Color(0xFF3B82F6), Color(0xFF10B981)) // Blue/Green
                    2 -> listOf(Color(0xFFF59E0B), Color(0xFFEF4444)) // Yellow/Red
                    else -> listOf(Color(0xFF06B6D4), Color(0xFF3B82F6)) // Cyan/Blue
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(angleColors)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size / 2)
                )
            }
        }
    }
}

@Composable
fun TrackCard(
    track: DbMediaTrack,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlayClick() }
            .testTag("track_item_${track.id}"),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        border = if (isPlaying) BorderStroke(1.dp, NeonGreen) else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = if (isPlaying) NeonGreen else NeonBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                TrackCoverImage(thumbnailUrl = track.thumbnailUrl, trackName = track.name, size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = track.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (track.isSample) "نموذج بث من مينا" else "ملف مستورد",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (track.type == "AUDIO") "MP3" else "MP4",
                    color = NeonGreen,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Delete button to inspect/modify imported files
                if (!track.isSample) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف الملف",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

// 5. Tab 2 CONTENT: الفيديوهات (Videos List + Player screen Box)
@Composable
fun VideosTabContent(viewModel: MinaViewModel) {
    val context = LocalContext.current
    val videos by viewModel.videoTracks.collectAsStateWithLifecycle()
    val playingVideo by viewModel.currentVideoTrack.collectAsStateWithLifecycle()

    val hasVideoPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
    var isPermissionGranted by remember { mutableStateOf(hasVideoPermission) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isPermissionGranted = permissions.values.all { it }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val fileName = getFileNameFromUri(context, it)
                viewModel.importTrack(fileName, it.toString(), "VIDEO")
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        if (!isPermissionGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .border(1.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "الوصول لملفات الفيديو",
                        color = NeonGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "يسمح هذا الإذن للتطبيق باكتشاف وتشغيل ملفات الفيديو والمواد المرئية المخزنة على جهازك مباشرة.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val perms = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                arrayOf(
                                    android.Manifest.permission.READ_MEDIA_AUDIO,
                                    android.Manifest.permission.READ_MEDIA_VIDEO
                                )
                            } else {
                                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            }
                            permissionLauncher.launch(perms)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("منح الإذن", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "مكتبة الفيديو",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            // Video browse buttons
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(NeonBlue, NeonGreen)))
                    .clickable { launcher.launch(arrayOf("video/*")) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideoCall,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Browse Video",
                    color = Color.Black,
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Embedded video player box if a video is active!
        playingVideo?.let { activeVideo ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .border(1.dp, NeonBlue, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black)
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.DarkGray)
                    ) {
                        VideoViewWidget(videoUrl = activeVideo.url)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activeVideo.name,
                            color = NeonBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { viewModel.pauseVideo() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "إغلاق",
                                tint = Color.Red,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        if (videos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier
                            .size(70.dp)
                            .padding(bottom = 12.dp)
                    )
                    Text(
                        text = "لا توجد فيديوهات مضافة حالياً",
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(videos) { track ->
                    val isPlayingVideo = playingVideo?.id == track.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.playVideo(track) },
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        shape = RoundedCornerShape(16.dp),
                        border = if (isPlayingVideo) BorderStroke(1.dp, NeonBlue) else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = NeonBlue,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = track.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (track.isSample) "نموذج بث" else "ملف فيديو مستورد",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!track.isSample) {
                                    IconButton(
                                        onClick = { viewModel.deleteTrack(track) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف الفيديو",
                                            tint = Color.Red.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Android native player bridge for Compose with robust lifecycle handling
@Composable
fun VideoViewWidget(videoUrl: String) {
    val context = LocalContext.current
    var videoViewRef: VideoView? by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(videoUrl) {
        onDispose {
            try {
                videoViewRef?.stopPlayback()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setOnErrorListener { _, _, _ ->
                    true // handle error gracefully to prevent system dialog popup or crash
                }
                videoViewRef = this
            }
        },
        update = { videoView ->
            try {
                val uri = Uri.parse(videoUrl)
                if (videoUrl.startsWith("content://")) {
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri,
                            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (e: Exception) {
                        // Ignore or log if permission cannot be extended
                    }
                }
                videoView.setVideoURI(uri)
                videoView.setOnPreparedListener { mp ->
                    try {
                        mp.isLooping = true
                        videoView.start()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

// 6. Tab 3 CONTENT: بلاي ليست (Playlists Grid)
@Composable
fun PlaylistTabContent(viewModel: MinaViewModel) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val selectedPlaylistName by viewModel.selectedPlaylistName.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .border(1.dp, NeonBlue, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "إنشاء قائمة تشغيل جديدة",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("اسم القائمة", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonGreen,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("playlist_name_input")
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (newPlaylistName.isNotBlank()) {
                                    viewModel.addPlaylist(newPlaylistName)
                                    newPlaylistName = ""
                                    showDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_create_playlist")
                        ) {
                            Text("تأكيد", color = Color.Black)
                        }
                        OutlinedButton(
                            onClick = { showDialog = false },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("إلغاء")
                        }
                    }
                }
            }
        }
    }

    if (selectedPlaylistName != null) {
        PlaylistDetailScreen(viewModel = viewModel, playlistName = selectedPlaylistName ?: "")
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "قوائم التشغيل",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Add list button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(NeonGreen, NeonBlue)))
                        .clickable { showDialog = true }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .testTag("add_playlist_btn"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "قائمة جديدة",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (playlists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier
                                .size(70.dp)
                                .padding(bottom = 12.dp)
                        )
                        Text(
                            text = "اصنع اول قائمة تشغيل مخصصة لك الآن",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(playlists) { playlist ->
                        PlaylistCard(
                            playlist = playlist,
                            onClick = { viewModel.selectPlaylist(playlist.name) },
                            onDelete = { viewModel.deletePlaylist(playlist) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistCard(
    playlist: DbPlaylist,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("playlist_card_${playlist.id}"),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonBlue.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(24.dp)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "مسح القائمة",
                        tint = Color.Red.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = playlist.name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${playlist.trackCount} ملف",
                color = TextMuted,
                fontSize = 11.sp
            )
        }
    }
}

// Custom Drill-Down detailed view of selected playlists
@Composable
fun PlaylistDetailScreen(
    viewModel: MinaViewModel,
    playlistName: String
) {
    val context = LocalContext.current
    val tracks by viewModel.getTracksForPlaylist(playlistName).collectAsStateWithLifecycle(initialValue = emptyList())
    val playingTrack by viewModel.currentPlayingTrack.collectAsStateWithLifecycle()

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            uri?.let {
                val fileName = getFileNameFromUri(context, it)
                viewModel.importTrack(fileName, it.toString(), "AUDIO")
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.selectPlaylist(null) }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "رجوع",
                        tint = Color.White
                    )
                }
                Text(
                    text = playlistName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { fileLauncher.launch(arrayOf("audio/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("إضافة صوت", color = Color.Black, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "القائمة فارغة، اضغط إضافة صوت لاستيراد أغانيك!",
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 30.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(tracks) { track ->
                    val isPlayingNow = playingTrack?.id == track.id
                    TrackCard(
                        track = track,
                        isPlaying = isPlayingNow,
                        onPlayClick = { viewModel.playTrack(track) },
                        onDeleteClick = { viewModel.deleteTrack(track) }
                    )
                }
            }
        }
    }
}

// 7. BOTTOM MINI PLAYER OVERLAY COMPONENT
@Composable
fun MinaMiniPlayer(viewModel: MinaViewModel) {
    val track by viewModel.currentPlayingTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()

    if (track == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { viewModel.setFullPlayerOpen(true) }
            .testTag("mini_player_bar"),
        colors = CardDefaults.cardColors(containerColor = NavBg.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album Art thumbnail with musical wave
                TrackCoverImage(thumbnailUrl = track?.thumbnailUrl, trackName = track?.name ?: "", size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = track?.name ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Mina Player",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Click play/pause stopPropagation
            IconButton(
                onClick = { viewModel.togglePlayPause() },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .testTag("play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// 8. FULL SCREEN PLAYER PAGE (Vinyl rotation & seek controls)
@Composable
fun MinaFullPlayerPage(viewModel: MinaViewModel) {
    val trackState by viewModel.currentPlayingTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentTimeMs by viewModel.currentTimeMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()

    val track = trackState ?: return

    // Vinyl spinning rotation transition state
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val currentRotationAngle = if (isPlaying) rotation else 0f

    val brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF111412), Color(0xFF050506))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush)
            .statusBarsPadding()
            .padding(20.dp)
            .semantics { contentDescription = "Full Screen Player" }
    ) {
        // App header Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.setFullPlayerOpen(false) },
                modifier = Modifier.testTag("back_down_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "إرجاع لأسفل",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = "مشغل الموسيقى",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Box(modifier = Modifier.size(48.dp)) // empty spacer
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Giant Vinyl Disc centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .clip(CircleShape)
                    .rotate(currentRotationAngle)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFF1A1A1F), Color(0xFF000000)),
                            radius = 450f
                        )
                    )
                    .border(8.dp, Color(0xFF27272A), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Vinyl grooves drawing
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = Color.White.copy(alpha = 0.05f), radius = size.minDimension / 2.5f)
                    drawCircle(color = Color.White.copy(alpha = 0.03f), radius = size.minDimension / 3.5f)
                }
                if (!track.thumbnailUrl.isNullOrEmpty()) {
                    androidx.compose.foundation.Image(
                        painter = coil.compose.rememberAsyncImagePainter(track.thumbnailUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    val angleColors = remember(track.name) {
                        val seed = java.lang.Math.abs(track.name.hashCode())
                        val choice = seed % 4
                        when (choice) {
                            0 -> listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                            1 -> listOf(Color(0xFF3B82F6), Color(0xFF10B981))
                            2 -> listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                            else -> listOf(Color(0xFF06B6D4), Color(0xFF3B82F6))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(angleColors))
                            .border(2.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }
        }

        // Song details
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = track?.name ?: "",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "MINA PLAYER",
                color = NeonGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Timeline Precision Slider Seeker
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTimeCompact(currentTimeMs),
                color = TextMuted,
                fontSize = 11.sp,
                modifier = Modifier.width(45.dp)
            )

            Slider(
                value = if (durationMs > 0) currentTimeMs.toFloat() else 0f,
                onValueChange = { viewModel.seekTo(it.toLong()) },
                valueRange = 0f..(if (durationMs > 0) durationMs.toFloat() else 100f),
                colors = SliderDefaults.colors(
                    activeTrackColor = NeonGreen,
                    inactiveTrackColor = Color(0xFF27272A),
                    thumbColor = NeonGreen
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("timeline_slider")
            )

            Text(
                text = formatTimeCompact(durationMs),
                color = TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.width(45.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playback Control Hub
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 30.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.playPreviousTrack() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "السابق",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { viewModel.rewind10Seconds() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "تأخير 10 ثواني",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Large neon play hub with outer shadow glow
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(NeonGreen)
                    .clickable { viewModel.togglePlayPause() }
                    .semantics { contentDescription = if (isPlaying) "Pause" else "Play" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(20.dp))

            IconButton(
                onClick = { viewModel.forward10Seconds() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "تقديم 10 ثواني",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            IconButton(
                onClick = { viewModel.playNextTrack() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "التالي",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

// 9. SETTINGS OVERLAY SHEET
@Composable
fun MinaSettingsSheet(viewModel: MinaViewModel) {
    val durationText = viewModel.formatListeningTime()
    val isEqEnabled by viewModel.isEqualizerEnabled.collectAsStateWithLifecycle()
    val isSleepEnabled by viewModel.isSleepTimerEnabled.collectAsStateWithLifecycle()
    val isBassEnabled by viewModel.isBassBoostEnabled.collectAsStateWithLifecycle()
    val isAmoledEnabled by viewModel.isAmoledEnabled.collectAsStateWithLifecycle()
    val isAutoplayEnabled by viewModel.isAutoplayNext.collectAsStateWithLifecycle()
    val isAudioHighEnabled by viewModel.isAudioQualityHigh.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable { viewModel.setSettingsOpen(false) }
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clickable(enabled = false) {}, // prevent click-through dismissal
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = NeonBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إعدادات المطور",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setSettingsOpen(false) },
                        modifier = Modifier.testTag("close_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق شيت الإعدادات",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Time Watch Dashboard Telemetry Value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(SettingsHeaderBg, Color.Black)))
                        .border(1.dp, SettingsBorderColor, RoundedCornerShape(16.dp))
                        .padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "وقت الاستماع النشط (Time Watch)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = durationText,
                            color = NeonGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Settings Rows
                MinaSettingsToggleRow(
                    title = "موزع الصوت المحيطي (Equalizer)",
                    subtitle = "تفعيل وضع الصوت ثلاثي الأبعاد والصدى",
                    isChecked = isEqEnabled,
                    onCheckedChange = { viewModel.toggleEqualizer() }
                )
                MinaSettingsToggleRow(
                    title = "مؤقت النوم الذكي (Sleep Timer)",
                    subtitle = "إيقاف التشغيل التلقائي بعد 30 ثانية لتسهيل الفحص والتشغيل",
                    isChecked = isSleepEnabled,
                    onCheckedChange = { viewModel.toggleSleepTimer() }
                )
                MinaSettingsToggleRow(
                    title = "تحسين الباص (Bass Boost)",
                    subtitle = "تضخيم النغمات والنبضات العميقة للموسيقى النيون",
                    isChecked = isBassEnabled,
                    onCheckedChange = { viewModel.toggleBassBoost() }
                )
                MinaSettingsToggleRow(
                    title = "الوضع الليلي فائق العتمة (AMOLED)",
                    subtitle = "تحسين التباين وتوفير طاقة خلايا شاشات AMOLED",
                    isChecked = isAmoledEnabled,
                    onCheckedChange = { viewModel.toggleAmoled() }
                )
                MinaSettingsToggleRow(
                    title = "التشغيل المتتالي التلقائي",
                    subtitle = "الانتقال التلقائي للأغنية التالية فور انتهاء الأغنية الحالية",
                    isChecked = isAutoplayEnabled,
                    onCheckedChange = { viewModel.toggleAutoplayNext() }
                )
                MinaSettingsToggleRow(
                    title = "محرك الصوت فائق الوضوح (HIFI HD)",
                    subtitle = "تفعيل ترقية إشارة الصوت وتحسين جودة البت إلى 320kbps",
                    isChecked = isAudioHighEnabled,
                    onCheckedChange = { viewModel.toggleAudioQualityHigh() }
                )
            }
        }
    }
}

@Composable
fun MinaSettingsToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = { onCheckedChange() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = NeonGreen,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

// 10. Format seconds to compact readable clock: 00:00
private fun formatTimeCompact(timeMs: Long): String {
    val totalSeconds = timeMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

// Extract real filename from Uri path safely
private fun getFileNameFromUri(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "ملف نيون مستورد"
}

// 11. Interactive Full-screen Integrated Web Platform screen
@Composable
fun YouTubeTabContent(viewModel: com.example.viewmodel.MinaViewModel) {
    var isWebLoading by remember { mutableStateOf(true) }
    var webViewInstance: android.webkit.WebView? by remember { mutableStateOf(null) }

    // Intercept hardware/gesture back press when on this tab to navigate back inside the WebView history
    androidx.activity.compose.BackHandler(enabled = webViewInstance?.canGoBack() == true) {
        webViewInstance?.goBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(NeonGreen, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "المنصة المباشرة",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Safe reload control
            IconButton(
                onClick = { webViewInstance?.reload() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "إعادة تحميل",
                    tint = NeonGreen
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            AndroidView(
                factory = { context ->
                    android.webkit.WebView(context).apply {
                        webViewInstance = this
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.databaseEnabled = true
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isWebLoading = true
                            }

                            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                isWebLoading = false
                            }
                            
                            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                val redirectUrl = request?.url?.toString() ?: ""
                                if (redirectUrl.isNotEmpty()) {
                                    view?.loadUrl(redirectUrl)
                                }
                                return true
                            }
                        }
                        loadUrl("https://freefy.app/#google_vignette")
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { /* Updates handled in standard factory lifecycle */ }
            )

            if (isWebLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonGreen)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "جاري الاتصال الآمن بالمنصة الفورية...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun YouTubeTabContentOldUnused(viewModel: com.example.viewmodel.MinaViewModel) {
    val searchQuery by viewModel.youtubeSearchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.youtubeSearchResults.collectAsStateWithLifecycle()
    val isLoading by viewModel.youtubeSearchLoading.collectAsStateWithLifecycle()
    val downloadingTrackId by viewModel.youtubeDownloadingTrackId.collectAsStateWithLifecycle()

    var textState by remember { mutableStateOf(searchQuery) }
    var isWebViewOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color.Red, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "البحث الذكي للميديا",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = { isWebViewOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, NeonGreen),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    tint = NeonGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "فتح المنصة المباشرة",
                    color = NeonGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Beautiful Search Bar Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = textState,
                onValueChange = { textState = it },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                placeholder = { Text("بحث الآن...", color = TextMuted, fontSize = 13.sp) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = NeonGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    cursorColor = NeonGreen,
                    focusedContainerColor = Color.White.copy(alpha = 0.03f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.searchYouTube(textState) },
                modifier = Modifier
                    .size(56.dp)
                    .background(NeonGreen, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NeonGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("جاري استخلاص النتائج الذكية...", color = TextMuted, fontSize = 12.sp)
                }
            }
        } else if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .clickable { isWebViewOpen = true }
                        .border(0.5.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.02f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tv,
                            contentDescription = "البحث",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "ابحث وتصفح وحمّل مباشرة!",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "ادخل كلمة بحث للاستماع الفوري وتنزيل أي ملف تريده مباشرة، أو اضغط على هذا القسم لفتح موقع البث والبحث المباشر داخل البرنامج مع الحفاظ التام على خصوصيتك وسريتك.",
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { isWebViewOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تصفح المنصة التفاعلية المباشرة",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(searchResults) { result ->
                    val isResultDownloading = downloadingTrackId == result.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(width = 0.5.dp, color = Color.White.copy(alpha = 0.06f), shape = RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (result.type == "VIDEO") Icons.Default.Videocam else Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = if (result.type == "VIDEO") NeonGreen else Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = result.channel,
                                        color = TextMuted,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "•",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = result.views,
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (result.type == "VIDEO") NeonGreen.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (result.type == "VIDEO") "فيديو" else "صوتيات",
                                            color = if (result.type == "VIDEO") NeonGreen else Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = result.duration,
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (result.type == "VIDEO") {
                                            viewModel.playVideo(
                                                com.example.database.DbMediaTrack(
                                                    name = result.title,
                                                    url = result.url,
                                                    type = "VIDEO",
                                                    playlistName = null,
                                                    isSample = false
                                                )
                                            )
                                        } else {
                                            viewModel.playTrack(
                                                com.example.database.DbMediaTrack(
                                                    name = result.title,
                                                    url = result.url,
                                                    type = "AUDIO",
                                                    playlistName = null,
                                                    isSample = false
                                                )
                                            )
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل",
                                        tint = NeonGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (isResultDownloading) {
                                    CircularProgressIndicator(
                                        color = NeonGreen,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else if (result.isDownloaded) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "تم التحميل",
                                        tint = NeonGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    IconButton(
                                        onClick = { viewModel.downloadYouTubeTrack(result) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "تنزيل",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Interactive Full-screen Web browser overlay inside the app, with website name hidden
    if (isWebViewOpen) {
        Dialog(
            onDismissRequest = { isWebViewOpen = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF121212)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Hidden website URL/domain, but interactive controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .background(Color(0xFF1E1E1E))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { isWebViewOpen = false },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowBack,
                                    contentDescription = "رجوع",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "منصة الميديا المباشرة",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = { isWebViewOpen = false }
                        ) {
                            Text("رجوع", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    var isWebLoading by remember { mutableStateOf(true) }
                    var webViewInstance: android.webkit.WebView? by remember { mutableStateOf(null) }

                    // Safe intercept system back press to navigate back in history inside webview
                    androidx.activity.compose.BackHandler(enabled = isWebViewOpen) {
                        if (webViewInstance?.canGoBack() == true) {
                            webViewInstance?.goBack()
                        } else {
                            isWebViewOpen = false
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { context ->
                                android.webkit.WebView(context).apply {
                                    webViewInstance = this
                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    settings.databaseEnabled = true
                                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                                    
                                    webViewClient = object : android.webkit.WebViewClient() {
                                        override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                            isWebLoading = true
                                        }

                                        override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                            isWebLoading = false
                                        }
                                        
                                        override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                            val redirectUrl = request?.url?.toString() ?: ""
                                            if (redirectUrl.isNotEmpty()) {
                                                view?.loadUrl(redirectUrl)
                                            }
                                            return true
                                        }
                                    }
                                    loadUrl("https://freefy.app/#google_vignette")
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            update = { /* handled in factory */ }
                        )

                        if (isWebLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = NeonGreen)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("جاري الاتصال الآمن بالمنصة التفاعلية...", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
