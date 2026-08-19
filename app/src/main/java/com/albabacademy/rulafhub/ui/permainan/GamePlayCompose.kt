@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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

// Warna Rasmi RuLaF
private val ArchBlue = Color(0xFF1793D1)
private val ArchOrange = Color(0xFFE95420)
private val SystemGreen = Color(0xFF10B981)

enum class TulisanMode { DWI, JAWI, RUMI }

data class SoalanModel(
    val qRumi: String,
    val qJawi: String,
    val rumiOptions: List<String>,
    val jawiOptions: List<String>,
    val aRumi: String,
    val aJawi: String,
    val gambarUrl: String? = null
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

    // 🔒 1. KUNCI KESELAMATAN: PENGGUNA BUKAN MURID DISEKAT
    if (!isMurid) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
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
                        Text(
                            text = "AKSES PERMAINAN DISEKAT",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Modul arked RPG ini merupakan pentaksiran khas untuk akaun Murid berdaftar sahaja. Pengguna luar, Guru, atau akaun tanpa MyKid sah tidak dibenarkan bermain.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onExit,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
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
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isAutoLoading = true
        scope.launch {
            try {
                val githubQuizzes = withContext(Dispatchers.IO) {
                    muatTurunSoalanJson("https://raw.githubusercontent.com/Barudy/rulaf-web/main/app/data/soalan.json")
                }
                val supabaseQuizzes = withContext(Dispatchers.IO) {
                    try { RetrofitClient.api.getQuizzes() } catch (e: Exception) { emptyList<QuizDto>() }
                }

                val mergedList = mutableListOf<SiriGameModel>()
                if (githubQuizzes != null) mergedList.addAll(githubQuizzes)

                supabaseQuizzes.forEach { q ->
                    try {
                        val parsedLevels = parseSupabaseQuizJson(q.soalan)
                        mergedList.add(
                            SiriGameModel(
                                id = q.id.toString(),
                                tajuk = q.tajuk,
                                subjek = q.subjek,
                                deskripsi = q.deskripsi ?: "Kuiz pentaksiran arked di Supabase.",
                                ikon = "📝",
                                kesukaran = q.darjah ?: "Semua",
                                levels = parsedLevels
                            )
                        )
                    } catch (_: Exception) {}
                }

                if (mergedList.isNotEmpty()) currentBankSoalan = mergedList
            } catch (_: Exception) {
            } finally {
                isAutoLoading = false
            }
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
// MENU SENARAI MISI PERMAINAN
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
// ARENA PERTEMPURAN RPG (TURN-BASED BATTLE ENGINE)
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

    // Status Pertarungan
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

    // Papan Pendahulu
    var leaderboardList by remember { mutableStateOf<List<LeaderboardDto>>(emptyList()) }
    var showLeaderboard by remember { mutableStateOf(false) }

    var tulisanMode by remember { mutableStateOf(TulisanMode.DWI) }

    // Fungsi Penalti Disiplin: Keluar sebelum tamat = 0 Markah Kerajinan
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

    // Tangkap butang fizikal 'Back' telefon
    BackHandler(enabled = !isVictory && !isGameOver) {
        showConfirmExitDialog = true
    }

    // Pengendalian Kemenangan Penuh
    fun prosesKemenangan() {
        isVictory = true
        val skorTerkira = maxOf(0, (currentLevel * 100) + ((correctAnswers + 1) * 10) - (mistakes * 5))
        finalScore = skorTerkira

        scope.launch {
            val tarikhHariIni = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            try {
                // 1. Tembak Rekod Leaderboard
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

                // 2. Ganjaran Bonus +3 Markah ke rekod_kerajinan_harian
                val kerajinanPayload = HantarKerajinanRequest(
                    tarikh = tarikhHariIni,
                    mykid = userMyKid,
                    subjek = game.subjek,
                    tugasan_siap = 3,
                    status_hadir = true
                )
                withContext(Dispatchers.IO) { RetrofitClient.api.hantarRekodKerajinan(body = kerajinanPayload) }

                // Muat turun senarai Top 10
                val lbData = withContext(Dispatchers.IO) { RetrofitClient.api.getLeaderboard() }
                leaderboardList = lbData
                Toast.makeText(context, "🏆 Kemenangan & Bonus +3 Kerajinan Direkodkan!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Disimpan secara luar talian.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Serangan Berasaskan Jawapan
    fun serang(jawapanTeks: String, numberOpt: Int, jawapanBetul: String) {
        if (isAnswered || isGameOver || isVictory) return

        selectedOptIdx = numberOpt
        isAnswered = true
        val isCorrect = jawapanTeks.trim() == jawapanBetul.trim()
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

            if (isBossLevel) {
                enemyHp = maxEnemyHp // Bos pulih HP penuh jika murid salah
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

    fun maraPusingan() {
        if (currentIdx + 1 < soalanList.size && enemyHp > 0) {
            currentIdx++
            isAnswered = false
            selectedOptIdx = null
        } else {
            if (currentLevel < maxLevel && !isGameOver) {
                currentLevel++
                currentIdx = 0
                isAnswered = false
                selectedOptIdx = null
                playerHp = minOf(maxPlayerHp, playerHp + 30)
                val newEnemyHp = if (currentLevel == maxLevel) 150 else 100
                enemyHp = newEnemyHp
            } else if (!isGameOver) {
                prosesKemenangan()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Amaran Keluar Manual (Penalti)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tahap $currentLevel / $maxLevel",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArchBlue
                )
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

        // Amaran Merah Peringkat Bos
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

        // ARENA BILAH HP HERO VS MUSUH
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
                        // Sisi Hero
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

                        // Sisi Musuh
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

        // KAWASAN SOALAN & PILIHAN SERANGAN
        if (!isGameOver && !isVictory && soalanList.isNotEmpty() && currentIdx < soalanList.size) {
            val s = soalanList[currentIdx]

            // Togol Tulisan di Tahap 1
            if (currentLevel == 1) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(TulisanMode.DWI to "Dwi-Tulisan", TulisanMode.JAWI to "Jawi", TulisanMode.RUMI to "Rumi").forEach { (m, l) ->
                            FilterChip(
                                selected = tulisanMode == m,
                                onClick = { tulisanMode = m },
                                label = { Text(l, fontSize = 9.sp) }
                            )
                        }
                    }
                }
            }

            // Paparan Imej Soalan (Coil AsyncImage)
            if (!s.gambarUrl.isNullOrBlank()) {
                item {
                    AsyncImage(
                        model = s.gambarUrl,
                        contentDescription = "Gambar Soalan",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )
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
                        Text(
                            text = "Soalan ${currentIdx + 1} / ${soalanList.size}",
                            fontSize = 10.sp,
                            color = ArchBlue,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
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

            // Butang Pilihan Serangan
            val options = if (tulisanMode == TulisanMode.JAWI) s.jawiOptions else s.rumiOptions
            val correctAnswer = if (tulisanMode == TulisanMode.JAWI) s.aJawi else s.aRumi

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

        // PAPARAN KEKALAHAN (GAME OVER)
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

        // PAPARAN KEMENANGAN & PAPAN PENDAHULU
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
                    Text(
                        text = "🏅 10 PENCAPAI TERATAS (LEADERBOARD)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ArchBlue
                    )
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
                                Text(
                                    text = "#${i + 1}",
                                    fontWeight = FontWeight.Black,
                                    color = if (i == 0) ArchOrange else Color.Gray,
                                    fontSize = 12.sp
                                )
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

    // Dialog Pengesahan Keluar (Penalti Disiplin)
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
// PARSER JSON SUPABASE & GITHUB RAW
// =====================================================================
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
                val img = soalanJson.optString("gambar_url", null)

                if (rumiObj != null && jawiObj != null) {
                    val rumiOpts = mutableListOf<String>()
                    val rumiOptsArr = rumiObj.getJSONArray("options")
                    for (k in 0 until rumiOptsArr.length()) rumiOpts.add(rumiOptsArr.getString(k))

                    val jawiOpts = mutableListOf<String>()
                    val jawiOptsArr = jawiObj.getJSONArray("options")
                    for (k in 0 until jawiOptsArr.length()) jawiOpts.add(jawiOptsArr.getString(k))

                    soalanList.add(
                        SoalanModel(
                            qRumi = rumiObj.getString("q"),
                            qJawi = jawiObj.getString("q"),
                            rumiOptions = rumiOpts,
                            jawiOptions = jawiOpts,
                            aRumi = rumiObj.getString("a"),
                            aJawi = jawiObj.getString("a"),
                            gambarUrl = img
                        )
                    )
                }
            }
            if (soalanList.isNotEmpty()) levelsMap[levelNum] = soalanList
        }
    } catch (_: Exception) {}
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
                            val img = soalanJson.optString("gambar_url", null)

                            if (rumiObj != null && jawiObj != null) {
                                val rumiOpts = mutableListOf<String>()
                                val rumiOptsArr = rumiObj.getJSONArray("options")
                                for (k in 0 until rumiOptsArr.length()) rumiOpts.add(rumiOptsArr.getString(k))

                                val jawiOpts = mutableListOf<String>()
                                val jawiOptsArr = jawiObj.getJSONArray("options")
                                for (k in 0 until jawiOptsArr.length()) jawiOpts.add(jawiOptsArr.getString(k))

                                soalanList.add(
                                    SoalanModel(
                                        qRumi = rumiObj.getString("q"),
                                        qJawi = jawiObj.getString("q"),
                                        rumiOptions = rumiOpts,
                                        jawiOptions = jawiOpts,
                                        aRumi = rumiObj.getString("a"),
                                        aJawi = jawiObj.getString("a"),
                                        gambarUrl = img
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
                            ikon = if (siriJson.optString("subjek").contains("Ibadah")) "🕌" else "📝",
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
                    null
                )
            )
        )
    )
)