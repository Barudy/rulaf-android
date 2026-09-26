package com.albabacademy.rulafhub.ui.Rvids

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.albabacademy.rulafhub.BuildConfig
import com.albabacademy.rulafhub.dapatkanNamaFailFizikal
import com.albabacademy.rulafhub.data.remote.RetrofitClient
import com.albabacademy.rulafhub.data.remote.RulafVideoDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.abs

// =====================================================================
// 📹 RVIDS - SUAIAN VIDEO PENDEK (TikTok / Shorts style)
// =====================================================================
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RvidsScreen(userRole: String, userEmail: String) {
    var videos by remember { mutableStateOf<List<RulafVideoDto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // State untuk FAB modal bottom sheet (Cipta Video)
    var showCiptaVideoSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val listState = rememberLazyListState()

    // 🎯 Kesan "snap" ala TikTok / YouTube Shorts: lepaskan jari terus lompat ke video penuh seterusnya
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    fun muatSemula() {
        isLoading = true
        scope.launch {
            try {
                videos = withContext(Dispatchers.IO) { RetrofitClient.api.getRulafVideos() }
                error = null
            } catch (e: Exception) {
                error = "Gagal memuat video. Sila periksa internet."
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { muatSemula() }

    // Pilih item yang berada di tengah skrin supaya hanya satu video dimainkan
    val indexAktif by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val pusat = layoutInfo.viewportEndOffset / 2
            layoutInfo.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - pusat) }?.index ?: 0
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )

            error != null -> Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(error.orEmpty(), color = Color.White, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                IconButton(onClick = { muatSemula() }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Cuba Semula",
                        tint = Color.White
                    )
                }
            }

            videos.isEmpty() -> Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.VideoLibrary,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f)
                )
                Text(
                    "Tiada video Rvids buat masa ini.",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }

            else -> {
                LazyColumn(
                    state = listState,
                    flingBehavior = flingBehavior,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(videos) { index, video ->
                        VerticalVideoItem(
                            video = video,
                            isActive = index == indexAktif,
                            modifier = Modifier.fillParentMaxHeight()
                        )
                    }
                }

                // Header atas (nama modul)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        "🎬 RVIDS :: BELAJAR MICRO",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        // 🎬 FAB untuk Cipta Video (hanya untuk Guru)
        if (!userRole.equals("Murid", ignoreCase = true)) {
            FloatingActionButton(
                onClick = { showCiptaVideoSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color(0xFF1793D1), // ArchBlue
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Videocam,
                    contentDescription = "Cipta Video",
                    modifier = androidx.compose.ui.Modifier.width(28.dp).height(28.dp)
                )
            }
        }
    }

    // 🎬 Modal Bottom Sheet untuk Cipta Video (Rvids Creator)
    if (showCiptaVideoSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCiptaVideoSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            content = {
                CiptaVideoSheetContent(
                    userRole = userRole,
                    userEmail = userEmail,
                    onDismiss = { showCiptaVideoSheet = false },
                    onVideoPublished = { muatSemula() }
                )
            }
        )
    }
}

@Composable
fun VerticalVideoItem(
    video: RulafVideoDto,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(video.pautan_video))
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = isActive
                prepare()
            }
    }

    LaunchedEffect(isActive) {
        if (isActive) {
            if (!exoPlayer.isPlaying) {
                exoPlayer.playWhenReady = true
                exoPlayer.play()
            }
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(
        modifier = modifier.background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = true
                    player = exoPlayer
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    keepScreenOn = true
                }
            },
            update = { it.player = exoPlayer },
            modifier = Modifier.fillMaxSize()
        )

        // Info video di bahagian bawah
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(start = 16.dp, end = 16.dp, bottom = 14.dp, top = 8.dp)
        ) {
            Text(
                video.tajuk_video,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "#${video.kategori ?: "Jawi"}",
                    color = Color(0xFF76D7FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    video.pencipta.orEmpty(),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
}
    }
}
}

// 🎬 Modal Bottom Sheet Content untuk Cipta Video (Rvids Creator)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CiptaVideoSheetContent(
    userRole: String,
    userEmail: String,
    onDismiss: () -> Unit,
    onVideoPublished: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tajuk by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf("Jawi") }
    var urlVideo by remember { mutableStateOf<String?>(null) }
    var namaFailDipilih by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var mesej by remember { mutableStateOf<String?>(null) }

    val senaraiKategori = listOf(
        "Jawi", "Ibadat", "Bahasa Arab", "Sirah", "Tauhid", "Adab", "Leaderboard"
    )

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            urlVideo = null
            namaFailDipilih = null
            uploadProgress = 0f
            scope.launch {
                isUploading = true
                mesej = null
                try {
                    val namaAsal = dapatkanNamaFailFizikal(context, uri)
                    val urlAwam = muatNaikVideoRvidsKeStoran(context, uri, namaAsal)
                    if (urlAwam != null) {
                        urlVideo = urlAwam
                        namaFailDipilih = namaAsal
                    } else {
                        mesej = "Gagal memuat naik video. Sila cuba lagi."
                    }
                } catch (e: Exception) {
                    mesej = "Ralat semasa memuat naik video."
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "🎬 CIPTA VIDEO RVIDS",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF1793D1)
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
            }
        }

        Text(
            "Muat naik video pendek pengajaran ke modul-rulaf (storan) dan siarkan ke suapan Rvids.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Tajuk Video
        androidx.compose.material3.OutlinedTextField(
            value = tajuk,
            onValueChange = { tajuk = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tajuk Video") },
            singleLine = true
        )

        // Kategori
        Text(
            "KATEGORI:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            senaraiKategori.forEach { cat ->
                androidx.compose.material3.FilterChip(
                    selected = kategori == cat,
                    onClick = { kategori = cat },
                    label = { Text(cat, fontSize = 9.sp) }
                )
            }
        }

        // Pilih & Muat Naik Video
        if (isUploading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = uploadProgress,
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF1793D1),
                    trackColor = Color.LightGray
                )
                Text(
                    "Memuat naik video ke storan awan... ${(uploadProgress * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = Color(0xFF1793D1),
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            androidx.compose.material3.OutlinedButton(
                onClick = { videoPicker.launch("video/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (namaFailDipilih != null) "✓ $namaFailDipilih" else "Pilih Video MP4 Dari Telefon")
            }
        }

        mesej?.let {
            Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
        }

        // Butang Siaran
        Button(
            onClick = {
                if (tajuk.isBlank()) {
                    Toast.makeText(context, "Sila isi tajuk video.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (urlVideo == null) {
                    Toast.makeText(context, "Sila pilih & muat naik video dahulu.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    mesej = null
                    try {
                        val dto = RulafVideoDto(
                            tajuk_video = tajuk.trim(),
                            pautan_video = urlVideo!!,
                            kategori = kategori,
                            pencipta = userEmail,
                            is_aktif = true
                        )
                        val res = withContext(Dispatchers.IO) {
                            RetrofitClient.api.publishRulafVideo(body = dto)
                        }
                        if (res.isSuccessful) {
                            Toast.makeText(context, "✓ Video berjaya disiarkan ke Rvids!", Toast.LENGTH_SHORT).show()
                            tajuk = ""
                            urlVideo = null
                            namaFailDipilih = null
                            onVideoPublished()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Gagal siarkan: ${res.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Ralat: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1793D1)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("🚀 Siaran Video", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
suspend fun muatNaikVideoRvidsKeStoran(
    context: Context,
    uri: Uri,
    namaAsal: String
): String? = kotlinx.coroutines.withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val projectUrl = "https://pzktjmtmkuicsjjjezjb.supabase.co"
        val namaBersih = namaAsal.substringBeforeLast('.').replace("[^a-zA-Z0-9]".toRegex(), "_")
        val storagePath = "rvids/${System.currentTimeMillis()}_$namaBersih.mp4"

        val uploadUrl = URL("$projectUrl/storage/v1/object/modul-rulaf/$storagePath")
        connection = (uploadUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            setRequestProperty("Content-Type", "video/mp4")
            connectTimeout = 60000
            readTimeout = 60000
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            connection.outputStream.use { output -> input.copyTo(output) }
        }

        if (connection.responseCode in 200..299) {
            "$projectUrl/storage/v1/object/public/modul-rulaf/$storagePath"
        } else {
            val ralat = connection.errorStream?.bufferedReader()?.use { it.readText() }
            android.util.Log.e("RuLaF_Rvids", "Ralat Muat Naik: ${connection.responseCode} - $ralat")
            null
        }
    } catch (e: Exception) {
        android.util.Log.e("RuLaF_Rvids", "Ralat Muat Naik: ${e.message}", e)
        null
    } finally {
        connection?.disconnect()
    }
}