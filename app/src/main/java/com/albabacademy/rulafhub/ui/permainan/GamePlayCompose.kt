@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)
package com.albabacademy.rulafhub.ui.permainan

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.albabacademy.rulafhub.data.remote.HantarKerajinanRequest
import com.albabacademy.rulafhub.data.remote.LeaderboardDto
import com.albabacademy.rulafhub.data.remote.QuizDto
import com.albabacademy.rulafhub.data.remote.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

// Warna Rasmi RuLaF
private val ArchBlue = Color(0xFF1793D1)
private val ArchOrange = Color(0xFFE95420)
private val SystemGreen = Color(0xFF10B981)

enum class TulisanMode { DWI, JAWI, RUMI }

enum class DifficultyLevel(val label: String, val desc: String, val timerSec: Int?, val multiplier: Double) {
    SENANG("Senang", "Tiada Masa • Tulisan Rumi • 1.0x Skor", null, 1.0),
    SEDERHANA("Sederhana", "30 Saat • Dwi-Tulisan • 1.5x Skor", 30, 1.5),
    SUKAR("Sukar", "15 Saat • Tulisan Jawi Sahaja • 2.0x Skor", 15, 2.0)
}

data class SoalanModel(
    val qRumi: String,
    val qJawi: String,
    val rumiOptions: List<String>,
    val jawiOptions: List<String>,
    val aRumi: String,
    val aJawi: String,
    val gambarUrl: String? = null,
    val type: String = "pilihan" // 🧩 Sokongan jenis soalan: 'pilihan' atau 'susun_atur'
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

data class ChipState(
    val id: Int,
    val word: String,
    val used: Boolean = false
)

// =====================================================================
// KONSOL UTAMA & KAWALAN PERANAN (MURID SAHAJA)
// =====================================================================
@Composable
fun RuLaFGameEngineApp(
    userRole: String = "Murid",
    userEmail: String = "",
    userMyKid: String = "",
    userName: String = "Hero Murid",
    isDarkMode: Boolean = true,
    onExit: () -> Unit = {}
) {
    val context = LocalContext.current
    val isMurid = userRole.equals("Murid", ignoreCase = true) && userMyKid.isNotBlank() && userMyKid != "000000000000"

    if (!isMurid) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("⛔", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("AKSES PERMAINAN DISEKAT", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Modul arked RPG ini merupakan pentaksiran khas untuk akaun Murid berdaftar sahaja.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Text("Kembali ke Papan Pemuka")
                        }
                    }
                }
            }
        }
        return
    }

    var selectedGame by remember { mutableStateOf<SiriGameModel?>(null) }
    var currentBankSoalan by remember { mutableStateOf(senaraiSiriGameAsal) }
    var isAutoLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isAutoLoading = true
        try {
            // 1. Muat turun dari GitHub (Statik)
            val githubQuizzes = withContext(Dispatchers.IO) {
                muatTurunSoalanJson("https://raw.githubusercontent.com/Barudy/rulaf-web/main/app/data/soalan.json")
            }

            // 2. Muat turun dari Supabase (Dinamik Guru)
            val supabaseQuizzes = withContext(Dispatchers.IO) {
                try {
                    RetrofitClient.api.getQuizzes()
                } catch (e: Exception) {
                    android.util.Log.e("RuLaF_API", "Ralat memuat turun dari Supabase: ${e.message}", e)
                    emptyList()
                }
            }

            val mergedList = mutableListOf<SiriGameModel>()
            if (githubQuizzes != null) mergedList.addAll(githubQuizzes)

            // 3. Gabungkan kuiz Supabase ke dalam senarai Android
            supabaseQuizzes.forEach { q ->
                try {
                    val soalanString = q.soalan?.toString() ?: "{}"
                    val parsedLevels = parseSupabaseQuizJson(soalanString)

                    if (parsedLevels.isNotEmpty()) {
                        mergedList.add(
                            SiriGameModel(
                                id = q.id.toString(),
                                tajuk = q.tajuk,
                                subjek = q.subjek,
                                deskripsi = q.deskripsi ?: "Kuiz pentaksiran arked di Supabase.",
                                ikon = if (q.subjek.contains("Ibadah", ignoreCase = true)) "🕌" else "📝",
                                kesukaran = q.darjah ?: "Semua",
                                levels = parsedLevels
                            )
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RuLaF_Parser", "Gagal parse kuiz id ${q.id}: ${e.message}")
                }
            }

            if (mergedList.isNotEmpty()) {
                currentBankSoalan = mergedList
            }
        } catch (e: Exception) {
            android.util.Log.e("RuLaF_Init", "Ralat keseluruhan inisialisasi: ${e.message}")
        } finally {
            isAutoLoading = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SmallTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "⚔️ RULAF RPG ARENA",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (isAutoLoading) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        }
                    }
                },
                navigationIcon = {
                    if (selectedGame == null) {
                        IconButton(onClick = onExit) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Exit")
                        }
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )

            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (selectedGame == null) {
                    GameMenuScreen(senaraiGame = currentBankSoalan, onGameSelect = { selectedGame = it })
                } else {
                    RpgBattleScreen(
                        game = selectedGame!!,
                        userMyKid = userMyKid,
                        userName = userName,
                        onBackToMenu = { selectedGame = null }
                    )
                }
            }
        }
    }
}

// =====================================================================
// MENU SENARAI MISI
// =====================================================================
@Composable
fun GameMenuScreen(senaraiGame: List<SiriGameModel>, onGameSelect: (SiriGameModel) -> Unit) {
    var query by remember { mutableStateOf("") }
    val ditapis = senaraiGame.filter {
        it.tajuk.contains(query, ignoreCase = true) || it.subjek.contains(query, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Cari Misi Pertarungan Jawi / Ibadah...") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(ditapis) { game ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onGameSelect(game) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(game.ikon, fontSize = 32.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(game.tajuk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(game.deskripsi, color = Color.Gray, fontSize = 11.sp, maxLines = 2)
                        }
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// =====================================================================
// ARENA PERTEMPURAN RPG (MOD PILIHAN & SUSUN KALIMAT)
// =====================================================================
@Composable
fun RpgBattleScreen(
    game: SiriGameModel,
    userMyKid: String,
    userName: String,
    onBackToMenu: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isBattleStarted by remember { mutableStateOf(false) }
    var selectedDifficulty by remember { mutableStateOf(DifficultyLevel.SEDERHANA) }
    var timeLeft by remember { mutableStateOf<Int?>(30) }
    var tulisanMode by remember { mutableStateOf(TulisanMode.DWI) }

    var currentLevel by remember { mutableStateOf(1) }
    val maxLevel = remember(game) { game.levels.keys.maxOrNull() ?: 1 }
    var currentIdx by remember { mutableStateOf(0) }

    val soalanList = remember(currentLevel, game) {
        game.levels[currentLevel] ?: game.levels.values.firstOrNull() ?: emptyList()
    }

    var playerHp by remember { mutableStateOf(100) }
    val maxPlayerHp = 100
    val isBossLevel = currentLevel == maxLevel
    val maxEnemyHp = if (isBossLevel) 150 else 100
    var enemyHp by remember { mutableStateOf(maxEnemyHp) }

    var battleLog by remember {
        mutableStateOf(if (isBossLevel) "⚠️ BOS AKHIR MUNCUL! Jawab salah = HP Bos pulih!" else "Pertarungan bermula! Serang musuh.")
    }

    var correctAnswers by remember { mutableStateOf(0) }
    var mistakes by remember { mutableStateOf(0) }
    var finalScore by remember { mutableStateOf(0) }

    var isAnswered by remember { mutableStateOf(false) }
    var selectedOptIdx by remember { mutableStateOf<Int?>(null) }
    var isGameOver by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }
    var showConfirmExitDialog by remember { mutableStateOf(false) }

    var leaderboardList by remember { mutableStateOf<List<LeaderboardDto>>(emptyList()) }
    var showLeaderboard by remember { mutableStateOf(false) }

    // 🧩 State Pengurusan Susun Atur Perkataan
    var susunWords by remember { mutableStateOf<List<String>>(emptyList()) }
    var availableChips by remember { mutableStateOf<List<ChipState>>(emptyList()) }

    val s = soalanList.getOrNull(currentIdx)

    val rawOptions = when (tulisanMode) {
        TulisanMode.RUMI -> {
            if (!s?.rumiOptions.isNullOrEmpty()) s!!.rumiOptions
            else if (s?.type == "susun_atur" && !s.aRumi.isNullOrBlank()) s.aRumi.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            else emptyList()
        }
        TulisanMode.JAWI, TulisanMode.DWI -> {
            if (!s?.jawiOptions.isNullOrEmpty()) s!!.jawiOptions
            else if (s?.type == "susun_atur" && !s.aJawi.isNullOrBlank()) s.aJawi.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            else s?.rumiOptions ?: emptyList()
        }
    }

    val correctAnswer = when (tulisanMode) {
        TulisanMode.RUMI -> s?.aRumi ?: ""
        TulisanMode.JAWI, TulisanMode.DWI -> {
            if (!s?.aJawi.isNullOrBlank()) s!!.aJawi else s?.aRumi ?: ""
        }
    }

    val options = remember(currentIdx, currentLevel, selectedDifficulty, tulisanMode, s) {
        if (selectedDifficulty == DifficultyLevel.SENANG) rawOptions else rawOptions.shuffled()
    }

    // Inisialisasi semula susunan perkataan setiap kali soalan bertukar
    LaunchedEffect(currentIdx, currentLevel, s, tulisanMode) {
        susunWords = emptyList()

        if (s?.type == "susun_atur" && rawOptions.isNotEmpty()) {
            var shuffledList = rawOptions.shuffled()
            var percubaan = 0

            // 🎯 Pastikan susunan TIDAK SAMA dengan susunan jawapan asal
            while (shuffledList == rawOptions && rawOptions.size > 1 && percubaan < 15) {
                shuffledList = rawOptions.shuffled()
                percubaan++
            }

            availableChips = shuffledList.mapIndexed { index, word ->
                ChipState(id = index, word = word, used = false)
            }
        } else {
            availableChips = emptyList()
        }
    }

    fun rekodPenaltiKeluar() {
        scope.launch(Dispatchers.IO) {
            try {
                val tarikhHariIni = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val payload = HantarKerajinanRequest(
                    tarikh = tarikhHariIni,
                    mykid = userMyKid,
                    subjek = game.subjek,
                    tugasan_siap = 0,
                    status_hadir = true
                )
                RetrofitClient.api.hantarRekodKerajinan(body = payload)
            } catch (_: Exception) {}
        }
    }

    BackHandler(enabled = isBattleStarted && !isVictory && !isGameOver) {
        showConfirmExitDialog = true
    }

    fun serang(jawapanTeks: String, numberOpt: Int, jawapanBetul: String) {
        if (isAnswered || isGameOver || isVictory) return

        selectedOptIdx = numberOpt
        isAnswered = true

        // Normalisasi teks untuk toleransi ruang kosong sintaks
        val normJawapan = jawapanTeks.trim().replace("\\s+".toRegex(), " ")
        val normBetul = jawapanBetul.trim().replace("\\s+".toRegex(), " ")
        val isCorrect = normJawapan.equals(normBetul, ignoreCase = true) && jawapanTeks != "[MASA TAMAT]"

        val damageDealt = kotlin.math.ceil(maxEnemyHp.toDouble() / maxOf(soalanList.size, 1)).toInt()
        val damageTaken = 25

        if (isCorrect) {
            correctAnswers++
            val bakiHp = maxOf(0, enemyHp - damageDealt)
            enemyHp = bakiHp
            battleLog = "💥 SERANGAN TEPAT! Musuh menerima $damageDealt kerosakan!"
        } else {
            mistakes++
            val bakiHero = maxOf(0, playerHp - damageTaken)
            playerHp = bakiHero

            if (jawapanTeks == "[MASA TAMAT]") {
                battleLog = "⏰ MASA TAMAT! Hero menerima serangan balas ($damageTaken dmg)!"
            } else if (isBossLevel) {
                enemyHp = maxEnemyHp
                battleLog = "❌ SALAH! Bos serang balas ($damageTaken dmg) & pulihkan HP penuh!"
            } else {
                battleLog = "❌ SALAH! Hero menerima serangan balas sebanyak $damageTaken kerosakan!"
            }

            if (bakiHero <= 0) {
                isGameOver = true
                battleLog = "💀 HERO TEWAS! Nyawa anda telah habis."
            }
        }
    }

    LaunchedEffect(currentIdx, currentLevel, isAnswered, isBattleStarted, isGameOver, isVictory) {
        if (!isBattleStarted || selectedDifficulty.timerSec == null || isAnswered || isGameOver || isVictory) {
            timeLeft = null
            return@LaunchedEffect
        }
        timeLeft = selectedDifficulty.timerSec
        while ((timeLeft ?: 0) > 0 && !isAnswered && !isGameOver && !isVictory) {
            kotlinx.coroutines.delay(1000L)
            timeLeft = (timeLeft ?: 1) - 1
        }
        if (timeLeft == 0 && !isAnswered && !isGameOver && !isVictory) {
            val qSemasa = soalanList.getOrNull(currentIdx)
            val jwpn = if (tulisanMode == TulisanMode.JAWI) qSemasa?.aJawi ?: "" else qSemasa?.aRumi ?: ""
            serang("[MASA TAMAT]", -1, jwpn)
        }
    }

    fun prosesKemenangan() {
        isVictory = true
        val baseScore = maxOf(0, (currentLevel * 100) + ((correctAnswers + 1) * 10) - (mistakes * 5))
        val skorTerkira = (baseScore * selectedDifficulty.multiplier).toInt()
        finalScore = skorTerkira

        scope.launch {
            val tarikhHariIni = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            try {
                val lbPayload = LeaderboardDto(
                    mykid = userMyKid,
                    nama_murid = userName,
                    skor = skorTerkira,
                    level_capai = currentLevel,
                    jawapan_betul = correctAnswers + 1,
                    jawapan_salah = mistakes,
                    tarikh = tarikhHariIni
                )
                withContext(Dispatchers.IO) { RetrofitClient.api.postLeaderboard(body = lbPayload) }

                val kerajinanPayload = HantarKerajinanRequest(
                    tarikh = tarikhHariIni,
                    mykid = userMyKid,
                    subjek = game.subjek,
                    tugasan_siap = 3,
                    status_hadir = false,
                    status_kehadiran = "latih_tubi", // Labelkan sebagai aktiviti latih tubi kendiri
                    catatan = "Latih Tubi Arked RPG (+3 Kerajinan)"
                )
                withContext(Dispatchers.IO) { RetrofitClient.api.hantarRekodKerajinan(body = kerajinanPayload) }

                val lbData = withContext(Dispatchers.IO) { RetrofitClient.api.getLeaderboard() }
                leaderboardList = lbData
                Toast.makeText(context, "🏆 Kemenangan & Bonus Direkodkan!", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {}
        }
    }

    fun maraPusingan() {
        if (currentIdx + 1 < soalanList.size) {
            currentIdx++
            isAnswered = false
            selectedOptIdx = null
            timeLeft = selectedDifficulty.timerSec
        } else {
            if (currentLevel < maxLevel && !isGameOver) {
                currentLevel++
                currentIdx = 0
                isAnswered = false
                selectedOptIdx = null
                playerHp = minOf(maxPlayerHp, playerHp + 30)
                val newEnemyHp = if (currentLevel == maxLevel) 150 else 100
                enemyHp = newEnemyHp
                timeLeft = selectedDifficulty.timerSec
            } else if (!isGameOver) {
                prosesKemenangan()
            }
        }
    }

    if (!isBattleStarted) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⚔️", fontSize = 42.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(game.tajuk, fontWeight = FontWeight.Black, fontSize = 16.sp, textAlign = TextAlign.Center)
                    Text(game.deskripsi, fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("PILIH TAHAP KESUKARAN PERTEMPURAN:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ArchBlue)
                    Spacer(modifier = Modifier.height(10.dp))

                    DifficultyLevel.values().forEach { diff ->
                        val isSelected = selectedDifficulty == diff
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { selectedDifficulty = diff },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) ArchBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) ArchBlue else Color.Gray.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(diff.label.uppercase(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(diff.desc, fontSize = 10.sp, color = Color.Gray)
                                }
                                if (isSelected) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = ArchBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            tulisanMode = when (selectedDifficulty) {
                                DifficultyLevel.SENANG -> TulisanMode.RUMI
                                DifficultyLevel.SUKAR -> TulisanMode.JAWI
                                DifficultyLevel.SEDERHANA -> TulisanMode.DWI
                            }
                            timeLeft = selectedDifficulty.timerSec
                            isBattleStarted = true
                        },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("⚔️ [ MULAKAN PERTEMPURAN ]", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = onBackToMenu) {
                        Text("Batal & Kembali ke Menu", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tahap $currentLevel / $maxLevel", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ArchBlue)
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(color = ArchBlue.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            text = "MOD: ${selectedDifficulty.label.uppercase()}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArchBlue,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                TextButton(
                    onClick = {
                        if (!isVictory && !isGameOver) showConfirmExitDialog = true
                        else onBackToMenu()
                    }
                ) {
                    Text("Keluar Misi", color = Color.Red, fontSize = 11.sp)
                }
            }
        }

        if (isBossLevel && !isGameOver && !isVictory) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color.Red)
                ) {
                    Text(
                        text = "⚠️ TAHAP BOS AKHIR: Kesilapan akan memulihkan HP Bos!",
                        color = Color.Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                }
            }
        }

        // Arena HP Hero vs Musuh
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🧙‍♂️ $userName", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("$playerHp/$maxPlayerHp HP", fontSize = 9.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = playerHp.toFloat() / maxPlayerHp.toFloat(),
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = SystemGreen,
                                trackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        }

                        Text(
                            text = "VS",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = ArchOrange,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isBossLevel) "🐉 Bos Ifrit" else "🛡️ Pendekar Bayang",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Text("$enemyHp/$maxEnemyHp HP", fontSize = 9.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = (enemyHp.toFloat() / maxEnemyHp.toFloat()).coerceIn(0f, 1f),
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = Color.Red,
                                trackColor = Color.Gray.copy(alpha = 0.3f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(color = Color.Gray.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = battleLog,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Soalan & Pilihan Jawapan
        if (!isGameOver && !isVictory && s != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Soalan ${currentIdx + 1} / ${soalanList.size}",
                        fontSize = 10.sp,
                        color = ArchBlue,
                        fontWeight = FontWeight.Bold
                    )

                    if (timeLeft != null && !isAnswered) {
                        Surface(
                            color = if ((timeLeft ?: 30) <= 5) Color.Red else ArchOrange.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "⏳ Baki: $timeLeft saat",
                                color = if ((timeLeft ?: 30) <= 5) Color.White else ArchOrange,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // 🖼️ PAPAR GAMBAR SOALAN BERBINGKAI
            if (!s.gambarUrl.isNullOrBlank() && s.gambarUrl != "null") {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, ArchBlue.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.03f)),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(s.gambarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Gambar Rangsangan Soalan",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }

            // Teks Soalan
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (tulisanMode == TulisanMode.JAWI || tulisanMode == TulisanMode.DWI) {
                            Text(
                                text = s.qJawi,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (tulisanMode == TulisanMode.RUMI || tulisanMode == TulisanMode.DWI) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = s.qRumi,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 🎯 LOGIK CABANG: SUSUN ATUR VS PILIHAN STANDARD
            if (s.type == "susun_atur") {
                val arahSusunan = if (tulisanMode == TulisanMode.RUMI) {
                    LayoutDirection.Ltr
                } else {
                    LayoutDirection.Rtl
                }

                item {
                    CompositionLocalProvider(LocalLayoutDirection provides arahSusunan) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Kotak Ayat Yang Sedang Disusun (Bermula dari Kanan untuk Jawi/Arab)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 65.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.5.dp, ArchBlue.copy(alpha = 0.6f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (susunWords.isEmpty()) {
                                        Text(
                                            text = if (tulisanMode == TulisanMode.RUMI)
                                                "Tekan perkataan di bawah untuk menyusun ayat..."
                                            else
                                                "تکن کلمه دباوه اونتوق مڽوسون ايات...",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    } else {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.Start,
                                            verticalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            susunWords.forEach { word ->
                                                Surface(
                                                    color = ArchBlue,
                                                    shape = RoundedCornerShape(6.dp),
                                                    shadowElevation = 2.dp,
                                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = word,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 15.sp,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 2. Cebisan Perkataan (Word Chips) Mengikut Arah RTL
                            FlowRow(
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            ) {
                                availableChips.forEach { chip ->
                                    Card(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .clickable(enabled = !chip.used && !isAnswered) {
                                                if (!chip.used && !isAnswered) {
                                                    susunWords = susunWords + chip.word
                                                    availableChips = availableChips.map {
                                                        if (it.id == chip.id) it.copy(used = true) else it
                                                    }
                                                }
                                            },
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (chip.used) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                            else MaterialTheme.colorScheme.surface
                                        ),
                                        border = BorderStroke(
                                            1.dp,
                                            if (chip.used) Color.Gray.copy(alpha = 0.2f) else ArchBlue.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = chip.word,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (chip.used) Color.Gray.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }

                            // 3. Butang Kawalan (Dikekalkan LTR supaya kedudukan butang konsisten)
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            if (!isAnswered) {
                                                susunWords = emptyList()
                                                availableChips = availableChips.map { it.copy(used = false) }
                                            }
                                        },
                                        enabled = !isAnswered && susunWords.isNotEmpty(),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("↺ Set Semula", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            val jawapanLengkap = susunWords.joinToString(" ")
                                            serang(jawapanLengkap, 0, correctAnswer)
                                        },
                                        enabled = !isAnswered && susunWords.isNotEmpty(),
                                        modifier = Modifier.weight(1.5f),
                                        colors = ButtonDefaults.buttonColors(containerColor = SystemGreen),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("⚔️ [ SAHKAN & SERANG ]", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Render Pilihan Standard 3 Butang
                items(options.indices.toList()) { idx ->
                    val optText = options[idx]
                    val isCorrect = optText.trim() == correctAnswer.trim()
                    val isSelected = selectedOptIdx == idx

                    val btnColor = when {
                        !isAnswered -> MaterialTheme.colorScheme.surface
                        isCorrect -> SystemGreen
                        isSelected -> Color.Red
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAnswered) {
                                serang(optText, idx, correctAnswer)
                            },
                        colors = CardDefaults.cardColors(containerColor = btnColor),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = optText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAnswered && (isCorrect || isSelected)) Color.White else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "[SERANG]",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isAnswered && (isCorrect || isSelected)) Color.White else ArchBlue
                            )
                        }
                    }
                }
            }

            if (isAnswered) {
                item {
                    Button(
                        onClick = { maraPusingan() },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ArchBlue)
                    ) {
                        Text("PUSINGAN SETERUSNYA ➡️", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        if (isGameOver) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("💀", fontSize = 54.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("HERO TEWAS!", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.Red)
                    Text("Nyawa anda telah habis diserang musuh.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBackToMenu) {
                        Text("Kembali ke Menu")
                    }
                }
            }
        }

        if (isVictory) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🏆", fontSize = 50.sp)
                        Text("MISI SELESAI!", fontWeight = FontWeight.Black, fontSize = 22.sp, color = SystemGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "$finalScore PTS",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = ArchBlue
                        )
                        Text(
                            text = "✓ Ganjaran +3 Markah Kerajinan Direkodkan!",
                            fontSize = 11.sp,
                            color = SystemGreen,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showLeaderboard = !showLeaderboard },
                                colors = ButtonDefaults.buttonColors(containerColor = ArchOrange)
                            ) {
                                Text(if (showLeaderboard) "Tutup Carta" else "🏅 Carta Juara", fontSize = 11.sp)
                            }
                            Button(
                                onClick = onBackToMenu,
                                colors = ButtonDefaults.buttonColors(containerColor = ArchBlue)
                            ) {
                                Text("Selesai", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            if (showLeaderboard) {
                item {
                    Text("🏅 10 PENCAPAI TERATAS", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ArchBlue)
                }

                items(leaderboardList.indices.toList()) { i ->
                    val entry = leaderboardList[i]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("#${i + 1}", fontWeight = FontWeight.Black, color = if (i == 0) ArchOrange else Color.Gray, fontSize = 12.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(entry.nama_murid, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Text("${entry.skor} PTS", fontWeight = FontWeight.Black, color = ArchBlue, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    if (showConfirmExitDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmExitDialog = false },
            title = { Text("⚠️ AMARAN DISIPLIN", fontWeight = FontWeight.Bold, color = Color.Red) },
            text = {
                Text("Jika anda keluar sekarang sebelum misi selesai, anda akan dikenakan PENALTI 0 MARKAH bagi tugasan kerajinan hari ini. Anda pasti mahu keluar?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        rekodPenaltiKeluar()
                        showConfirmExitDialog = false
                        onBackToMenu()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Ya, Keluar (Penalti 0)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmExitDialog = false }) {
                    Text("Batal & Teruskan")
                }
            }
        )
    }
}

// =====================================================================
// PARSER JSON SUPABASE & GITHUB (SOKONGAN 'pilihan' & 'susun_atur')
// =====================================================================
private fun ekstrakUrlGambar(jsonObj: JSONObject): String? {
    return when {
        jsonObj.has("img") && !jsonObj.isNull("img") -> jsonObj.optString("img").takeIf { it.isNotBlank() && it != "null" }
        jsonObj.has("gambar_url") && !jsonObj.isNull("gambar_url") -> jsonObj.optString("gambar_url").takeIf { it.isNotBlank() && it != "null" }
        jsonObj.has("gambarUrl") && !jsonObj.isNull("gambarUrl") -> jsonObj.optString("gambarUrl").takeIf { it.isNotBlank() && it != "null" }
        else -> null
    }
}

fun parseSupabaseQuizJson(soalanJsonStr: String): Map<Int, List<SoalanModel>> {
    val levelsMap = mutableMapOf<Int, List<SoalanModel>>()
    try {
        val rootObj = JSONObject(soalanJsonStr)
        for (levelNum in 1..3) {
            val levelArray = rootObj.optJSONArray("level$levelNum") ?: continue
            val soalanList = mutableListOf<SoalanModel>()
            for (i in 0 until levelArray.length()) {
                val soalanJson = levelArray.getJSONObject(i)
                val rumiObj = soalanJson.optJSONObject("rumi")
                val jawiObj = soalanJson.optJSONObject("jawi")
                val img = ekstrakUrlGambar(soalanJson)

                // Pengesanan Mod 'pilihan' atau 'susun_atur'
                val qType = soalanJson.optString(
                    "type",
                    if (rumiObj?.optString("q")?.contains("[Susun Atur]") == true ||
                        jawiObj?.optString("q")?.contains("[Susun Atur]") == true
                    ) "susun_atur" else "pilihan"
                )

                if (rumiObj != null && jawiObj != null) {
                    val rumiOpts = mutableListOf<String>()
                    val rumiOptsArr = rumiObj.optJSONArray("options")
                    if (rumiOptsArr != null) {
                        for (k in 0 until rumiOptsArr.length()) rumiOpts.add(rumiOptsArr.getString(k))
                    }

                    val jawiOpts = mutableListOf<String>()
                    val jawiOptsArr = jawiObj.optJSONArray("options")
                    if (jawiOptsArr != null) {
                        for (k in 0 until jawiOptsArr.length()) jawiOpts.add(jawiOptsArr.getString(k))
                    }

                    val aRumiText = rumiObj.optString("a", "")
                    val aJawiText = jawiObj.optString("a", "")

                    // Jika soalan susun atur dan options kosong, pecahkan ayat jawapan kepada perkataan
                    if (qType == "susun_atur") {
                        if (rumiOpts.isEmpty() && aRumiText.isNotBlank()) {
                            rumiOpts.addAll(aRumiText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() })
                        }
                        if (jawiOpts.isEmpty() && aJawiText.isNotBlank()) {
                            jawiOpts.addAll(aJawiText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() })
                        }
                    } else {
                        if (rumiOpts.isEmpty() && aRumiText.isNotBlank()) rumiOpts.add(aRumiText)
                        if (jawiOpts.isEmpty() && aJawiText.isNotBlank()) jawiOpts.add(aJawiText)
                    }

                    soalanList.add(
                        SoalanModel(
                            qRumi = rumiObj.optString("q", "Soalan Rumi"),
                            qJawi = jawiObj.optString("q", "سوءالن جاوي"),
                            rumiOptions = rumiOpts,
                            jawiOptions = jawiOpts,
                            aRumi = aRumiText,
                            aJawi = aJawiText,
                            gambarUrl = img,
                            type = qType
                        )
                    )
                }
            }
            if (soalanList.isNotEmpty()) levelsMap[levelNum] = soalanList
        }
    } catch (e: Exception) {
        android.util.Log.e("RuLaF_Parser", "Ralat parsing JSON kuiz Supabase: ${e.message}")
    }
    return levelsMap
}

suspend fun muatTurunSoalanJson(urlPath: String): List<SiriGameModel>? {
    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(urlPath).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonString = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) jsonString.append(line)
                reader.close()

                val rootObj = JSONObject(jsonString.toString())
                val senaraiSiri = mutableListOf<SiriGameModel>()
                val keys = rootObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val siriJson = rootObj.getJSONObject(key)
                    val levelsMap = mutableMapOf<Int, List<SoalanModel>>()

                    for (levelNum in 1..3) {
                        val levelArray = siriJson.optJSONArray("level$levelNum") ?: continue
                        val soalanList = mutableListOf<SoalanModel>()
                        for (i in 0 until levelArray.length()) {
                            val soalanJson = levelArray.getJSONObject(i)
                            val rumiObj = soalanJson.optJSONObject("rumi")
                            val jawiObj = soalanJson.optJSONObject("jawi")
                            val img = ekstrakUrlGambar(soalanJson)

                            val qType = soalanJson.optString(
                                "type",
                                if (rumiObj?.optString("q")?.contains("[Susun Atur]") == true ||
                                    jawiObj?.optString("q")?.contains("[Susun Atur]") == true
                                ) "susun_atur" else "pilihan"
                            )

                            if (rumiObj != null && jawiObj != null) {
                                val rumiOpts = mutableListOf<String>()
                                val rumiOptsArr = rumiObj.optJSONArray("options")
                                if (rumiOptsArr != null) {
                                    for (k in 0 until rumiOptsArr.length()) rumiOpts.add(rumiOptsArr.getString(k))
                                }

                                val jawiOpts = mutableListOf<String>()
                                val jawiOptsArr = jawiObj.optJSONArray("options")
                                if (jawiOptsArr != null) {
                                    for (k in 0 until jawiOptsArr.length()) jawiOpts.add(jawiOptsArr.getString(k))
                                }

                                val aRumiText = rumiObj.optString("a", "")
                                val aJawiText = jawiObj.optString("a", "")

                                if (qType == "susun_atur") {
                                    if (rumiOpts.isEmpty() && aRumiText.isNotBlank()) {
                                        rumiOpts.addAll(aRumiText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() })
                                    }
                                    if (jawiOpts.isEmpty() && aJawiText.isNotBlank()) {
                                        jawiOpts.addAll(aJawiText.trim().split("\\s+".toRegex()).filter { it.isNotBlank() })
                                    }
                                } else {
                                    if (rumiOpts.isEmpty() && aRumiText.isNotBlank()) rumiOpts.add(aRumiText)
                                    if (jawiOpts.isEmpty() && aJawiText.isNotBlank()) jawiOpts.add(aJawiText)
                                }

                                soalanList.add(
                                    SoalanModel(
                                        qRumi = rumiObj.optString("q", "Misi"),
                                        qJawi = jawiObj.optString("q", "ميسي"),
                                        rumiOptions = rumiOpts,
                                        jawiOptions = jawiOpts,
                                        aRumi = aRumiText,
                                        aJawi = aJawiText,
                                        gambarUrl = img,
                                        type = qType
                                    )
                                )
                            }
                        }
                        if (soalanList.isNotEmpty()) levelsMap[levelNum] = soalanList
                    }

                    senaraiSiri.add(
                        SiriGameModel(
                            id = key,
                            tajuk = siriJson.optString("tajuk", "Misi Baru"),
                            subjek = siriJson.optString("subjek", "Ibadah"),
                            deskripsi = siriJson.optString("deskripsi", ""),
                            ikon = if (siriJson.optString("subjek").contains("Ibadah", ignoreCase = true)) "🕌" else "📝",
                            kesukaran = "Sederhana",
                            levels = levelsMap
                        )
                    )
                }
                senaraiSiri
            } else null
        } catch (_: Exception) { null }
    }
}

val senaraiSiriGameAsal = listOf(
    SiriGameModel(
        id = "ibadah_solat_Jumaat",
        tajuk = "Misi Solat Jumaat Bahagian 1",
        subjek = "Ibadah",
        deskripsi = "Syarat Wajib, Syarat Sah, Dalil Pensyariatan dan Hikmah Solat Jumaat.",
        ikon = "🕌",
        kesukaran = "Sederhana",
        levels = mapOf(
            1 to listOf(
                SoalanModel(
                    "Apakah pengertian solat jumaat?",
                    "اڤاکه ڤڠرتين صلاة جمعة؟",
                    listOf(
                        "Solat yang wajib dilakukan oleh ahli Jumaat pada waktu Zohor seramai 40 orang",
                        "Solat sunat pada waktu Maghrib",
                        "Solat yang dilakukan secara bersendirian pada bila-bila masa"
                    ),
                    listOf(
                        "صلاة يڠ واجب دلاکوکن اوليه اهلي جمعة ڤد وقتو ظهر سراماي 40 اورڠ",
                        "صلاة سنة ڤد وقتو مغرب",
                        "صلاة يڠ دلاکوکن برsendiriان"
                    ),
                    "Solat yang wajib dilakukan oleh ahli Jumaat pada waktu Zohor seramai 40 orang",
                    "صلاة يڠ واجب دلاکوکن اوليه اهلي جمعة ڤد وقتو ظهر سراماي 40 اورڠ",
                    null,
                    "pilihan"
                )
            )
        )
    )
)