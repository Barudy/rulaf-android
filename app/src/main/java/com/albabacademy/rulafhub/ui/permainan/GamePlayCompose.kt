package com.albabacademy.rulafhub.ui.permainan

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
@Composable
fun RuLaFGameEngineApp() {
    var selectedGame by remember { mutableStateOf<SiriGameModel?>(null) }
    var currentBankSoalan by remember { mutableStateOf(senaraiSiriGameAsal) }
    var showImportDialog by remember { mutableStateOf(false) }
    var isAutoLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 🚀 AUTO-LOAD: Memuat turun soalan secara masa nyata sebaik sahaja modul dibuka!
    LaunchedEffect(Unit) {
        isAutoLoading = true
        scope.launch {
            try {
                val hasil = muatTurunSoalanJson("https://rulaf-web.vercel.app/data/soalan.json")
                if (hasil != null) {
                    currentBankSoalan = hasil
                    Toast.makeText(context, "☁️ Misi Arked dikemaskini secara masa nyata!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "🔌 Mod Luar Talian: Memuat data siri permainan dari cache tempatan.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                // Fail silently or fallback
            } finally {
                isAutoLoading = false
            }
        }
    }

    RuLaFGameTheme {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🎮 RULAF CONSOLE v2.0",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF1793D1)
                            )
                            if (isAutoLoading) {
                                Spacer(modifier = Modifier.width(12.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF1793D1),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showImportDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Import JSON Web",
                                tint = Color(0xFF1793D1)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF171A21)
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF0F1419))
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

                // Dialog Pembina/Import JSON Manual (Fallback)
                if (showImportDialog) {
                    var inputUrl by remember { mutableStateOf("https://rulaf-web.vercel.app/data/soalan.json") }
                    var isDownloading by remember { mutableStateOf(false) }

                    AlertDialog(
                        onDismissRequest = { showImportDialog = false },
                        containerColor = Color(0xFF171A21),
                        title = {
                            Text(
                                "🛰️ IMPORT SOALAN MANUAL",
                                color = Color(0xFF1793D1),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Masukkan pautan soalan.json daripada pelayan Vercel atau repositori GitHub anda:",
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                                OutlinedTextField(
                                    value = inputUrl,
                                    onValueChange = { inputUrl = it },
                                    placeholder = { Text("Pautan URL .json") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 10.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF1793D1)
                                    )
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                enabled = !isDownloading,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White, disabledContainerColor = Color.Gray, disabledContentColor = Color.LightGray),
                                onClick = {
                                    scope.launch {
                                        isDownloading = true
                                        val hasil = muatTurunSoalanJson(inputUrl)
                                        isDownloading = false
                                        if (hasil != null) {
                                            currentBankSoalan = hasil
                                            Toast.makeText(context, "🎉 Berjaya mengimport ${hasil.size} siri permainan dari awan!", Toast.LENGTH_LONG).show()
                                            showImportDialog = false
                                        } else {
                                            Toast.makeText(context, "❌ Gagal mengimport. Sila semak pautan atau format JSON anda.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            ) {
                                Text(if (isDownloading) "MEMUAT..." else "[ IMPORT ]", color = Color.White, fontFamily = FontFamily.Monospace)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImportDialog = false }) {
                                Text("Batal", color = Color.Gray, fontFamily = FontFamily.Monospace)
                            }
                        }
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171A21)),
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
                                color = Color.White,
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
                            tint = Color(0xFF1793D1)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GamePlayScreen(game: SiriGameModel, onBackToMenu: () -> Unit) {
    var currentLevel by remember { mutableStateOf(1) }
    var score by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎮 Bermain: ${game.tajuk}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBackToMenu) {
            Text("Kembali ke Menu")
        }
    }
}

@Composable
fun RuLaFGameTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
