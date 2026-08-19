package com.albabacademy.rulafhub.ui.permainan

import androidx.compose.material3.ExperimentalMaterial3Api
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

// =====================================================================
// MODEL DATA PERMAINAN RULAFHUB
// =====================================================================
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

// =====================================================================
// KONSOL UTAMA & URUSAN AUTO-IMPORT SOALAN DARI WEB (AUTO-SYNC ENGINE)
// =====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuLaFGameEngineApp(isDarkMode: Boolean = true, onExit: () -> Unit = {}) {
    var selectedGame by remember { mutableStateOf<SiriGameModel?>(null) }
    var currentBankSoalan by remember { mutableStateOf(senaraiSiriGameAsal) }
    var isAutoLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 🚀 AUTO-LOAD: Memuat turun soalan secara masa nyata sebaik sahaja modul dibuka!
    LaunchedEffect(Unit) {
        isAutoLoading = true
        scope.launch {
            try {
                val hasil = muatTurunSoalanJson("https://raw.githubusercontent.com/Barudy/rulaf-web/main/app/data/soalan.json")
                if (hasil != null) {
                    currentBankSoalan = hasil
                    Toast.makeText(context, "☁️ Misi Arked dikemaskini secara masa nyata!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "🔌 Mod Luar Talian: Memuat data siri permainan dari cache tempatan.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Fallback silently
            } finally {
                isAutoLoading = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background // Fully integrated Dark/Light mode!
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Clean Top Bar without manual download cloud icon
            SmallTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🎮 RULAF CONSOLE v2.0",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (isAutoLoading) {
                            Spacer(modifier = Modifier.width(12.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (selectedGame == null) {
                    GameMenuScreen(
                        senaraiGame = currentBankSoalan,
                        onGameSelect = { selectedGame = it }
                    )
                } else {
                    GamePlayScreen(
                        game = selectedGame!!,
                        onBackToMenu = { selectedGame = null }
                    )
                }
            }
        }
    }
}

// =====================================================================
// FUNGSIONAL UTAMA: PARSER & PULLER JSON REMOTE (ONLINE ENGINE)
// =====================================================================
suspend fun muatTurunSoalanJson(urlPath: String): List<SiriGameModel>? {
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(urlPath)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonString = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    jsonString.append(line)
                }
                reader.close()

                val rootObj = JSONObject(jsonString.toString())
                val senaraiSiri = mutableListOf<SiriGameModel>()

                val keys = rootObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val siriJson = rootObj.getJSONObject(key)

                    val subjek = siriJson.optString("subjek", "Ibadah")
                    val tajuk = siriJson.optString("tajuk", "Misi Baru")
                    val deskripsi = siriJson.optString("deskripsi", "Ulangkaji interaktif.")

                    // Parse Levels
                    val levelsMap = mutableMapOf<Int, List<SoalanModel>>()
                    for (levelNum in 1..3) {
                        val levelArray = siriJson.optJSONArray("level$levelNum") ?: continue
                        val soalanList = mutableListOf<SoalanModel>()
                        for (i in 0 until levelArray.length()) {
                            val soalanJson = levelArray.getJSONObject(i)
                            val rumiObj = soalanJson.optJSONObject("rumi")
                            val jawiObj = soalanJson.optJSONObject("jawi")

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
                                        aJawi = jawiObj.getString("a")
                                    )
                                )
                            }
                        }
                        if (soalanList.isNotEmpty()) {
                            levelsMap[levelNum] = soalanList
                        }
                    }

                    senaraiSiri.add(
                        SiriGameModel(
                            id = key,
                            tajuk = tajuk,
                            subjek = subjek,
                            deskripsi = deskripsi,
                            ikon = if (subjek.contains("Ibadah")) "🕌" else "📝",
                            kesukaran = "Sederhana",
                            levels = levelsMap
                        )
                    )
                }
                senaraiSiri
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// Mock original fallback questions list
val senaraiSiriGameAsal = listOf(
    SiriGameModel(
        id = "ibadah_solat_Jumaat",
        tajuk = "Misi Solat Jumaat Bahagian 1",
        subjek = "Ibadah",
        deskripsi = "Uji kefahaman anda tentang Pengertian, Dalil Pensyariatan, Hikmah serta Syarat Wajib dan Syarat Sah Solat Jumaat.",
        ikon = "🕌",
        kesukaran = "Sederhana",
        levels = mapOf(
            1 to listOf(
                SoalanModel(
                    "Apakah pengertian solat jumaat?",
                    "اڤاکه ڤڠرتين صلاة جمعة؟",
                    listOf("Solat yang wajib dilakukan oleh ahli Jumaat yang cukup syarat-syaratnya pada waktu Zohor hari Jumaat seramai 40 orang ahli Jumaat", "Solat yang wajib dilakukan oleh ahli Khamis yang cukup syarat-syaratnya pada waktu Maghrib hari Khamis", "Solat yang sunat dilakukan seramai 40 orang"),
                    listOf("صلاة يڠ واجب دلاکوکن اوليه اهلي جمعة يڠ چوکوڤ شرط-شرطڽ ڤد وقتو ظهر هاري جمعة سراماي 40 اورڠ اهلي جمعة", "صلاة يڠ واجب دلاکوکن اوليه اهلي خميس", "صلاة يڠ سنة دلاکوکن"),
                    "Solat yang wajib dilakukan oleh ahli Jumaat yang cukup syarat-syaratnya pada waktu Zohor hari Jumaat seramai 40 orang ahli Jumaat",
                    "صلاة يڠ واجب دلاکوکن اوليه اهلي جمعة يڠ چوکوڤ شرط-شرطڽ ڤد وقتو ظهر هاري جمعة سراماي 40 اورڠ اهلي جمعة"
                )
            )
        )
    )
)

@Composable
fun GameMenuScreen(
    senaraiGame: List<SiriGameModel>,
    onGameSelect: (SiriGameModel) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val ditapis = senaraiGame.filter {
        it.tajuk.contains(query, ignoreCase = true) || it.subjek.contains(query, ignoreCase = true)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Cari Misi Permainan Jawi/Ibadah") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            singleLine = true
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(ditapis) { game ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGameSelect(game) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(game.ikon, fontSize = 36.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = game.tajuk,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = game.deskripsi,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// 🎮 REAL INTERACTIVE GAMEPLAY SCREEN (BEBAS PEPIJAT TERKELUAR!)
// =====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamePlayScreen(game: SiriGameModel, onBackToMenu: () -> Unit) {
    val context = LocalContext.current
    var hasSelectedSettings by remember { mutableStateOf(false) }
    var tulisanMode by remember { mutableStateOf(TulisanMode.DWI) }
    var selectedLevel by remember { mutableStateOf(1) }

    // Quiz game states
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var score by remember { mutableStateOf(0) }
    var answered by remember { mutableStateOf(false) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var isQuizCompleted by remember { mutableStateOf(false) }

    // Safely retrieve questions for selected level
    val questionsList = remember(selectedLevel, game) {
        game.levels[selectedLevel] ?: game.levels.values.firstOrNull() ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!hasSelectedSettings) {
            // STEP 1: CHOOSE PRE-GAME SETTINGS
            Text(
                "⚙️ Tetapan Misi Permainan",
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Pilih Mod Tulisan:", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(TulisanMode.DWI to "Dwi-Tulisan", TulisanMode.JAWI to "Jawi Sahaja", TulisanMode.RUMI to "Rumi Sahaja").forEach { (mode, label) ->
                            FilterChip(
                                selected = tulisanMode == mode,
                                onClick = { tulisanMode = mode },
                                label = { Text(label, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Pilih Tahap Peringkat:", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        game.levels.keys.sorted().forEach { lvl ->
                            FilterChip(
                                selected = selectedLevel == lvl,
                                onClick = { selectedLevel = lvl },
                                label = { Text("Tahap $lvl", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { hasSelectedSettings = true },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("▶️ MULAKAN MISI", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onBackToMenu,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Kembali")
            }
        } else if (isQuizCompleted) {
            // STEP 3: RESULT SCREEN
            Text("🏆 Misi Selesai!", fontSize = 28.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Markah Anda: $score / ${questionsList.size}", fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground)
            Text("Mata XP Diperoleh: ${score * 100} XP 🎉", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onBackToMenu,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Kembali Ke Menu Utama")
            }
        } else {
            // STEP 2: ACTIVE QUESTION SCREEN
            if (questionsList.isNotEmpty() && currentQuestionIndex < questionsList.size) {
                val soalan = questionsList[currentQuestionIndex]

                Text(
                    text = "Tahap $selectedLevel | Soalan ${currentQuestionIndex + 1} daripada ${questionsList.size}",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Render question depending on chosen Tulisan Mode
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (tulisanMode == TulisanMode.JAWI || tulisanMode == TulisanMode.DWI) {
                            Text(
                                text = soalan.qJawi,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        if (tulisanMode == TulisanMode.RUMI || tulisanMode == TulisanMode.DWI) {
                            Text(
                                text = soalan.qRumi,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Answer Options List
                val options = if (tulisanMode == TulisanMode.JAWI) soalan.jawiOptions else soalan.rumiOptions
                val correctAnswer = if (tulisanMode == TulisanMode.JAWI) soalan.aJawi else soalan.aRumi

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    options.forEach { opt ->
                        val isCorrect = opt == correctAnswer
                        val isSelected = opt == selectedAnswer
                        val btnColor = when {
                            !answered -> MaterialTheme.colorScheme.surfaceVariant
                            isCorrect -> Color(0xFF16A34A) // Green for correct answer
                            isSelected -> Color(0xFFDC2626) // Red for selected wrong answer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        Button(
                            onClick = {
                                if (!answered) {
                                    selectedAnswer = opt
                                    answered = true
                                    if (isCorrect) {
                                        score++
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = btnColor,
                                contentColor = if (answered && (isCorrect || isSelected)) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(opt, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (answered) {
                    Button(
                        onClick = {
                            if (currentQuestionIndex < questionsList.size - 1) {
                                currentQuestionIndex++
                                answered = false
                                selectedAnswer = null
                            } else {
                                isQuizCompleted = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = if (currentQuestionIndex < questionsList.size - 1) "Seterusnya ➡️" else "Selesai 🏆",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Fallback if level contains no questions
                Text("Tiada soalan ditemui untuk tahap ini.", color = Color.Gray)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { hasSelectedSettings = false }) {
                    Text("Pilih Tahap Lain")
                }
            }
        }
    }
}

@Composable
fun RuLaFGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
