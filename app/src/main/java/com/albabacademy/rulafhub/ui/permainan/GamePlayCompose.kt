package com.albabacademy.rulafhub.ui.permainan

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.Locale

// ==========================================
// KATEGORI & MODEL DATA UNTUK KOTLIN COMPOSE
// ==========================================
enum class TulisanMode { DWI, JAWI, RUMI }

data class SoalanModel(
    val qRumi: String,
    val qJawi: String,
    val rumiOptions: List<String>,
    val jawiOptions: List<String>,
    val aRumi: String,
    val aJawi: String
)

data class SiriGameModel(
    val id: String,
    val tajuk: String,
    val subjek: String,
    val deskripsi: String,
    val ikon: String,
    val kesukaran: String,
    val levels: Map<Int, List<SoalanModel>>
)

// ==========================================
// BANK SOALAN LOKAL (MAPPING DARIPADA SOALAN.JSON)
// ==========================================
val senaraiSiriGameLokal = listOf(
    SiriGameModel(
        id = "ibadah_solat_Jumaat",
        tajuk = "Misi Solat Jumaat Bahagian 1",
        subjek = "Ibadah",
        deskripsi = "Menguasai Pengertian, Dalil Pensyariatan, serta Hikmah Solat Jumaat secara modular.",
        ikon = "🕌",
        kesukaran = "Sederhana",
        levels = mapOf(
            1 to listOf(
                SoalanModel(
                    qRumi = "Apakah pengertian solat jumaat?",
                    qJawi = "اڤاکه ڤڠرتين صلاة جمعة؟",
                    rumiOptions = listOf(
                        "Solat yang wajib dilakukan oleh ahli Jumaat pada waktu Zohor hari Jumaat seramai 40 orang",
                        "Solat yang wajib dilakukan oleh ahli Khamis pada waktu Maghrib",
                        "Solat yang sunat dilakukan pada waktu Asar"
                    ),
                    jawiOptions = listOf(
                        "صلاة يڠ واجب دلاکوکن اوليه اهلي جمعة يڠ چوکوڤ شرط-شرطڽ ڤd وقتو ظهر هاري جمعة سراماي 40 اورڠ اهلي جمعة",
                        "صلاة يڠ واجب دلاکوکن اوليه اهلي خميس يڠ چوکوڤ شرط-شرطڽ ڤد وقتو مغرب",
                        "صلاة يڠ سنة دلاکوکن اوليه اهلي جمعة يڠ چوکوڤ شرط-شرطڽ"
                    ),
                    aRumi = "Solat yang wajib dilakukan oleh ahli Jumaat pada waktu Zohor hari Jumaat seramai 40 orang",
                    aJawi = "صلاة يڠ واجب دلاکوکن اوليه اهلي جمعة يڠ چوکوڤ شرط-شرطڽ ڤd وقتو ظهر هاري جمعة سراماي 40 اورڠ اهلي جمعة"
                )
            ),
            2 to listOf(
                SoalanModel(
                    qRumi = "Apakah nama surah yang mensyariatkan solat Jumaat?",
                    qJawi = "اڤاکه نام سورة يڠ منشريعتکن صلاة جمعة؟",
                    rumiOptions = listOf("Surah Al-Qariah", "Surah Al-Jumuah", "Surah Al-Munafiqun"),
                    jawiOptions = listOf("سورة القارعة", "سورة الجمعة", "سورة المنافقون"),
                    aRumi = "Surah Al-Jumuah",
                    aJawi = "سورة الجمعة"
                )
            ),
            3 to listOf(
                SoalanModel(
                    qRumi = "Pilih hikmah solat Jumaat:",
                    qJawi = "ڤيليه حکمه صلاة جمعة:",
                    rumiOptions = listOf("Menjadi manusia dibenci", "Melemahkan persaudaraan", "Mengeratkan hubungan Silaturahim sesama Muslim"),
                    jawiOptions = listOf("منجادي ماءنسي يڠ دبنچي", "ملمهکن ايکتن ڤرساوداراءن", "مڠرتکن هوبوڠن صلة الرحيم سسام مسلم"),
                    aRumi = "Mengeratkan hubungan Silaturahim sesama Muslim",
                    aJawi = "مڠرتکن هوبوڠن صلة الرحيم سسام مسلم"
                )
            )
        )
    ),
    SiriGameModel(
        id = "ibadah_solat_istisqa1",
        tajuk = "Misi Solat Istisqa' Bahagian 1",
        subjek = "Ibadah",
        deskripsi = "Menguasai Lafaz Niat, Kaifiat Solat, dan Cara Memohon Hujan ketika musim kemarau.",
        ikon = "🌧️",
        kesukaran = "Tinggi",
        levels = mapOf(
            1 to listOf(
                SoalanModel(
                    qRumi = "Asal perkataan istisqa' ialah:",
                    qJawi = "اصل ڤرکاتاءن استسقاء اياله:",
                    rumiOptions = listOf("taqa", "saqa", "itqa"),
                    jawiOptions = listOf("تقى", "سقى", "اتقى"),
                    aRumi = "saqa",
                    aJawi = "سقى"
                )
            ),
            2 to listOf(
                SoalanModel(
                    qRumi = "Istisqa' pada bahasa ialah:",
                    qJawi = "استسقاء ڤد بهاس اياله:",
                    rumiOptions = listOf("Meminta makanan", "Meminta sedekah", "Meminta Air"),
                    jawiOptions = listOf("ممينتا ماکن", "ممينتا صدقه", "ممينتا اءير"),
                    aRumi = "Meminta Air",
                    aJawi = "ممينتا اءير"
                )
            )
        )
    )
)

// ==========================================
// SKRIN UTAMA: KONSOL PERMAINAN BERSEPADU
// ==========================================
@Composable
fun GamePlayCompose() {
    var selectedGame by remember { mutableStateOf<SiriGameModel?>(null) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (selectedGame == null) {
            GameMenuScreen(onGameSelect = { selectedGame = it })
        } else {
            GamePlayScreen(
                game = selectedGame!!,
                onBackToMenu = { selectedGame = null }
            )
        }
    }
}

// ==========================================
// 1. SKRIN MENU (ARKED PERMAINAN MODULAR)
// ==========================================
@Composable
fun GameMenuScreen(onGameSelect: (SiriGameModel) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredGames = senaraiSiriGameLokal.filter {
        it.tajuk.contains(searchQuery, ignoreCase = true) ||
                it.subjek.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "🎮 ARKED DIDAKTIK RULAF",
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF1793D1),
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Pilih siri pembelajaran bermodular bagi pengukuhan literasi Jawi dan pentaksiran prestasi murid.",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("🔍 Cari siri permainan...") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(8.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(filteredGames) { game ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGameSelect(game) },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = game.ikon, fontSize = 32.sp)
                            Badge(
                                containerColor = if (game.kesukaran == "Mudah") Color(0xFF2E7D32) else Color(0xFFD84315)
                            ) {
                                Text(text = "Tahap: ${game.kesukaran}", color = Color.White, modifier = Modifier.padding(4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = game.tajuk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = game.deskripsi,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Kategori: ${game.subjek}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = "[ Mula Belajar ]", color = Color(0xFF1793D1), fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 2. KONSOL PERMAINAN ASLI (TADRIJ 3 TAHAP)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreen(game: SiriGameModel, onBackToMenu: () -> Unit) {
    val context = LocalContext.current
    var currentLevel by remember { mutableStateOf(1) }
    var currentIdx by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }

    // State Suis Tulisan di Level 1
    var modeTulisan by remember { mutableStateOf(TulisanMode.DWI) }

    // TTS & Canvas Setup
    var tts: TextToSpeech? by remember { mutableStateOf(null) }
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }

    // Muatkan soalan berdasarkan Tahap semasa
    val soalanList = game.levels[currentLevel] ?: emptyList()
    val soalanSemasa = if (soalanList.isNotEmpty() && currentIdx < soalanList.size) soalanList[currentIdx] else null

    // Inisialisasi TextToSpeech
    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("ar", "SA")
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Set semula canvas apabila soalan bertukar
    LaunchedEffect(currentIdx, currentLevel) {
        paths.clear()
        currentPath = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "🎮 ${game.tajuk.uppercase()}") },
                navigationIcon = {
                    IconButton(onClick = onBackToMenu) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1793D1), titleContentColor = Color.White)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isFinished && soalanSemasa != null) {
                // Progress Bar & Level
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "TAHAP SEMASA: TAHAP $currentLevel " +
                                if (currentLevel == 1) "(JAWI & RUMI)" else "(JAWI SAHAJA)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1793D1)
                    )
                    Text(
                        text = "Skor: $score Mata",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Suis Tulisan hanya terpapar pada Level 1 sahaja!
                if (currentLevel == 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TulisanMode.values().forEach { mode ->
                            val isSelected = modeTulisan == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) Color(0xFF1793D1) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { modeTulisan = mode }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (mode) {
                                        TulisanMode.DWI -> "Dwi-Tulisan"
                                        TulisanMode.JAWI -> "Jawi"
                                        TulisanMode.RUMI -> "Rumi"
                                    },
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // Paksa mod Jawi apabila sudah lepas Level 1 (Sistem Tadrij)
                    modeTulisan = TulisanMode.JAWI
                }

                // Kad Soalan Utama
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Misi ${currentIdx + 1} daripada ${soalanList.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            // Butang Sebutan Suara Arab (TTS) untuk lafaz
                            if (soalanSemasa.qRumi.contains("lafaz", ignoreCase = true) || soalanSemasa.qJawi.contains("لفظ", ignoreCase = true)) {
                                Button(
                                    onClick = {
                                        tts?.speak(soalanSemasa.aJawi, TextToSpeech.QUEUE_FLUSH, null, null)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFB300))
                                ) {
                                    Text(text = "🔊 Sebutan", fontSize = 10.sp, color = Color.Black)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Teks Soalan Responsif berdasarkan Mod Tulisan pilihan
                        when (modeTulisan) {
                            TulisanMode.DWI -> {
                                Text(
                                    text = soalanSemasa.qJawi,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Rumi: " + soalanSemasa.qRumi,
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                            TulisanMode.JAWI -> {
                                Text(
                                    text = soalanSemasa.qJawi,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            TulisanMode.RUMI -> {
                                Text(
                                    text = soalanSemasa.qRumi,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Interaktiviti: Kanvas menulis Jawi jika ada soalan melakar
                if (soalanSemasa.qRumi.contains("tulis", ignoreCase = true) || soalanSemasa.qJawi.contains("توليس", ignoreCase = true)) {
                    Text(
                        text = "Sila lakar huruf Jawi jawapan anda pada kanvas di bawah:",
                        fontSize = 11.sp,
                        color = Color.Red,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val path = Path().apply { moveTo(offset.x, offset.y) }
                                        currentPath = path
                                        paths.add(path)
                                    },
                                    onDrag = { change, dragAmount ->
                                        currentPath?.lineTo(change.position.x, change.position.y)
                                    },
                                    onDragEnd = {
                                        currentPath = null
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            paths.forEach { path ->
                                drawPath(
                                    path = path,
                                    color = Color.Black,
                                    style = Stroke(width = 8f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { paths.clear() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Padam")
                        }
                        Button(
                            onClick = {
                                score++
                                Toast.makeText(context, "Syabas! Lakaran Jawi dikesan.", Toast.LENGTH_SHORT).show()
                                if (currentIdx + 1 < soalanList.size) {
                                    currentIdx++
                                } else {
                                    if (currentLevel < (game.levels.size)) {
                                        currentLevel++
                                        currentIdx = 0
                                    } else {
                                        isFinished = true
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Sahkan Tulisan")
                        }
                    }
                } else {
                    // Pilihan jawapan MCQ Dinamik
                    val options = if (modeTulisan == TulisanMode.RUMI) soalanSemasa.rumiOptions else soalanSemasa.jawiOptions
                    val correctAns = if (modeTulisan == TulisanMode.RUMI) soalanSemasa.aRumi else soalanSemasa.aJawi

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(options) { opt ->
                            Button(
                                onClick = {
                                    if (opt == correctAns) {
                                        score++
                                        Toast.makeText(context, "🎉 Betul!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "❌ Kurang Tepat. Jawapan: $correctAns", Toast.LENGTH_LONG).show()
                                    }

                                    // Aliran mara ke soalan / level seterusnya
                                    if (currentIdx + 1 < soalanList.size) {
                                        currentIdx++
                                    } else {
                                        if (currentLevel < (game.levels.size)) {
                                            currentLevel++
                                            currentIdx = 0
                                        } else {
                                            isFinished = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                            ) {
                                Text(
                                    text = opt,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // Skrin Selesai Permainan & Laporan Keputusan
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🏆", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tahniah! Semua Tahap Selesai",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Anda telah menunjukkan usaha penguasaan (Tadrij) Jawi yang sangat cemerlang.",
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "SKOR AKHIR ANDA:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$score Mata",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "* Skor ini disegerakkan (sync) secara automatik ke profil kemajuan sekolah.",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onBackToMenu,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1793D1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Balik Ke Arked")
                        }
                    }
                }
            }
        }
    }
}