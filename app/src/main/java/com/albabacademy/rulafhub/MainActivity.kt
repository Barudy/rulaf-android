@file:OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
package com.albabacademy.rulafhub

import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.albabacademy.rulafhub.data.remote.*
import com.albabacademy.rulafhub.utils.AuthHelper
import com.albabacademy.rulafhub.utils.AuthHelper.findActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.nio.charset.Charset
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import android.provider.OpenableColumns
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.style.TextOverflow
import com.albabacademy.rulafhub.ui.components.KalendarKehadiranMurid
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.withContext



// =====================================================================
// THEME CONFIGURATION (DWI-TEMA)
// =====================================================================
private val DarkBg = Color(0xFF0F1419)
private val DarkCard = Color(0xFF171A21)
private val LightBg = Color(0xFFF5F7FA)
private val LightCard = Color(0xFFFFFFFF)
private val ArchBlue = Color(0xFF1793D1)
private val ArchOrange = Color(0xFFE95420)
private val SystemGreen = Color(0xFF16A34A)
private val RuLaFBlue = Color(0xFF1793D1)

@Composable
fun RuLaFTheme(isDarkMode: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (isDarkMode) {
        darkColorScheme(
            primary = ArchBlue,
            background = DarkBg,
            surface = DarkCard,
            onBackground = Color(0xFFA5B2D9),
            onSurface = Color.White
        )
    } else {
        lightColorScheme(
            primary = ArchBlue,
            background = LightBg,
            surface = LightCard,
            onBackground = Color(0xFF333333),
            onSurface = Color(0xFF111111)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

@Composable
fun BetaWarningDialog(
    versiApp: String = "v2.0-beta",
    onBukaForum: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefKey = "has_seen_release_notes_$versiApp"
    val sharedPref = remember { context.getSharedPreferences("rulaf_app_prefs", Context.MODE_PRIVATE) }

    // Semak sama ada pengguna sudah pernah baca amaran untuk versi ini
    var showDialog by remember {
        mutableStateOf(!sharedPref.getBoolean(prefKey, false))
    }

    if (!showDialog) return

    AlertDialog(
        onDismissRequest = { /* Kunci dialog supaya pengguna wajib tekan butang faham */ },
        shape = RoundedCornerShape(14.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = "Amaran Beta",
                    tint = ArchOrange,
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = "VERSI PERCUBAAN AWAL",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = ArchOrange
                    )
                    Text(
                        text = "RuLaFHub Modul Rintis :: $versiApp",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = ArchOrange.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, ArchOrange.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Aplikasi ini sedang giat diuji (peringkat rintis). Sesetengah ciri pangkalan data mungkin mengalami perubahan dari semasa ke semasa.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(10.dp),
                        lineHeight = 16.sp
                    )
                }

                Text(
                    text = "📌 APA YANG BAHARU DALAM KELUARAN INI:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArchBlue
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("• Modul Kuiz RPG Susun Kalimat (RTL Jawi & Arab)", fontSize = 10.sp)
                    Text("• Muat naik fail terus ke storan awan (modul-rulaf)", fontSize = 10.sp)
                    Text("• Pengasingan hak milik folder repositori guru (Draft Box)", fontSize = 10.sp)
                    Text("• Pengiraan automatik Kumpulan RuLaF berasaskan rubrik 60/40", fontSize = 10.sp)
                }

                Divider(color = Color.Gray.copy(alpha = 0.2f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.BugReport,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Jika terjumpa sebarang ralat (bug), mohon laporkan segera di tab Forum dengan menyertakan tangkapan skrin.",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        lineHeight = 14.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Simpan status sudah baca
                    sharedPref.edit().putBoolean(prefKey, true).apply()
                    showDialog = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Saya Faham & Teruskan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    sharedPref.edit().putBoolean(prefKey, true).apply()
                    showDialog = false
                    onBukaForum()
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Buka Forum", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ArchBlue)
            }
        }
    )
}

data class PrestasiPaparan(
    val namaMurid: String = "Tiada Rekod",
    val kelasId: String = "-",
    val bulanTahun: String = "Ogos 2026",
    val tahapRulaf: String = "Belum Ditetapkan",
    val nilaiAkademik: String = "0.0%",
    val nilaiSahsiah: String = "0.0%",
    val skorAkhir: String = "0.00",
    val bacaanQuran: String = "-",
    val hafazan: String = "-"
)

fun dapatkanNamaFailFizikal(context: Context, uri: Uri): String {
    var namaFail: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) namaFail = cursor.getString(index)
            }
        }
    }
    return namaFail ?: uri.path?.substringAfterLast('/') ?: "BBM_${System.currentTimeMillis()}.pdf"
}

// 📤 2. Hantar fail secara raw stream terus ke Supabase Storage (modul-rulaf)
suspend fun muatNaikFailKeModulRulaf(
    context: Context,
    uri: Uri,
    namaAsal: String
): String? = withContext(Dispatchers.IO) {
    try {
        val projectUrl = "https://pzktjmtmkuicsjjjezjb.supabase.co"
        val fileExt = namaAsal.substringAfterLast('.', "pdf")
        val namaBersih = namaAsal.substringBeforeLast('.').replace("[^a-zA-Z0-9]".toRegex(), "_")
        val storagePath = "bbm-fail/${System.currentTimeMillis()}_$namaBersih.$fileExt"

        val uploadUrl = URL("$projectUrl/storage/v1/object/modul-rulaf/$storagePath")
        val connection = (uploadUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            // Gunakan kunci anon Supabase projek anda dari BuildConfig
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            setRequestProperty("Content-Type", mimeType)
            connectTimeout = 30000
            readTimeout = 30000
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            connection.outputStream.use { output ->
                input.copyTo(output)
            }
        }

        if (connection.responseCode in 200..299) {
            "$projectUrl/storage/v1/object/public/modul-rulaf/$storagePath"
        } else {
            android.util.Log.e("RuLaF_Storage", "Ralat Muat Naik HTTP: ${connection.responseCode}")
            null
        }
    } catch (e: Exception) {
        android.util.Log.e("RuLaF_Storage", "Ralat Sambungan: ${e.message}", e)
        null
    }
}

// 📷 Muat naik gambar profil ke Supabase Storage (bucket: profile-pictures)
suspend fun muatNaikGambarProfilKeStoran(
    context: Context,
    uri: Uri
): String? = withContext(Dispatchers.IO) {
    try {
        val projectUrl = "https://pzktjmtmkuicsjjjezjb.supabase.co"
        val namaAsal = dapatkanNamaFailFizikal(context, uri)
        val fileExt = namaAsal.substringAfterLast('.', "jpg").lowercase()
        val namaBersih = "profil_" + System.currentTimeMillis()
        val storagePath = "$namaBersih.$fileExt"

        val uploadUrl = URL("$projectUrl/storage/v1/object/profile-pictures/$storagePath")
        val connection = (uploadUrl.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            setRequestProperty("Content-Type", mimeType)
            connectTimeout = 30000
            readTimeout = 30000
        }

        context.contentResolver.openInputStream(uri)?.use { input ->
            connection.outputStream.use { output ->
                input.copyTo(output)
            }
        }

        if (connection.responseCode in 200..299) {
            "$projectUrl/storage/v1/object/public/profile-pictures/$storagePath"
        } else {
            android.util.Log.e("RuLaF_Profil", "Ralat Muat Naik HTTP: ${connection.responseCode}")
            null
        }
    } catch (e: Exception) {
        android.util.Log.e("RuLaF_Profil", "Ralat Muat Naik: ${e.message}", e)
        null
    }
}

fun tentukanTahapRuLaFColor(skor: Double): Pair<String, Color> {
    return when {
        skor >= 80.0 -> Pair("RuLaF Ta", Color(0xFF10B981))      // Hijau Emerald
        skor >= 60.0 -> Pair("RuLaF Ba", Color(0xFF1793D1))      // Biru RuLaF
        skor >= 40.0 -> Pair("RuLaF Alif", Color(0xFFF59E0B))    // Amber / Jingga
        else -> Pair("RuLaF Khas", Color(0xFFEF4444))            // Merah Khas
    }
}

fun kiraPrestasiHolistik(dto: MarkahMuridDto): PrestasiPaparan {
    val ujianBertulis = dto.ujian_bertulis ?: 0.0
    val markahJawi = dto.markah_jawi ?: 0.0
    val purataAkademik = ((ujianBertulis + markahJawi) / 200.0) * 100.0

    val akhlak = dto.akhlak ?: 0.0
    val kerajinan = dto.kerajinan_usaha ?: 0.0
    val kerjasama = dto.kerjasama_kumpulan ?: 0.0
    val hariHadir = dto.hari_hadir ?: dto.kehadiran ?: 0.0
    val jumlahHari = (dto.jumlah_hari_sekolah ?: 140.0).coerceAtLeast(1.0)

    val kehadiranSkala = ((hariHadir / jumlahHari) * 100.0) / 10.0
    val jumlahSahsiah = akhlak + kerajinan + kerjasama + kehadiranSkala
    val peratusSahsiah = (jumlahSahsiah / 40.0) * 100.0

    val skorKeseluruhan = (purataAkademik * 0.60) + (peratusSahsiah * 0.40)
    val (autoTahap, _) = tentukanTahapRuLaFColor(skorKeseluruhan)

    return PrestasiPaparan(
        namaMurid = dto.nama_murid ?: "Murid",
        kelasId = dto.kelas_id ?: "Kelas",
        bulanTahun = dto.bulan_tahun ?: "Ogos 2026",
        tahapRulaf = autoTahap, // 🔥 Auto-dijana, bukan bergantung pada teks lama DB
        nilaiAkademik = String.format(Locale.US, "%.1f", purataAkademik),
        nilaiSahsiah = String.format(Locale.US, "%.1f", peratusSahsiah),
        skorAkhir = String.format(Locale.US, "%.2f", skorKeseluruhan),
        bacaanQuran = dto.bacaan_quran ?: "-",
        hafazan = dto.hafazan ?: dto.hafazan ?: "-"
    )
}

fun ekstrakDataNfc(tag: Tag): String? {
    val ndef = Ndef.get(tag) ?: return null
    return try {
        ndef.connect()
        val ndefMessage = ndef.cachedNdefMessage ?: ndef.ndefMessage
        val record = ndefMessage.records.firstOrNull { it.tnf == NdefRecord.TNF_WELL_KNOWN }

        record?.let {
            val payload = it.payload
            val textEncoding = if ((payload[0].toInt() and 128) == 0) Charset.forName("UTF-8") else Charset.forName("UTF-16")
            val languageCodeLength = payload[0].toInt() and 51
            String(payload, languageCodeLength + 1, payload.size - languageCodeLength - 1, textEncoding)
        }
    } catch (e: Exception) {
        null
    } finally {
        try { ndef.close() } catch (_: Exception) {}
    }
}

// =====================================================================
// MAIN ACTIVITY ENTRY POINT
// =====================================================================
class MainActivity : FragmentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    var lastScannedMyKid by mutableStateOf<String?>(null)
    var showNfcActionSheet by mutableStateOf(false)
    var studentListForNfc by mutableStateOf<List<StudentDto>>(emptyList())
    var lastScannedPhone by mutableStateOf<String?>(null)
    var userRoleGlobal by mutableStateOf("")

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (userRoleGlobal.equals("Murid", ignoreCase = true)) {
            Toast.makeText(this, "⛔ Akses Ditolak: Hanya Guru dibenarkan mengimbas kehadiran!", Toast.LENGTH_SHORT).show()
            return
        }

        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {

            val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

            tag?.let {
                val rawPayload = ekstrakDataNfc(it)
                if (!rawPayload.isNullOrEmpty()) {
                    val pecahanData = rawPayload.split("|")
                    val mykidDiimbas = pecahanData[0].trim()
                    val noKecemasan = if (pecahanData.size > 1) pecahanData[1].trim() else ""

                    lastScannedMyKid = mykidDiimbas
                    lastScannedPhone = noKecemasan
                    showNfcActionSheet = true

                    Toast.makeText(this, "Berjaya Imbas Murid: $mykidDiimbas", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Kad kosong atau format NDEF tidak sah!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)

        setContent {
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("RuLaF_Prefs", Context.MODE_PRIVATE) }

            var themeMode by remember { mutableStateOf(sharedPrefs.getString("theme_mode", "system") ?: "system") }
            val isDarkMode = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            var isLoggedIn by remember { mutableStateOf(false) }
            var userRole by remember { mutableStateOf("") }

            LaunchedEffect(userRole) {
                userRoleGlobal = userRole
            }

            var loggedInUserEmail by remember { mutableStateOf("") }

            // Log Masuk Automatik Tempatan
            LaunchedEffect(Unit) {
                val isAutoLoginEnabled = sharedPrefs.getBoolean("auto_login_enabled", false)
                val savedEmail = sharedPrefs.getString("saved_email", "") ?: ""
                val savedRole = sharedPrefs.getString("saved_role", "") ?: ""
                val useBiometric = sharedPrefs.getBoolean("biometric_login_enabled", false)

                if (isAutoLoginEnabled && savedEmail.isNotEmpty() && savedRole.isNotEmpty()) {
                    if (useBiometric) {
                        AuthHelper.authenticateWithBiometric(this@MainActivity) { success ->
                            if (success) {
                                userRole = savedRole
                                loggedInUserEmail = savedEmail
                                isLoggedIn = true
                                Toast.makeText(context, "🎉 Selamat Kembali! Log Masuk Sidik Jari Berjaya.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        userRole = savedRole
                        loggedInUserEmail = savedEmail
                        isLoggedIn = true
                    }
                }
            }

            RuLaFTheme(isDarkMode = isDarkMode) {
                if (!isLoggedIn) {
                    LoginScreen(
                        onLoginSuccess = { role, email ->
                            userRole = role
                            loggedInUserEmail = email
                            isLoggedIn = true
                        }
                    )
                } else {
                    MainAppShell(
                        userRole = userRole,
                        userEmail = loggedInUserEmail,
                        themeMode = themeMode,
                        isDarkMode = isDarkMode,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            sharedPrefs.edit().putString("theme_mode", mode).apply()
                        },
                        onLogout = {
                            isLoggedIn = false
                            userRole = ""
                            loggedInUserEmail = ""
                            sharedPrefs.edit().putBoolean("auto_login_enabled", false).apply()
                        }
                    )
                }

                // 🔑 DIBAIKI: Hanya SATU dialog NFC dipanggil dan hanya dibenarkan untuk peranan GURU
                if (!userRole.equals("Murid", ignoreCase = true) && showNfcActionSheet && !lastScannedMyKid.isNullOrEmpty()) {
                    NfcActionBottomSheet(
                        mykid = lastScannedMyKid!!,
                        noTelefonCard = lastScannedPhone,
                        senaraiMurid = studentListForNfc,
                        onDismiss = {
                            showNfcActionSheet = false
                            lastScannedMyKid = null
                            lastScannedPhone = null
                        },
                        onSuccessSimpan = { }
                    )
                }
            }
        }
    }
}

// =====================================================================
// 🔑 LOGIN SCREEN (KUKUH & MENYOKONG EMEL PENTADBIR UTAMA)
// =====================================================================
@Composable
fun LoginScreen(onLoginSuccess: (String, String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isAuthenticating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "RuLaFHub",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-2).sp
        )
        Text(
            text = "Sistem Pengurusan & Pentaksiran Jawi",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Daftar Masuk Pengguna",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Alamat E-mel") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Kata Laluan") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val formattedEmail = emailInput.trim().lowercase()
                if (formattedEmail.isNotEmpty() && passwordInput.length >= 6) {
                    isAuthenticating = true
                    scope.launch {
                        try {
                            // 🔒 1. Sandaran Luar Talian (Ditambah sokongan emel Pentadbir Rasmi)
                            val isAdminOrTeacherEmail = formattedEmail == "guru@rulafhub.com" ||
                                    formattedEmail == "admin@rulafhub.com" ||
                                    formattedEmail.startsWith("admin") ||
                                    formattedEmail.contains("ismail") ||
                                    formattedEmail.contains("ust_ismail")

                            if (isAdminOrTeacherEmail && passwordInput == "rulaf2026") {
                                onLoginSuccess("Guru", formattedEmail)
                                Toast.makeText(context, "🎉 Log Masuk Pentadbir/Guru Berjaya!", Toast.LENGTH_SHORT).show()
                                return@launch
                            } else if (formattedEmail == "murid@rulafhub.com" && passwordInput == "rulaf2026") {
                                onLoginSuccess("Murid", formattedEmail)
                                Toast.makeText(context, "🎉 Log Masuk Murid Berjaya!", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            // 🌐 2. Pengesahan Atas Talian Melalui Supabase Auth
                            val authResponse = withContext(Dispatchers.IO) {
                                RetrofitClient.api.login(LoginBody(formattedEmail, passwordInput))
                            }

                            if (authResponse.isSuccessful && authResponse.body() != null) {
                                val profiles = withContext(Dispatchers.IO) {
                                    try {
                                        RetrofitClient.api.getUserProfile("eq.$formattedEmail")
                                    } catch (e: Exception) {
                                        emptyList<UserProfileDto>()
                                    }
                                }

                                val rawRole = profiles.firstOrNull()?.peranan?.trim() ?: ""

                                val resolvedRole = when {
                                    rawRole.equals("Guru", ignoreCase = true) ||
                                            rawRole.equals("Pendidik", ignoreCase = true) ||
                                            rawRole.equals("Admin", ignoreCase = true) -> "Guru"
                                    rawRole.equals("Murid", ignoreCase = true) -> "Murid"
                                    isAdminOrTeacherEmail -> "Guru"
                                    else -> "Murid"
                                }

                                onLoginSuccess(resolvedRole, formattedEmail)
                                Toast.makeText(context, "🎉 Selamat Datang $formattedEmail", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "❌ Kelayakan ditolak: E-mel atau kata laluan salah (Kod: ${authResponse.code()})!", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "❌ Ralat sambungan: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        } finally {
                            isAuthenticating = false
                        }
                    }
                } else {
                    Toast.makeText(context, "Sila isi alamat e-mel & kata laluan (min 6 aksara)!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isAuthenticating
        ) {
            Text(
                text = if (isAuthenticating) "MENYELIDIK IDENTITI..." else "LOG MASUK SISTEM",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

// =====================================================================
// 📱 MAIN APP SHELL
// =====================================================================
@Composable
fun MainAppShell(
    userRole: String,
    userEmail: String,
    themeMode: String,
    isDarkMode: Boolean,
    onThemeModeChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"
    var currentUserName by remember { mutableStateOf("Hero Murid") }
    var currentUserMyKid by remember { mutableStateOf("") }

    LaunchedEffect(userEmail) {
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.api.getUserProfile("eq.$userEmail")
            }
            if (response.isNotEmpty()) {
                val userRecord = response.first()
                currentUserName = userRecord.nama ?: "Hero Murid"
                currentUserMyKid = userRecord.mykid ?: ""
            }
        } catch (_: Exception) {}
    }

    Scaffold(
        bottomBar = {
            // 🔒 KUNCI: Sembunyikan navigasi bawah jika sedang bermain RPG
            if (currentRoute != "arked") {
                RuLaFBottomNavigationBar(navController, userRole)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    userRole = userRole,
                    userEmail = userEmail,
                    navController = navController,
                    onNavigateToSemakan = {
                        navController.navigate("dashboard")
                    }
                )
            }
            composable("arked") {
                com.albabacademy.rulafhub.ui.permainan.RuLaFGameEngineApp(
                    userRole = userRole,
                    userEmail = userEmail,
                    userMyKid = currentUserMyKid,
                    userName = currentUserName,
                    isDarkMode = isDarkMode,
                    onExit = { navController.navigate("dashboard") }
                )
            }
            // 🔑 DIBAIKI: Dwi-panggilan RepositoryScreen dipadamkan
            composable("repositori") {
                RepositoryScreen(userRole = userRole, userEmail = userEmail)
            }
            composable("forum") {
                ForumScreen(userRole, userEmail)
            }
            composable("profil") {
                ProfilScreen(
                    userRole = userRole,
                    userEmail = userEmail,
                    themeMode = themeMode,
                    isDarkMode = isDarkMode,
                    onThemeModeChange = onThemeModeChange,
                    onLogout = onLogout,
                    onNavigateToSemakan = { navController.navigate("dashboard") }
                )
            }
        }
        BetaWarningDialog(
            versiApp = "v1.7.12-Experimental/Daily",
            onBukaForum = {
                // Navigasi terus ke tab forum jika murid/guru mahu semak isu sedia ada
                navController.navigate("Forum")// Sesuaikan dengan penanda navigasi anda
            }
        )
    }
}



@Composable
fun RuLaFBottomNavigationBar(navController: NavHostController, userRole: String) {
    val isMurid = userRole.equals("Murid", ignoreCase = true)
    val items = if (isMurid) {
        listOf(
            BottomNavItem("Prestasi", "dashboard", Icons.Filled.Assessment),
            BottomNavItem("Permainan", "arked", Icons.Filled.PlayArrow),
            BottomNavItem("Repo BBM", "repositori", Icons.Filled.FolderShared),
            BottomNavItem("Forum", "forum", Icons.Filled.Forum),
            BottomNavItem("Profil", "profil", Icons.Filled.Person)
        )
    } else {
        listOf(
            BottomNavItem("Dashboard", "dashboard", Icons.Filled.Home),
            BottomNavItem("Permainan", "arked", Icons.Filled.PlayArrow),
            BottomNavItem("Repo BBM", "repositori", Icons.Filled.FolderShared),
            BottomNavItem("Forum", "forum", Icons.Filled.Forum),
            BottomNavItem("Profil", "profil", Icons.Filled.Person)
        )
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                selected = isSelected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ArchBlue,
                    selectedTextColor = ArchBlue,
                    indicatorColor = MaterialTheme.colorScheme.inverseOnSurface,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun InAppNoticeBanner(
    notisList: List<NotifikasiDto>,
    userRole: String = "Murid"
) {
    val context = LocalContext.current
    var dismissMap by remember { mutableStateOf(setOf<Long>()) }

    // Tapis mengikut peranan sasaran pengguna
    val senaraiPaparan = notisList.filter { notis ->
        !dismissMap.contains(notis.id) &&
                (notis.sasaran.equals("Semua", ignoreCase = true) || notis.sasaran.equals(userRole, ignoreCase = true))
    }

    if (senaraiPaparan.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        senaraiPaparan.forEach { notis ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, if (notis.kategori == "kemaskini") ArchOrange.copy(alpha = 0.5f) else ArchBlue.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Campaign,
                        contentDescription = "Hebahan",
                        tint = if (notis.kategori == "kemaskini") ArchOrange else ArchBlue,
                        modifier = Modifier.size(22.dp)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = notis.tajuk,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = (if (notis.kategori == "kemaskini") ArchOrange else ArchBlue).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(3.dp)
                            ) {
                                Text(
                                    text = (notis.kategori ?: "INFO").uppercase(),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (notis.kategori == "kemaskini") ArchOrange else ArchBlue,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = notis.mesej,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 15.sp
                        )

                        if (!notis.pautan_tindakan.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            TextButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(notis.pautan_tindakan)))
                                    } catch (_: Exception) {}
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Buka Tindakan", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ArchBlue)
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(12.dp), tint = ArchBlue)
                                }
                            }
                        }
                    }

                    IconButton(
                        onClick = { dismissMap = dismissMap + notis.id },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Tutup",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// 📊 DASHBOARD SCREEN
// =====================================================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    userRole: String,
    userEmail: String,
    navController: NavHostController,
    onNavigateToSemakan: () -> Unit = {},
) {
    val isGuru = userRole.equals("Guru", ignoreCase = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var studentList by remember { mutableStateOf<List<StudentDto>>(emptyList()) }
    var tahapMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var markahMap by remember { mutableStateOf<Map<String, MarkahMuridDto>>(emptyMap()) }
    var isSyncing by remember { mutableStateOf(false) }
    var senaraiKerajinanMurid by remember { mutableStateOf<List<RekodKerajinanDto>>(emptyList()) }

    // Rekod kehadiran kelas hari ini (MyKid -> RekodKerajinanDto)
    var rekodHariIniMap by remember { mutableStateOf<Map<String, RekodKerajinanDto>>(emptyMap()) }
    var leaderboardDashboard by remember { mutableStateOf<List<LeaderboardDto>>(emptyList()) }

    var paparanMurid by remember { mutableStateOf<PrestasiPaparan?>(null) }
    var statusMesejMurid by remember { mutableStateOf<String?>(null) }
    var isLoadingMurid by remember { mutableStateOf(false) }

    var filterDarjah by remember { mutableStateOf("Semua") }
    var filterTahap by remember { mutableStateOf("Semua") }
    var filterBulan by remember { mutableStateOf("Ogos 2026") }

    val tarikhHariIni = remember {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
    }

    val filteredStudents = studentList.filter { student ->
        val matchDarjah = when (filterDarjah) {
            "Darjah 3" -> student.kelas_id.contains("3", ignoreCase = true)
            "Darjah 5" -> student.kelas_id.contains("5", ignoreCase = true)
            else -> true
        }

        val tahapSebenar = tahapMap[student.mykid.trim()] ?: student.tahap ?: "RuLaF Ba"
        val matchTahap = when (filterTahap) {
            "Semua" -> true
            else -> tahapSebenar.contains(filterTahap.replace("RuLaF ", ""), ignoreCase = true)
        }

        matchDarjah && matchTahap
    }
    // 🔔 State Notifikasi & Dialog Pop-up
    var senaraiNotifikasi by remember { mutableStateOf<List<NotifikasiDto>>(emptyList()) }
    var showNotisDialog by remember { mutableStateOf(false) }
    var isMarkingNotisBaca by remember { mutableStateOf(false) }
    var notisDibacaIds by remember { mutableStateOf(loadNotisDibacaIds(context)) }

    // Bilangan notifikasi belum dibaca untuk lencana (badge)
    val senaraiBelumBaca = senaraiNotifikasi.filter { it.id !in notisDibacaIds }

    // Tandakan semua notifikasi terpapar sebagai telah dibaca
    fun tandakanSemuaDibaca() {
        val baru = (senaraiNotifikasi.map { it.id } + notisDibacaIds).toSet()
        notisDibacaIds = baru
        simpanNotisDibacaIds(context, baru)
    }

    // Tarik notifikasi aktif secara automatik bila skrin dibuka
    LaunchedEffect(Unit) {
        try {
            // Panggilan terus menggunakan default arguments dari ApiService
            val notis = withContext(Dispatchers.IO) {
                RetrofitClient.api.getActiveNotifications()
            }
            senaraiNotifikasi = notis.filter { it.is_aktif }
        } catch (e: Exception) {
            android.util.Log.e("RuLaF_Notis", "Ralat tarik notifikasi: ${e.message}", e)
        }
    }

    // Fungsi muat turun data kehadiran & murid
    fun muatSemulaDataGuru() {
        scope.launch {
            isSyncing = true
            try {
                val students = withContext(Dispatchers.IO) { RetrofitClient.api.getStudents() }
                (context as? MainActivity)?.studentListForNfc = students

                val allGrades = withContext(Dispatchers.IO) { RetrofitClient.api.getAllStudentGrades() }

                tahapMap = allGrades
                    .filter { !it.mykid.isNullOrBlank() && !it.tahap_rulaf.isNullOrBlank() }
                    .associate { it.mykid!!.trim() to it.tahap_rulaf!!.trim() }

                markahMap = allGrades
                    .filter { !it.mykid.isNullOrBlank() }
                    .associateBy { it.mykid!!.trim() }

                studentList = students
            } catch (_: Exception) {
            } finally {
                isSyncing = false
            }
        }
    }

    LaunchedEffect(userRole, userEmail) {
        if (isGuru) {
            muatSemulaDataGuru()
            try {
                val topPlayers = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getLeaderboard()
                }
                leaderboardDashboard = topPlayers.take(5)
            } catch (_: Exception) {}
        } else {
            isLoadingMurid = true
            statusMesejMurid = null
            paparanMurid = null
            try {
                val profiles = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getUserProfile("eq.$userEmail")
                }
                val myProfile = profiles.firstOrNull()
                val userMyKid = myProfile?.mykid?.trim()

                if (!userMyKid.isNullOrEmpty()) {
                    val grades = withContext(Dispatchers.IO) {
                        RetrofitClient.api.getStudentGrades("eq.$userMyKid")
                    }
                    if (grades.isNotEmpty()) {
                        // 🔥 FIX: Ambil rekod id tertinggi / paling terkini, bukan grades.first()
                        val rekodTerkini = grades.maxByOrNull { it.id ?: 0 } ?: grades.last()
                        paparanMurid = kiraPrestasiHolistik(rekodTerkini)
                    }

                    val rekodNfc = withContext(Dispatchers.IO) {
                        try {
                            RetrofitClient.api.getRekodKerajinanMurid("eq.$userMyKid")
                        } catch (_: Exception) {
                            emptyList<RekodKerajinanDto>()
                        }
                    }
                    senaraiKerajinanMurid = rekodNfc
                }
                val topPlayers = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getLeaderboard()
                }
                leaderboardDashboard = topPlayers.take(10)
            } catch (_: Exception) {
                statusMesejMurid = "Gagal memuat data markah daripada pelayan awan."
            } finally {
                isLoadingMurid = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ==============================================================
        // 🔔 1. TOP HEADER BAR (PROFIL KIRI + BUTANG LONCENG KANAN)
        // ==============================================================
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. KIRI: Avatar & Nama (Dihadkan dengan weight supaya loceng kekal stabil)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f) // 🔥 KUNCI UTAMA: Ambil baki ruang sahaja, jangan tolak loceng!
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {

                            navController.navigate("profil")
                        }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ArchBlue.copy(alpha = 0.15f),
                        border = BorderStroke(1.5.dp, ArchBlue),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (isGuru) "👨‍🏫" else "🧙‍♂️", fontSize = 22.sp)
                        }
                    }

                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        val namaTeks = if (isGuru) "Ustaz / Guru" else (paparanMurid?.namaMurid ?: userEmail)

                        Text(
                            text = namaTeks,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, // 🛑 Hadkan 1 baris
                            overflow = TextOverflow.Ellipsis // ✂️ Gantikan lebihan nama dengan '...'
                        )
                        Text(
                            text = if (isGuru) userEmail else (paparanMurid?.kelasId ?: "RuLaFHub Player"),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 2. KANAN: Butang Loceng (Sentiasa selamat di bucu kanan)
                Box(modifier = Modifier.size(46.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        IconButton(onClick = { showNotisDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifikasi",
                                tint = if (senaraiBelumBaca.isNotEmpty()) ArchBlue else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    if (senaraiBelumBaca.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Red,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.background),
                            modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .sizeIn(minWidth = 18.dp, minHeight = 18.dp)
                                    .padding(horizontal = 5.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (senaraiBelumBaca.size > 99) "99+" else "${senaraiBelumBaca.size}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        if (isGuru) {
            // Header Pentadbir
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        CountDownUpkkCard()
                        Text(
                            "🛠 PAPAN KAWALAN PENTADBIR :: RULAFHUB",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Log Masuk Sebagai: $userEmail",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }
            }

            // Grid Statistik Utama (Quick Actions dipadamkan)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardStatCard(modifier = Modifier.weight(1f), title = "MURID BERDAFTAR", value = "${studentList.size.coerceAtLeast(58)}", unit = "Orang", subtitle = "Darjah 3 & Darjah 5", icon = "🎓")
                        DashboardStatCard(modifier = Modifier.weight(1f), title = "PENGGUNA AKTIF", value = "5", unit = "🟢", subtitle = "Sedang menguji sistem", icon = "")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardStatCard(modifier = Modifier.weight(1f), title = "SIRI PERMAINAN", value = "2", unit = "Kuiz", subtitle = "Modular RPG Battle", icon = "🎮")
                        DashboardStatCard(modifier = Modifier.weight(1f), title = "SUMBANGAN BBM", value = "7", unit = "Bahan", subtitle = "Bahan sokongan terbuka", icon = "📁")
                    }
                }
            }

            // Bar Penapis Murid & Darjah
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Senarai Murid (${filteredStudents.size})",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("Ogos 2026", "Julai 2026").forEach { bulan ->
                                    FilterChip(
                                        selected = filterBulan == bulan,
                                        onClick = { filterBulan = bulan },
                                        label = { Text(bulan, fontSize = 9.sp, fontFamily = FontFamily.Monospace) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("PILIHAN DARJAH:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Semua", "Darjah 3", "Darjah 5").forEach { d ->
                                FilterChip(
                                    selected = filterDarjah == d,
                                    onClick = { filterDarjah = d },
                                    label = { Text(d, fontSize = 10.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("KUMPULAN RULAF:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Semua", "RuLaF Alif", "RuLaF Ba", "RuLaF Ta").forEach { t ->
                                FilterChip(
                                    selected = filterTahap == t,
                                    onClick = { filterTahap = t },
                                    label = { Text(t, fontSize = 10.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // SENARAI KAD REKOD MURID (DENGAN INDIKATOR WARNA & SELEKSI TIDAK HADIR)
            items(filteredStudents) { student ->
                val mykidBersih = student.mykid.trim()
                val tahapSebenar = tahapMap[mykidBersih] ?: student.tahap ?: "RuLaF Ba"

                var isExpanded by remember { mutableStateOf(false) }
                var isSendingData by remember { mutableStateOf(false) }

                // Status Kehadiran Hari Ini
                var statusSemasa by remember { mutableStateOf("belum_tanda") } // "hadir", "lewat", "sakit", "berkenyataan", "ponteng", "belum_tanda"
                var showStatusMenu by remember { mutableStateOf(false) }

                val senaraiSubjekKelas = remember(student.kelas_id) {
                    if (student.kelas_id.contains("5", ignoreCase = true)) {
                        listOf("Al-Quran", "Tauhid", "Ibadat", "Bahasa Arab")
                    } else {
                        listOf(
                            "Al-Quran", "Hafazan", "Tauhid", "Ibadat",
                            "Bahasa Arab", "Sirah", "Jawi", "Akhlak",
                            "Amalan Lazim", "KIJ"
                        )
                    }
                }

                var sasaranSubjekHariIni by remember { mutableStateOf(2) }
                val subjekSelesaiSet = remember { mutableStateListOf<String>() }

                // Muat status dan subjek sedia ada dari rekodHariIniMap
                LaunchedEffect(rekodHariIniMap[mykidBersih]) {
                    val rekod = rekodHariIniMap[mykidBersih]
                    if (rekod != null) {
                        statusSemasa = rekod.status_kehadiran ?: if (rekod.status_hadir) "hadir" else "ponteng"
                        if (rekod.subjek.isNotBlank() && rekod.subjek != "Hadir Kelas" && rekod.subjek != "Tiada Tugasan") {
                            val subjekLama = rekod.subjek.split(",").map { it.trim() }
                            subjekSelesaiSet.clear()
                            subjekSelesaiSet.addAll(subjekLama.filter { senaraiSubjekKelas.contains(it) })
                        }
                    }
                }

                val jumlahSiap = subjekSelesaiSet.size
                val peratusSiap = if (sasaranSubjekHariIni > 0) {
                    (jumlahSiap.toFloat() / sasaranSubjekHariIni.toFloat()) * 100f
                } else 0f

                val isHadirHariIni = statusSemasa == "hadir" || statusSemasa == "lewat"

                // Gaya Border & Warna Kad Berdasarkan Status
                val cardBorder = when (statusSemasa) {
                    "hadir" -> BorderStroke(1.5.dp, SystemGreen)
                    "lewat" -> BorderStroke(1.5.dp, Color(0xFF8B5CF6))
                    "sakit" -> BorderStroke(1.5.dp, Color(0xFF38BDF8))
                    "berkenyataan" -> BorderStroke(1.5.dp, ArchOrange)
                    "ponteng" -> BorderStroke(1.5.dp, Color(0xFFEF4444))
                    else -> if (isExpanded) BorderStroke(1.dp, ArchBlue) else null
                }

                val cardBg = when (statusSemasa) {
                    "hadir" -> SystemGreen.copy(alpha = 0.04f)
                    "lewat" -> Color(0xFF8B5CF6).copy(alpha = 0.04f)
                    "sakit" -> Color(0xFF38BDF8).copy(alpha = 0.04f)
                    "berkenyataan" -> ArchOrange.copy(alpha = 0.04f)
                    "ponteng" -> Color(0xFFEF4444).copy(alpha = 0.04f)
                    else -> MaterialTheme.colorScheme.surface
                }

                // Fungsi Menghantar Status Kehadiran / Subjek Bertahap
                fun hantarStatus(statusBaru: String, subjekTeks: String = "") {
                    if (isSendingData) return
                    isSendingData = true
                    scope.launch {
                        try {
                            val isHadirBool = statusBaru == "hadir" || statusBaru == "lewat"
                            val payloadSubjek = if (subjekTeks.isNotBlank()) {
                                subjekTeks
                            } else if (subjekSelesaiSet.isNotEmpty()) {
                                subjekSelesaiSet.joinToString(", ")
                            } else {
                                if (isHadirBool) "Hadir Kelas" else "Tidak Hadir ($statusBaru)"
                            }

                            val payload = HantarKerajinanRequest(
                                tarikh = tarikhHariIni,
                                mykid = mykidBersih,
                                subjek = payloadSubjek,
                                tugasan_siap = if (isHadirBool) jumlahSiap else 0,
                                status_hadir = isHadirBool,
                                status_kehadiran = statusBaru,
                                catatan = "Direkod guru melalui konsol mudah alih"
                            )

                            val res = withContext(Dispatchers.IO) {
                                RetrofitClient.api.hantarRekodKerajinan(body = payload)
                            }

                            if (res.isSuccessful) {
                                statusSemasa = statusBaru
                                Toast.makeText(context, "✓ Status: ${student.nama_murid} [$statusBaru] disimpan!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Ralat: Kod ${res.code()}", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Ralat sambungan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        } finally {
                            isSendingData = false
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(10.dp),
                    border = cardBorder
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = student.nama_murid,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "MyKid: ${student.mykid} | ${student.kelas_id}",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )

                                // Lencana Status Visual
                                if (statusSemasa != "belum_tanda") {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = when (statusSemasa) {
                                            "hadir" -> "● HADIR HARI INI"
                                            "lewat" -> "● HADIR LEWAT"
                                            "sakit" -> "● CUTI SAKIT / MC"
                                            "berkenyataan" -> "● CUTI KEBENARAN"
                                            "ponteng" -> "● TIDAK HADIR / TH"
                                            else -> ""
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (statusSemasa) {
                                            "hadir" -> SystemGreen
                                            "lewat" -> Color(0xFF8B5CF6)
                                            "sakit" -> Color(0xFF38BDF8)
                                            "berkenyataan" -> ArchOrange
                                            else -> Color(0xFFEF4444)
                                        }
                                    )
                                }
                            }

                            // Butang Pemilih Status Kehadiran Pantas
                            Box {
                                Button(
                                    onClick = { showStatusMenu = true },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when (statusSemasa) {
                                            "hadir" -> SystemGreen
                                            "lewat" -> Color(0xFF8B5CF6)
                                            "sakit" -> Color(0xFF38BDF8)
                                            "berkenyataan" -> ArchOrange
                                            "ponteng" -> Color(0xFFEF4444)
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    enabled = !isSendingData
                                ) {
                                    Text(
                                        text = when (statusSemasa) {
                                            "hadir" -> "HADIR ✓"
                                            "lewat" -> "LEWAT"
                                            "sakit" -> "MC"
                                            "berkenyataan" -> "IZIN"
                                            "ponteng" -> "TH"
                                            else -> "+ STATUS"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (statusSemasa == "belum_tanda") MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                                    )
                                }

                                // Menu Lungsur Menanda Kehadiran / Ketidakhadiran
                                DropdownMenu(
                                    expanded = showStatusMenu,
                                    onDismissRequest = { showStatusMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("🟢 Hadir Tepat (H)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        onClick = { showStatusMenu = false; hantarStatus("hadir") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🟣 Hadir Lewat (L)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        onClick = { showStatusMenu = false; hantarStatus("lewat") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🔵 Cuti Sakit / MC (S)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        onClick = { showStatusMenu = false; hantarStatus("sakit") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🟠 Berkenyataan / Cuti (B)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        onClick = { showStatusMenu = false; hantarStatus("berkenyataan") }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("🔴 Ponteng / Tiada Sebab (TH)", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                        onClick = { showStatusMenu = false; hantarStatus("ponteng") }
                                    )
                                }
                            }
                        }

                        // Bahagian Perincian Subjek (Bila Kad Ditekan)
                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Divider(color = Color.Gray.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Jantina: ${student.jantina}", fontSize = 11.sp, color = Color.Gray)
                                    Text(
                                        "Penjaga: ${student.no_tel?.ifBlank { "Tiada No. Tel" } ?: "Tiada No. Tel"}",
                                        fontSize = 11.sp,
                                        color = ArchBlue,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Pemilihan Sasaran & Subjek
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SASARAN HARI INI:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ArchBlue)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(2, 3).forEach { sasaran ->
                                            FilterChip(
                                                selected = sasaranSubjekHariIni == sasaran,
                                                onClick = { sasaranSubjekHariIni = sasaran },
                                                label = { Text("$sasaran Subjek", fontSize = 9.sp) }
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text("TANDAKAN SUBJEK YANG TELAH SIAP:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))

                                // Grid Subjek Dinamik (Boleh ditanda bertahap)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    senaraiSubjekKelas.forEach { subjek ->
                                        val isChecked = subjekSelesaiSet.contains(subjek)
                                        FilterChip(
                                            selected = isChecked,
                                            onClick = {
                                                if (isChecked) {
                                                    subjekSelesaiSet.remove(subjek)
                                                } else {
                                                    if (subjekSelesaiSet.size < sasaranSubjekHariIni) {
                                                        subjekSelesaiSet.add(subjek)
                                                    } else {
                                                        Toast.makeText(context, "Maksimum $sasaranSubjekHariIni subjek sahaja!", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            label = { Text(subjek, fontSize = 10.sp) },
                                            leadingIcon = if (isChecked) {
                                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                            } else null
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Butang Simpan Kemajuan Subjek Terkini
                                Button(
                                    onClick = {
                                        val statusBaru = if (statusSemasa == "belum_tanda") "hadir" else statusSemasa
                                        hantarStatus(statusBaru, subjekSelesaiSet.joinToString(", "))
                                    },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                                    shape = RoundedCornerShape(4.dp),
                                    enabled = !isSendingData
                                ) {
                                    Text(
                                        text = if (isSendingData) "MENYIMPAN..." else "[ SIMPAN KEMAJUAN SUBJEK ($jumlahSiap/$sasaranSubjekHariIni) ]",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Ringkasan Pentaksiran Holistik 60/40
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Kumpulan Diselaraskan:", fontSize = 10.sp, color = Color.Gray)
                                            Text(text = tahapSebenar, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ArchBlue)
                                        }

                                        val rekodMurid = markahMap[mykidBersih]
                                        val hariHadir = rekodMurid?.hari_hadir ?: rekodMurid?.kehadiran ?: 0.0
                                        val jumlahHari = (rekodMurid?.jumlah_hari_sekolah ?: 140.0).coerceAtLeast(1.0)
                                        val peratusKehadiran = (hariHadir / jumlahHari) * 100.0
                                        val prestasi = rekodMurid?.let { kiraPrestasiHolistik(it) }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text("KEHADIRAN & SAHSIAH HOLISTIK", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ArchBlue, fontFamily = FontFamily.Monospace)
                                                    Text("Kumulatif (Ogos 2026)", fontSize = 9.sp, color = Color.Gray)
                                                }

                                                Spacer(modifier = Modifier.height(6.dp))

                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Surface(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp)) {
                                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text("KEHADIRAN", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                                            Text("${hariHadir.toInt()}/${jumlahHari.toInt()} Hari", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                            Text(String.format("%.1f%%", peratusKehadiran), fontSize = 9.sp, color = if (peratusKehadiran >= 85.0) SystemGreen else ArchOrange, fontWeight = FontWeight.Bold)
                                                        }
                                                    }

                                                    Surface(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp)) {
                                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text("SAHSIAH (40%)", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                                            Text(prestasi?.nilaiSahsiah ?: "0.0%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ArchBlue)
                                                            Text("Skala REDF", fontSize = 9.sp, color = Color.Gray)
                                                        }
                                                    }

                                                    Surface(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(4.dp)) {
                                                        Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Text("PURATA AKHIR", fontSize = 8.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                                            Text("${prestasi?.skorAkhir ?: "0.00"}%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = if ((prestasi?.skorAkhir?.toDoubleOrNull() ?: 0.0) >= 80.0) SystemGreen else ArchBlue)
                                                            Text("60/40 Holistik", fontSize = 9.sp, color = Color.Gray)
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
                }
            }
        } else {
            // Paparan Murid / Penjaga
            item {
                if (isLoadingMurid) {

                    Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (statusMesejMurid != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("⚠️ Status Semakan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ArchOrange)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(statusMesejMurid ?: "", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                        }
                    }
                } else if (paparanMurid != null) {
                    val p = paparanMurid!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        CountDownUpkkCard()
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Semakan Prestasi RuLaF", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            Text("[ DASHBOARD PINTAR PELAJAR / PENJAGA ]", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Text("[+] REKOD DIJUMPAI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ArchBlue, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("NAMA MURID:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(p.namaMurid, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("KELAS:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(p.kelasId, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("BULAN / TAHUN KEMAS KINI:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    Text(p.bulanTahun, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("KUMPULAN RULAF:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

                                    // 🔥 FIX: Warna dinamik mengikut tahap sebenar
                                    val warnaTahap = when (p.tahapRulaf) {
                                        "RuLaF Ta" -> Color(0xFF10B981)
                                        "RuLaF Ba" -> Color(0xFF1793D1)
                                        "RuLaF Alif" -> Color(0xFFF59E0B)
                                        else -> Color(0xFFEF4444)
                                    }

                                    Text(
                                        text = p.tahapRulaf,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = warnaTahap
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("PENCAPAIAN HOLISTIK (60/40)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ArchBlue, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ArchBlue.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("GRED PURATA KUMULATIF:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        Text("60% Akademik + 40% Sahsiah", fontSize = 9.sp, color = Color.Gray)
                                    }
                                    Text("${p.skorAkhir}%", fontSize = 24.sp, fontWeight = FontWeight.Black, color = ArchBlue)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetricBox(modifier = Modifier.weight(1f), title = "AKADEMIK", value = p.nilaiAkademik)
                                MetricBox(modifier = Modifier.weight(1f), title = "SAHSIAH", value = p.nilaiSahsiah)
                                MetricBox(modifier = Modifier.weight(1f), title = "BACAAN QURAN", value = p.bacaanQuran)
                                MetricBox(modifier = Modifier.weight(1f), title = "UJIAN HAFAZAN", value = p.hafazan)
                            }

                            KerajinanHeatmapCompose(
                                senaraiRekod = senaraiKerajinanMurid,
                                jumlahTugasanLalai = 2
                            )
                        }
                    }
                    KalendarKehadiranMurid(senaraiRekod = senaraiKerajinanMurid)
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🏆 CARTA JUARA RULAF (TOP 10)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = ArchOrange
                            )
                            Text(
                                text = "Arked RPG",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(color = Color.Gray.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(8.dp))

                        if (leaderboardDashboard.isEmpty()) {
                            Text("Tiada rekod kejuaraan lagi. Jadilah juara pertama!", fontSize = 10.sp, color = Color.Gray)
                        } else {
                            leaderboardDashboard.forEachIndexed { idx, player ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Bahagian Nama Murid
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = when (idx) {
                                                0 -> "🥇"
                                                1 -> "🥈"
                                                2 -> "🥉"
                                                else -> "#${idx + 1}"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = player.nama_murid,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1, // 🛑 Elak tolak markah
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    // Bahagian Skor (Kekal utuh di kanan tanpa patah baris)
                                    Text(
                                        text = "${player.skor} PTS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = ArchBlue
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    // ==============================================================
    // 📩 POPUP PETI NOTIFIKASI & HEBAHAN RULAFHUB
    // ==============================================================
    if (showNotisDialog) {
        AlertDialog(
            onDismissRequest = { showNotisDialog = false },
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = ArchBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Peti Notifikasi & Hebahan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            },
            text = {
                if (senaraiNotifikasi.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📭", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tiada notifikasi baharu buat masa ini.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(senaraiNotifikasi) { notis ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.background
                                ),
                                border = BorderStroke(1.dp, ArchBlue.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = notis.tajuk,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = ArchBlue
                                        )
                                        Surface(
                                            color = ArchBlue.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = (notis.kategori ?: "INFO").uppercase(),
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = ArchBlue,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = notis.mesej,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 15.sp
                                    )

                                    if (!notis.pautan_tindakan.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        TextButton(
                                            onClick = {
                                                try {
                                                    context.startActivity(
                                                        Intent(Intent.ACTION_VIEW, Uri.parse(notis.pautan_tindakan))
                                                    )
                                                } catch (_: Exception) {}
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("Buka Pautan ➔", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ArchBlue)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (senaraiBelumBaca.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                tandakanSemuaDibaca()
                                Toast.makeText(context, "Semua notifikasi ditandakan sebagai dibaca.", Toast.LENGTH_SHORT).show()
                            },
                            enabled = !isMarkingNotisBaca
                        ) {
                            Text("✓ Tanda Sudah Dibaca", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ArchBlue)
                        }
                    }
                    Button(
                        onClick = { showNotisDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Tutup", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        )
    }
}

// =====================================================================
// COUNTDOWN UPKK
// =====================================================================


@Composable
fun CountDownUpkkCard(modifier: Modifier = Modifier) {
    // Tetapan Sasaran Peperiksaan: 2 November 2026, 8:00 AM (Waktu Malaysia)
    val targetMillis = remember {
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur")).apply {
            set(2026, Calendar.NOVEMBER, 2, 8, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    var bakiMasaMillis by remember { mutableStateOf(targetMillis - System.currentTimeMillis()) }

    // Gelung kemas kini setiap 1 saat
    LaunchedEffect(Unit) {
        while (true) {
            bakiMasaMillis = (targetMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            delay(1000L)
        }
    }

    val totalSaat = bakiMasaMillis / 1000
    val hari = totalSaat / (60 * 60 * 24)
    val jam = (totalSaat % (60 * 60 * 24)) / (60 * 60)
    val minit = (totalSaat % (60 * 60)) / 60
    val saat = totalSaat % 60

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⏳ DETIK PEPERIKSAAN UPKK 2026",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = RuLaFBlue,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (bakiMasaMillis <= 0) {
                Text(
                    text = "🎉 Peperiksaan UPKK Bermula! Bittaufiq Wannajah!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UnitMasaBox(nilai = "$hari", label = "HARI")
                    Text(":", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                    UnitMasaBox(nilai = String.format("%02d", jam), label = "JAM")
                    Text(":", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                    UnitMasaBox(nilai = String.format("%02d", minit), label = "MINIT")
                    Text(":", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                    UnitMasaBox(nilai = String.format("%02d", saat), label = "SAAT", warnaTeks = RuLaFBlue)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Tarikh Peperiksaan: 2 November 2026",
                fontSize = 10.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun UnitMasaBox(
    nilai: String,
    label: String,
    warnaTeks: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.width(55.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = nilai,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = warnaTeks,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =====================================================================
// 📁 REPOSITORY SCREEN (PENGANJURAN STACKED FOLDER BERHIRARKI)
// =====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryScreen(userRole: String, userEmail: String = "Guru") {
    val isUserAdmin = userEmail.contains("admin", ignoreCase = true) ||
            userEmail.contains("ust_ismail", ignoreCase = true) ||
            userEmail.contains("ismail", ignoreCase = true)
    val isGuru = userRole.equals("Guru", ignoreCase = true) || isUserAdmin

    var bbmList by remember { mutableStateOf<List<BbmDto>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 🧭 Navigasi & Penapis
    var currentFolderId by remember { mutableStateOf<Int?>(null) }
    val folderHistory = remember { mutableStateListOf<Int>() }
    var selectedDarjah by remember { mutableStateOf<String?>(null) }
    var selectedSubjek by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var filterKoleksiSaya by remember { mutableStateOf(false) }

    // Dialog Tambah Bahan
    var showDialogTambah by remember { mutableStateOf(false) }
    var jenisSumbangan by remember { mutableStateOf("fail") }
    var tajukInput by remember { mutableStateOf("") }
    var pautanInput by remember { mutableStateOf("") }
    var subjekInput by remember { mutableStateOf("Jawi") }
    var darjahInput by remember { mutableStateOf("Darjah 3") }
    var topikInput by remember { mutableStateOf("") }
    var readmeInput by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    val senaraiPilihanDarjah = listOf("Darjah 1", "Darjah 2", "Darjah 3", "Darjah 4", "Darjah 5", "UPKK")
    val senaraiPilihanSubjek = listOf("Jawi", "Ibadat", "Bahasa Arab", "Sirah", "Tauhid", "Adab")

    // 🔒 State Khas Muat Naik Dokumen & Kunci Pautan
    var pautanTerkunci by remember { mutableStateOf(false) }
    var isUploadingFileToStorage by remember { mutableStateOf(false) }

    // 📂 Pengurus Pemilihan Fail Dari Telefon
    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isUploadingFileToStorage = true
                val namaFail = dapatkanNamaFailFizikal(context, uri)
                val urlAwam = muatNaikFailKeModulRulaf(context, uri, namaFail)

                if (urlAwam != null) {
                    pautanInput = urlAwam
                    pautanTerkunci = true // 🔒 Kunci medan input pautan serta-merta
                    if (tajukInput.isBlank()) {
                        tajukInput = namaFail.substringBeforeLast('.')
                    }
                    Toast.makeText(context, "✓ Fail berjaya dimuat naik ke storan awan!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal memuat naik fail. Sila cuba lagi.", Toast.LENGTH_LONG).show()
                }
                isUploadingFileToStorage = false
            }
        }
    }

    fun muatDataRepo() {
        scope.launch {
            isSyncing = true
            syncError = null
            try {
                val data = withContext(Dispatchers.IO) { RetrofitClient.api.getBbmMaterials() }
                bbmList = data
            } catch (e: Exception) {
                syncError = "Gagal memuat BBM daripada pelayan awan."
            } finally {
                isSyncing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        muatDataRepo()
    }

    fun kembaliSatuAras() {
        when {
            selectedSubjek != null -> selectedSubjek = null
            selectedDarjah != null -> selectedDarjah = null
            currentFolderId != null -> {
                currentFolderId = if (folderHistory.isNotEmpty()) {
                    folderHistory.removeAt(folderHistory.size - 1)
                } else null
            }
        }
    }

    fun pergiKeRoot() {
        selectedSubjek = null
        selectedDarjah = null
        currentFolderId = null
        folderHistory.clear()
    }

    BackHandler(enabled = selectedSubjek != null || selectedDarjah != null || currentFolderId != null) {
        kembaliSatuAras()
    }

    val currentFolder = remember(bbmList, currentFolderId) {
        bbmList.firstOrNull { it.id == currentFolderId }
    }

    val isPemilikFolder = remember(currentFolder, userEmail, isUserAdmin) {
        currentFolderId == null || currentFolder?.penyumbang.equals(userEmail, ignoreCase = true) || isUserAdmin
    }

    val senaraiAsas = remember(bbmList, filterKoleksiSaya, userEmail) {
        if (filterKoleksiSaya) bbmList.filter { it.penyumbang.equals(userEmail, ignoreCase = true) }
        else bbmList
    }

    val itemsInCurrentFolder = remember(senaraiAsas, currentFolderId) {
        senaraiAsas.filter { item ->
            if (currentFolderId == null) item.parent_id == null || item.parent_id == 0
            else item.parent_id == currentFolderId
        }
    }

    val manualSubfolders = remember(itemsInCurrentFolder) { itemsInCurrentFolder.filter { it.is_folder == true } }
    val filesInCurrentFolder = remember(itemsInCurrentFolder) { itemsInCurrentFolder.filter { it.is_folder != true } }

    val senaraiDarjahUnik = remember(filesInCurrentFolder) {
        filesInCurrentFolder.map {
            val d = it.darjah?.trim()
            if (d.isNullOrBlank()) "Umum" else d
        }.distinct().sorted()
    }

    val senaraiSubjekUnik = remember(filesInCurrentFolder, selectedDarjah) {
        if (selectedDarjah == null) emptyList()
        else filesInCurrentFolder
            .filter { (it.darjah?.trim() ?: "Umum").equals(selectedDarjah, ignoreCase = true) }
            .map { it.subjek?.trim() ?: "Umum" }
            .distinct()
            .sorted()
    }

    val senaraiFailDitapis = remember(filesInCurrentFolder, selectedDarjah, selectedSubjek, searchQuery, senaraiAsas) {
        if (searchQuery.isNotBlank()) {
            senaraiAsas.filter {
                it.is_folder != true && (
                        it.tajuk.contains(searchQuery, ignoreCase = true) ||
                                (it.subjek?.contains(searchQuery, ignoreCase = true) == true) ||
                                (it.darjah?.contains(searchQuery, ignoreCase = true) == true) ||
                                (it.topik?.contains(searchQuery, ignoreCase = true) == true)
                        )
            }
        } else if (selectedDarjah != null && selectedSubjek != null) {
            filesInCurrentFolder.filter {
                (it.darjah?.trim() ?: "Umum").equals(selectedDarjah, ignoreCase = true) &&
                        (it.subjek?.trim() ?: "Umum").equals(selectedSubjek, ignoreCase = true)
            }
        } else emptyList()
    }

    Scaffold(
        floatingActionButton = {
            if (isGuru) {
                FloatingActionButton(
                    onClick = {
                        if (!isPemilikFolder) {
                            Toast.makeText(context, "⛔ Akses Ditolak: Folder ini milik pendidik lain!", Toast.LENGTH_LONG).show()
                        } else {
                            if (selectedDarjah != null) darjahInput = selectedDarjah!!
                            if (selectedSubjek != null) subjekInput = selectedSubjek!!
                            showDialogTambah = true
                        }
                    },
                    containerColor = if (isPemilikFolder) ArchBlue else Color.Gray,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Tambah BBM")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Bar Tajuk
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📁 REPOSITORI OPEN-BBM",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace
                )
                if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Penapis Draft Box Guru
            if (isGuru) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !filterKoleksiSaya,
                        onClick = { filterKoleksiSaya = false; pergiKeRoot() },
                        label = { Text("🌐 Semua Bahan", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = filterKoleksiSaya,
                        onClick = { filterKoleksiSaya = true; pergiKeRoot() },
                        label = { Text("🗄️ Draft Box Saya", fontSize = 11.sp) }
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Bar Carian
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cari tajuk BBM, darjah, atau topik...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Breadcrumbs Navigasi
            val breadcrumbText = buildString {
                append(if (filterKoleksiSaya) "draft-box" else "rulaf-hub")
                if (currentFolder != null) append(" / ${currentFolder.tajuk}")
                if (selectedDarjah != null) append(" / $selectedDarjah")
                if (selectedSubjek != null) append(" / $selectedSubjek")
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = breadcrumbText,
                        fontSize = 11.sp,
                        color = ArchBlue,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f).clickable { pergiKeRoot() }
                    )
                    if (currentFolderId != null || selectedDarjah != null || selectedSubjek != null) {
                        Text(
                            text = "[Kembali ⬅️]",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier.clickable { kembaliSatuAras() }
                        )
                    }
                }
            }

            // Amaran Folder Guru Lain
            if (!isPemilikFolder) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = ArchOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, ArchOrange.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🔒 Mod Baca Sahaja: Koleksi ini milik pendidik lain (${currentFolder?.penyumbang}).",
                        fontSize = 10.sp,
                        color = ArchOrange,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Kandungan Hirarki
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (searchQuery.isNotBlank()) {
                    items(senaraiFailDitapis) { bbm -> FailBbmCard(bbm = bbm) }
                } else if (currentFolderId == null) {
                    val rootFolders = itemsInCurrentFolder.filter { it.is_folder == true }
                    items(rootFolders) { folder ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable {
                                currentFolderId?.let { folderHistory.add(it) }
                                currentFolderId = folder.id
                                selectedDarjah = null
                                selectedSubjek = null
                            },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Folder, contentDescription = null, tint = ArchBlue, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(folder.tajuk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Pemilik: ${folder.penyumbang ?: "Umum"}", fontSize = 11.sp, color = Color.Gray)
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                } else if (selectedDarjah == null) {
                    items(senaraiDarjahUnik) { darjah ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedDarjah = darjah },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Folder, contentDescription = null, tint = ArchBlue, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(darjah, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }

                    // 📋 Kad README diletakkan di bahagian bawah senarai darjah dalam koleksi
                    if (currentFolder != null && !currentFolder.readme_text.isNullOrBlank()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, ArchBlue.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Description, contentDescription = null, tint = ArchBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "README.md • PENERANGAN KOLEKSI",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = ArchBlue,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Divider(color = Color.Gray.copy(alpha = 0.2f))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = currentFolder.readme_text ?: "",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                } else if (selectedSubjek == null) {
                    items(senaraiSubjekUnik) { subjek ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedSubjek = subjek },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FolderSpecial, contentDescription = null, tint = ArchOrange, modifier = Modifier.size(26.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(subjek, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.Gray)
                            }
                        }
                    }
                } else {
                    // Senarai Fail BBM Sebenar
                    if (senaraiFailDitapis.isEmpty()) {
                        item { Text("Tiada fail di dalam folder subjek ini.", fontSize = 11.sp, color = Color.Gray) }
                    } else {
                        items(senaraiFailDitapis) { bbm -> FailBbmCard(bbm = bbm) }
                    }
                }
            }
        }
    }

    // Dialog Tambah Bahan (Dengan Pembersihan URL Automatik)
    if (showDialogTambah) {
        AlertDialog(
            onDismissRequest = { if (!isUploading && !isUploadingFileToStorage) showDialogTambah = false },
            confirmButton = {
                Button(
                    onClick = {
                        val pautanBersih = pautanInput.trim()
                        if (jenisSumbangan == "fail" && (tajukInput.isBlank() || pautanBersih.isBlank())) {
                            Toast.makeText(context, "Sila isi Tajuk dan sediakan fail/pautan!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (jenisSumbangan == "folder" && tajukInput.isBlank()) {
                            Toast.makeText(context, "Sila isi Nama Folder Koleksi!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isUploading = true
                        scope.launch {
                            try {
                                val payload = if (jenisSumbangan == "fail") {
                                    TambahBbmRequest(
                                        tajuk = tajukInput.trim(),
                                        pautan = pautanBersih,
                                        penyumbang = userEmail,
                                        subjek = subjekInput.trim(),
                                        darjah = darjahInput.trim(),
                                        topik = topikInput.trim(),
                                        is_folder = false,
                                        parent_id = currentFolderId
                                    )
                                } else {
                                    TambahBbmRequest(
                                        tajuk = tajukInput.trim(),
                                        readme_text = readmeInput.trim(),
                                        penyumbang = userEmail,
                                        is_folder = true,
                                        parent_id = currentFolderId
                                    )
                                }

                                val res = withContext(Dispatchers.IO) { RetrofitClient.api.tambahBbm(body = payload) }
                                if (res.isSuccessful) {
                                    Toast.makeText(context, "🎉 BBM berjaya diterbitkan!", Toast.LENGTH_SHORT).show()
                                    showDialogTambah = false
                                    tajukInput = ""
                                    pautanInput = ""
                                    pautanTerkunci = false
                                    topikInput = ""
                                    readmeInput = ""
                                    muatDataRepo()
                                } else {
                                    Toast.makeText(context, "Ralat: Kod ${res.code()}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ralat sambungan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                    enabled = !isUploading && !isUploadingFileToStorage
                ) {
                    Text(if (isUploading) "Menerbitkan..." else "Terbitkan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialogTambah = false },
                    enabled = !isUploading && !isUploadingFileToStorage
                ) {
                    Text("Batal")
                }
            },
            title = { Text("Sumbangan Bahan / Folder", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = jenisSumbangan == "fail",
                            onClick = { jenisSumbangan = "fail" },
                            label = { Text("📄 Fail BBM", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = jenisSumbangan == "folder",
                            onClick = { jenisSumbangan = "folder" },
                            label = { Text("📁 Cipta Koleksi", fontSize = 11.sp) }
                        )
                    }

                    if (jenisSumbangan == "fail") {
                        // 📤 KOTAK MUAT NAIK TERUS KE STORAN MODUL-RULAF
                        Card(
                            colors = CardDefaults.cardColors(containerColor = ArchBlue.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, ArchBlue.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "📤 Muat Naik Dokumen / PDF (modul-rulaf):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ArchBlue
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Button(
                                    onClick = {
                                        filePickerLauncher.launch("*/*")
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                                    enabled = !isUploadingFileToStorage
                                ) {
                                    Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isUploadingFileToStorage) "Sedang Memuat Naik..." else "Pilih Dokumen / PDF Dari Telefon",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (isUploadingFileToStorage) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = ArchBlue)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = tajukInput,
                            onValueChange = { tajukInput = it },
                            label = { Text("Tajuk Bahan") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Medan Pautan dengan Status Kunci
                        Column {
                            OutlinedTextField(
                                value = pautanInput,
                                onValueChange = { if (!pautanTerkunci) pautanInput = it.trim() },
                                readOnly = pautanTerkunci,
                                label = { Text(if (pautanTerkunci) "Pautan Terkunci (Auto-Storan)" else "Pautan Fail / URL Luar") },
                                trailingIcon = {
                                    if (pautanTerkunci) {
                                        IconButton(onClick = {
                                            pautanTerkunci = false
                                            pautanInput = ""
                                        }) {
                                            Icon(Icons.Filled.LockOpen, contentDescription = "Buka Kunci", tint = Color.Red)
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = if (pautanTerkunci) SystemGreen else ArchBlue,
                                    unfocusedBorderColor = if (pautanTerkunci) SystemGreen else Color.Gray
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            if (pautanTerkunci) {
                                Text(
                                    text = "🔒 Pautan dikunci bagi mengelakkan ralat aksara.",
                                    fontSize = 9.sp,
                                    color = SystemGreen,
                                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                )
                            }
                        }

                        Text("Peringkat Darjah:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            senaraiPilihanDarjah.take(3).forEach { d ->
                                FilterChip(selected = darjahInput == d, onClick = { darjahInput = d }, label = { Text(d, fontSize = 9.sp) })
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            senaraiPilihanDarjah.drop(3).forEach { d ->
                                FilterChip(selected = darjahInput == d, onClick = { darjahInput = d }, label = { Text(d, fontSize = 9.sp) })
                            }
                        }

                        Text("Subjek:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            senaraiPilihanSubjek.take(3).forEach { s ->
                                FilterChip(selected = subjekInput == s, onClick = { subjekInput = s }, label = { Text(s, fontSize = 9.sp) })
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            senaraiPilihanSubjek.drop(3).forEach { s ->
                                FilterChip(selected = subjekInput == s, onClick = { subjekInput = s }, label = { Text(s, fontSize = 9.sp) })
                            }
                        }

                        OutlinedTextField(
                            value = topikInput,
                            onValueChange = { topikInput = it },
                            label = { Text("Topik / Bab (Pilihan)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = tajukInput,
                            onValueChange = { tajukInput = it },
                            label = { Text("Nama Koleksi / Folder") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = readmeInput,
                            onValueChange = { readmeInput = it },
                            label = { Text("Penerangan Koleksi / README") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            }
        )
    }
}

// ==============================================================
// 📄 KOMPONEN KAD FAIL BBM
// ==============================================================

@Composable
fun FailBbmCard(
    bbm: BbmDto,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📄 ${bbm.tajuk}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${bbm.subjek ?: "Umum"} - ${bbm.darjah ?: "Semua"} | Bab: ${bbm.topik ?: "Umum"}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        val link = bbm.pautan
                        if (!link.isNullOrBlank()) {
                            try {
                                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link.trim()))
                                context.startActivity(webIntent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Pautan tidak sah atau tiada pelayar web!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Pautan bahan tidak disediakan.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RuLaFBlue)
                ) {
                    Text("Buka", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                IconButton(
                    onClick = {
                        val link = bbm.pautan
                        if (!link.isNullOrBlank()) {
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Bahan BBM RuLaFHub: ${bbm.tajuk}\nPautan: $link")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Kongsi BBM"))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Gagal berkongsi bahan.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = "Kongsi",
                        tint = RuLaFBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// =====================================================================
// 💬 FORUM SCREEN
// =====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForumScreen(userRole: String, userEmail: String) {
    val isGuru = userRole.equals("Guru", ignoreCase = true)
    var forumList by remember { mutableStateOf<List<ForumDto>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedThread by remember { mutableStateOf<ForumDto?>(null) }
    var commentList by remember { mutableStateOf<List<CommentDto>>(emptyList()) }
    var newCommentText by remember { mutableStateOf("") }

    var showDialogTambah by remember { mutableStateOf(false) }
    var dialogTajuk by remember { mutableStateOf("") }
    var dialogKeterangan by remember { mutableStateOf("") }
    var dialogKategori by remember { mutableStateOf("QNA") }
    var isPostingThread by remember { mutableStateOf(false) }

    fun muatForumData() {
        scope.launch {
            isSyncing = true
            syncError = null
            try {
                val data = withContext(Dispatchers.IO) { RetrofitClient.api.getForumThreads() }
                forumList = data
            } catch (e: Exception) {
                syncError = "Gagal memuat Forum secara langsung."
            } finally {
                isSyncing = false
            }
        }
    }

    LaunchedEffect(Unit) {
        muatForumData()
    }

    LaunchedEffect(selectedThread) {
        if (selectedThread != null) {
            try {
                val comments = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getComments("eq.${selectedThread!!.id}")
                }
                commentList = comments
            } catch (e: Exception) {
                commentList = emptyList()
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            if (isGuru && selectedThread == null) {
                FloatingActionButton(
                    onClick = {
                        dialogKategori = "QNA"
                        dialogTajuk = ""
                        dialogKeterangan = ""
                        showDialogTambah = true
                    },
                    containerColor = ArchBlue,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.AddComment, contentDescription = "Buka Topik")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (selectedThread == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "💬 COMMUNITY FORUM",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ArchOrange, RoundedCornerShape(12.dp))
                        .clickable {
                            dialogKategori = "BUG"
                            dialogTajuk = "[BUG] "
                            dialogKeterangan = ""
                            showDialogTambah = true
                        },
                    colors = CardDefaults.cardColors(containerColor = ArchOrange.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.BugReport, contentDescription = "Bugs", tint = ArchOrange, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("📌 LAPOR PEPIJAT SISTEM (TEKAN DI SINI)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ArchOrange)
                            Text("Daftarkan ralat sistem terus ke papan perbincangan pentadbir.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(forumList) { disc ->
                        val isBug = disc.kategori.equals("BUG", ignoreCase = true) || disc.tajuk.contains("[BUG]", ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedThread = disc },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            border = if (isBug) BorderStroke(1.dp, ArchOrange.copy(alpha = 0.5f)) else null
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (isBug) ArchOrange.copy(alpha = 0.15f) else ArchBlue.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = disc.kategori ?: if (isBug) "BUG" else "QNA",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isBug) ArchOrange else ArchBlue,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(disc.penulis, fontSize = 10.sp, color = Color.Gray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(disc.tajuk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(disc.soalan, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("💬 Buka Perbincangan & Komen", color = ArchBlue, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            } else {
                val thread = selectedThread!!
                BackRow(name = "Kembali ke Senarai Forum") {
                    selectedThread = null
                    commentList = emptyList()
                }
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(thread.penulis, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        Text(thread.tajuk, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(thread.soalan, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("💬 Maklum Balas Komuniti (${commentList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(commentList) { comment ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(comment.penulis, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = ArchOrange)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(comment.komen, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newCommentText,
                        onValueChange = { newCommentText = it },
                        placeholder = { Text("Tulis ulasan anda...", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newCommentText.isNotBlank()) {
                                scope.launch {
                                    try {
                                        val response = withContext(Dispatchers.IO) {
                                            RetrofitClient.api.postComment(
                                                comment = CommentDto(
                                                    forum_id = thread.id,
                                                    komen = newCommentText.trim(),
                                                    penulis = userEmail
                                                )
                                            )
                                        }
                                        if (response.isSuccessful) {
                                            val comments = withContext(Dispatchers.IO) {
                                                RetrofitClient.api.getComments("eq.${thread.id}")
                                            }
                                            commentList = comments
                                            newCommentText = ""
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Hantar", fontSize = 11.sp)
                    }
                }
            }
        }
    }

    if (showDialogTambah) {
        AlertDialog(
            onDismissRequest = { if (!isPostingThread) showDialogTambah = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (dialogTajuk.isBlank() || dialogKeterangan.isBlank()) {
                            Toast.makeText(context, "Sila lengkapkan tajuk dan penerangan!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isPostingThread = true
                        scope.launch {
                            try {
                                val payload = TambahForumRequest(
                                    tajuk = dialogTajuk.trim(),
                                    soalan = dialogKeterangan.trim(),
                                    penulis = userEmail,
                                    subjek = if (dialogKategori == "BUG") "Sistem" else "Jawi",
                                    darjah = "Semua",
                                    kategori = dialogKategori
                                )
                                val res = withContext(Dispatchers.IO) {
                                    RetrofitClient.api.postForumThread(body = payload)
                                }
                                if (res.isSuccessful) {
                                    Toast.makeText(context, "Topik berjaya didaftarkan ke pangkalan data!", Toast.LENGTH_SHORT).show()
                                    showDialogTambah = false
                                    muatForumData()
                                } else {
                                    Toast.makeText(context, "Ralat: Kod ${res.code()}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ralat pelayan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isPostingThread = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (dialogKategori == "BUG") ArchOrange else ArchBlue),
                    enabled = !isPostingThread
                ) {
                    Text(if (isPostingThread) "Menghantar..." else "Terbitkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogTambah = false }, enabled = !isPostingThread) {
                    Text("Batal")
                }
            },
            title = {
                Text(
                    text = if (dialogKategori == "BUG") "🐞 Laporan Pepijat Sistem" else "💬 Buka Topik Perbincangan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dialogTajuk,
                        onValueChange = { dialogTajuk = it },
                        label = { Text("Tajuk Isu / Soalan") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = dialogKeterangan,
                        onValueChange = { dialogKeterangan = it },
                        label = { Text("Butiran Terperinci") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            }
        )
    }
}

// =====================================================================
// ☀️ PROFIL SCREEN
// =====================================================================
@Composable
fun ProfilScreen(
    userRole: String,
    userEmail: String,
    themeMode: String,
    isDarkMode: Boolean,
    onThemeModeChange: (String) -> Unit,
    onLogout: () -> Unit,
    onNavigateToSemakan: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sharedPrefs = remember { context.getSharedPreferences("RuLaF_Prefs", Context.MODE_PRIVATE) }

    var profileName by remember { mutableStateOf("") }
    var profileAge by remember { mutableStateOf("") }
    var profileGender by remember { mutableStateOf("Lelaki") }
    var profileRole by remember { mutableStateOf(userRole) }
    var profileMyKid by remember { mutableStateOf("") }

    var isLoadingProfile by remember { mutableStateOf(true) }
    var profileLoadError by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    var isAutoLoginEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("auto_login_enabled", false)) }
    var isBiometricEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("biometric_login_enabled", false)) }

    var isProfilConfigExpanded by rememberSaveable { mutableStateOf(true) }
    var isPetiSimpananExpanded by rememberSaveable { mutableStateOf(false) }

    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var showProfilePictureDialog by remember { mutableStateOf(false) }
    var isUploadingPicture by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val isMurid = profileRole.equals("Murid", ignoreCase = true)

    LaunchedEffect(userEmail) {
        isLoadingProfile = true
        profileLoadError = null
        try {
            val response = withContext(Dispatchers.IO) {
                RetrofitClient.api.getUserProfile("eq.$userEmail")
            }

            if (response.isNotEmpty()) {
                val userRecord = response.first()
                profileName = userRecord.nama ?: ""
                profileAge = userRecord.umur?.toString() ?: ""
                profileGender = userRecord.jantina ?: "Lelaki"
                profileRole = userRecord.peranan ?: userRole
                profileMyKid = userRecord.mykid ?: ""
                profilePictureUrl = userRecord.profile_picture_url
            } else {
                profileLoadError = "Maklumat pengguna tiada dalam pangkalan data. Sila simpan untuk mendaftar."
            }
        } catch (e: Exception) {
            profileLoadError = "Ralat memuat profil dari pelayan. Sila periksa internet."
        } finally {
            isLoadingProfile = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val profilPicturePicker = androidx.activity.compose.rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                if (uri != null) {
                    scope.launch {
                        isUploadingPicture = true
                        try {
                            val urlBaru = muatNaikGambarProfilKeStoran(context, uri)
                            if (urlBaru != null) {
                                val simpanan = withContext(Dispatchers.IO) {
                                    RetrofitClient.api.updateProfilePictureUrl(
                                        email = userEmail,
                                        body = mapOf("profile_picture_url" to urlBaru)
                                    )
                                }
                                profilePictureUrl = urlBaru
                                if (simpanan.isSuccessful) {
                                    Toast.makeText(context, "✓ Gambar profil dikemas kini!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Gambar dinaikkan, tetapi pautan gagal disimpan.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Gagal memuat naik gambar. Sila cuba lagi.", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Ralat semasa memuat naik gambar.", Toast.LENGTH_LONG).show()
                        } finally {
                            isUploadingPicture = false
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { profilPicturePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePictureUrl != null) {
                            AsyncImage(
                                model = profilePictureUrl,
                                contentDescription = "Gambar Profil",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Tiada Gambar",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ArchBlue)
                            .clickable { profilPicturePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isUploadingPicture) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Tukar Gambar",
                                modifier = Modifier.size(18.dp),
                                tint = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Profil",
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (profileName.isNotBlank()) "$profileName • $userEmail" else userEmail,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isProfilConfigExpanded = !isProfilConfigExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profil",
                                tint = ArchBlue
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "PROFIL & TETAPAN",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (profileName.isNotBlank()) profileName else "USER_ID: $userEmail",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isProfilConfigExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = Color.Gray
                        )
                    }

                    AnimatedVisibility(visible = isProfilConfigExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            Divider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "USER_ID: $userEmail",
                        color = SystemGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoadingProfile) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        if (profileLoadError != null) {
                            Text(
                                text = profileLoadError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("NAMA PENUH:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ArchBlue, fontFamily = FontFamily.Monospace)
                                OutlinedTextField(
                                    value = profileName,
                                    onValueChange = { profileName = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Nama pengguna...", fontSize = 11.sp) },
                                    singleLine = true
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("UMUR:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ArchBlue, fontFamily = FontFamily.Monospace)
                                OutlinedTextField(
                                    value = profileAge,
                                    onValueChange = { profileAge = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("Tahun...", fontSize = 11.sp) },
                                    singleLine = true
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("JANTINA:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ArchBlue, fontFamily = FontFamily.Monospace)
                                OutlinedTextField(
                                    value = profileGender,
                                    onValueChange = { profileGender = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PERANAN (ROLE):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9C27B0), fontFamily = FontFamily.Monospace)
                                OutlinedTextField(
                                    value = profileRole,
                                    onValueChange = {},
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }

                        if (isMurid) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column {
                                Text("NO. MYKID / PENGENALAN:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = ArchBlue, fontFamily = FontFamily.Monospace)
                                OutlinedTextField(
                                    value = profileMyKid,
                                    onValueChange = { profileMyKid = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = { Text("NoMykid Tiada (-)", fontSize = 11.sp) },
                                    singleLine = true
                                )
                            }
                        }

                        Text(
                            text = "*Hanya (Admin) boleh menukar akses peranan.",
                            fontSize = 9.sp,
                            color = Color.Red,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isSaving = true
                                scope.launch {
                                    try {
                                        val dto = UserProfileDto(
                                            email = userEmail,
                                            nama = profileName.trim(),
                                            umur = profileAge.toIntOrNull(),
                                            jantina = profileGender.trim(),
                                            peranan = profileRole,
                                            mykid = if (isMurid) profileMyKid.trim() else null
                                        )
                                        val response = withContext(Dispatchers.IO) {
                                            RetrofitClient.api.upsertUserProfile(profile = dto)
                                        }
                                        if (response.isSuccessful) {
                                            Toast.makeText(context, "Profil berjaya disimpan ke Supabase!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Ralat Pelayan: ${response.code()}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Ralat rangkaian semasa menyimpan!", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSaving = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                            shape = RoundedCornerShape(4.dp),
                            enabled = !isSaving
                        ) {
                            Text(
                                text = if (isSaving) "MENYIMPAN KE PANGKALAN DATA..." else "Simpan Konfigurasi",
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        if (isMurid) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = SystemGreen.copy(alpha = 0.08f)),
                                border = BorderStroke(1.dp, SystemGreen),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("[ AKSES MURID ]", color = SystemGreen, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Lihat prestasi formatif, pencapaian gamifikasi, dan sejarah semakan markah Jawi terkini anda di sini.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = onNavigateToSemakan,
                                        colors = ButtonDefaults.buttonColors(containerColor = SystemGreen),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text("Semak Markah Saya", fontSize = 11.sp)
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

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPetiSimpananExpanded = !isPetiSimpananExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Keselamatan",
                                tint = ArchBlue
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Peti Simpanan Kelayakan & Keselamatan",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 2
                                )
                                Text(
                                    when {
                                        isAutoLoginEnabled && isBiometricEnabled -> "Auto-login & sidik jari aktif."
                                        isAutoLoginEnabled -> "Daftar masuk automatik aktif."
                                        isBiometricEnabled -> "Sidik jari diaktifkan."
                                        else -> "Tiada tetapan keselamatan aktif."
                                    },
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isPetiSimpananExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = Color.Gray
                        )
                    }

                    AnimatedVisibility(visible = isPetiSimpananExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Divider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Daftar Masuk Automatik", fontSize = 12.sp)
                                Switch(
                                    checked = isAutoLoginEnabled,
                                    onCheckedChange = { isChecked ->
                                        isAutoLoginEnabled = isChecked
                                        sharedPrefs.edit().apply {
                                            putBoolean("auto_login_enabled", isChecked)
                                            putString("saved_email", userEmail)
                                            putString("saved_role", profileRole)
                                            apply()
                                        }
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Daftar Masuk Sidik Jari (Fingerprint)", fontSize = 12.sp)
                                Switch(
                                    checked = isBiometricEnabled,
                                    onCheckedChange = { isChecked ->
                                        val activity = context.findActivity()
                                        if (isChecked && activity != null) {
                                            AuthHelper.authenticateWithBiometric(activity) { success ->
                                                if (success) {
                                                    isBiometricEnabled = true
                                                    sharedPrefs.edit().putBoolean("biometric_login_enabled", true).apply()
                                                } else {
                                                    isBiometricEnabled = false
                                                }
                                            }
                                        } else {
                                            isBiometricEnabled = false
                                            sharedPrefs.edit().putBoolean("biometric_login_enabled", false).apply()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            var isThemeExpanded by remember { mutableStateOf(false) }
            val themeOptions = listOf(
                Triple("system", "Sistem", "Ikut tema telefon anda secara automatik."),
                Triple("light", "Cerah", "Sentiasa gunakan tema terang."),
                Triple("dark", "Gelap", "Sentiasa gunakan tema gelap.")
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isThemeExpanded = !isThemeExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Filled.Palette, contentDescription = "Theme")
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Tema Paparan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    themeOptions.firstOrNull { it.first == themeMode }?.third
                                        ?: "Ikut tema telefon anda secara automatik.",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (isThemeExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Expand theme options",
                            tint = Color.Gray
                        )
                    }

                    AnimatedVisibility(visible = isThemeExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        ) {
                            Divider(
                                modifier = Modifier.padding(bottom = 4.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                            themeOptions.forEach { (mode, label, description) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onThemeModeChange(mode)
                                            isThemeExpanded = false
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = themeMode == mode,
                                        onClick = {
                                            onThemeModeChange(mode)
                                            isThemeExpanded = false
                                        },
                                        colors = RadioButtonDefaults.colors(selectedColor = ArchBlue)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text(
                                            description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("LOG KELUAR SISTEM", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

fun loadNotisDibacaIds(context: Context): Set<Long> {
    return try {
        context.getSharedPreferences("RuLaF_Prefs", Context.MODE_PRIVATE)
            .getString("notis_dibaca_ids", "")
            ?.split(",")
            ?.mapNotNull { it.trim().toLongOrNull() }
            ?.toSet() ?: emptySet()
    } catch (_: Exception) {
        emptySet()
    }
}

fun simpanNotisDibacaIds(context: Context, ids: Set<Long>) {
    context.getSharedPreferences("RuLaF_Prefs", Context.MODE_PRIVATE)
        .edit()
        .putString("notis_dibaca_ids", ids.joinToString(","))
        .apply()
}

@Composable
fun BackRow(name: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Back",
            tint = ArchBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            color = ArchBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NfcActionBottomSheet(
    mykid: String,
    noTelefonCard: String? = null,
    senaraiMurid: List<StudentDto>,
    onDismiss: () -> Unit,
    onSuccessSimpan: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val murid = senaraiMurid.firstOrNull { it.mykid.trim() == mykid.trim() }
    val namaPaparan = murid?.nama_murid ?: "Murid Kad NFC"
    val kelasPaparan = murid?.kelas_id ?: "3 Murshid"
    val noTelPaparan = when {
        !noTelefonCard.isNullOrBlank() -> noTelefonCard.trim()
        !murid?.no_tel.isNullOrBlank() -> murid!!.no_tel!!.trim()
        else -> null
    }

    val senaraiSubjekKelas = remember(kelasPaparan) {
        if (kelasPaparan.contains("5", ignoreCase = true)) {
            listOf("Al-Quran", "Tauhid", "Ibadat", "Bahasa Arab")
        } else {
            listOf(
                "Al-Quran", "Hafazan", "Tauhid", "Ibadat",
                "Bahasa Arab", "Sirah", "Jawi", "Akhlak",
                "Amalan Lazim", "KIJ"
            )
        }
    }

    var sasaranSubjekHariIni by remember { mutableStateOf(2) }
    val subjekSelesaiSet = remember { mutableStateListOf<String>() }
    var isSending by remember { mutableStateOf(false) }

    val jumlahSiap = subjekSelesaiSet.size
    val peratusSiap = if (sasaranSubjekHariIni > 0) {
        (jumlahSiap.toFloat() / sasaranSubjekHariIni.toFloat()) * 100f
    } else 0f

    val warnaSkor = when {
        jumlahSiap == 0 -> Color(0xFF6B7280)
        peratusSiap >= 100f -> Color(0xFF10B981)
        peratusSiap >= 50f -> Color(0xFFFBBF24)
        else -> Color(0xFFEF4444)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "● HADIR KELAS :: IMBASAN NFC DISAHKAN",
                    color = Color(0xFF10B981),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = namaPaparan,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "MyKid: $mykid | Kelas: $kelasPaparan",
                fontSize = 11.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )

            if (!noTelPaparan.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.background)
                        .clickable {
                            val callIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$noTelPaparan"))
                            context.startActivity(callIntent)
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = ArchBlue, modifier = Modifier.size(12.dp))
                    Text("Penjaga: $noTelPaparan [Dail]", fontSize = 10.sp, color = ArchBlue, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = Color.Gray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SASARAN HARI INI:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ArchBlue)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(2, 3).forEach { sasaran ->
                        FilterChip(
                            selected = sasaranSubjekHariIni == sasaran,
                            onClick = { sasaranSubjekHariIni = sasaran },
                            label = { Text("$sasaran Subjek", fontSize = 10.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TANDAKAN SUBJEK SIAP:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Surface(
                    color = warnaSkor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "$jumlahSiap / $sasaranSubjekHariIni SIAP",
                        color = if (peratusSiap >= 50f && peratusSiap < 100f) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                senaraiSubjekKelas.forEach { subjek ->
                    val isChecked = subjekSelesaiSet.contains(subjek)
                    FilterChip(
                        selected = isChecked,
                        onClick = {
                            if (isChecked) {
                                subjekSelesaiSet.remove(subjek)
                            } else {
                                if (subjekSelesaiSet.size < sasaranSubjekHariIni) {
                                    subjekSelesaiSet.add(subjek)
                                } else {
                                    Toast.makeText(context, "Maksimum $sasaranSubjekHariIni subjek untuk hari ini!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        label = { Text(subjek, fontSize = 10.sp) },
                        leadingIcon = if (isChecked) {
                            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==============================================================
            // 1. BUTANG PANTAS: SAHKAN KEHADIRAN SAHAJA (MURID LAMBAT SIAP KERJA)
            // ==============================================================
            Button(
                onClick = {
                    if (!isSending) {
                        isSending = true
                        scope.launch {
                            try {
                                val tarikhHariIni = java.text.SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date())

                                // Rekod hadir sahaja (tugasan 0 dahulu)
                                val payload = HantarKerajinanRequest(
                                    tarikh = tarikhHariIni,
                                    mykid = mykid,
                                    subjek = "Hadir Kelas",
                                    tugasan_siap = 0,
                                    status_hadir = true
                                )

                                val res = withContext(Dispatchers.IO) {
                                    RetrofitClient.api.hantarRekodKerajinan(body = payload)
                                }

                                if (res.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "✓ [Hadir Sahaja] Kehadiran $namaPaparan disahkan!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onSuccessSimpan()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Ralat Pelayan: Kod ${res.code()}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ralat sambungan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSending = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SystemGreen),
                shape = RoundedCornerShape(8.dp),
                enabled = !isSending
            ) {
                Text(
                    text = "⚡ [ SAHKAN HADIR SAHAJA ]",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("— ATAU REKOD BERSAMA SUBJEK SIAP —", fontSize = 9.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

            // ==============================================================
            // 2. BUTANG ASAL: REKOD KEHADIRAN + SUBJEK YANG SIAP
            // ==============================================================
            Button(
                onClick = {
                    if (!isSending) {
                        isSending = true
                        scope.launch {
                            try {
                                val tarikhHariIni = java.text.SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date())

                                val ringkasanSubjek = if (subjekSelesaiSet.isNotEmpty()) {
                                    subjekSelesaiSet.joinToString(", ")
                                } else {
                                    "Tiada Tugasan"
                                }

                                val payload = HantarKerajinanRequest(
                                    tarikh = tarikhHariIni,
                                    mykid = mykid,
                                    subjek = ringkasanSubjek,
                                    tugasan_siap = jumlahSiap,
                                    status_hadir = true
                                )

                                val res = withContext(Dispatchers.IO) {
                                    RetrofitClient.api.hantarRekodKerajinan(body = payload)
                                }

                                if (res.isSuccessful) {
                                    Toast.makeText(
                                        context,
                                        "✓ [Hadir & Tugasan] $namaPaparan: $jumlahSiap/$sasaranSubjekHariIni subjek siap ($ringkasanSubjek)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onSuccessSimpan()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Ralat Pelayan: Kod ${res.code()}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ralat sambungan: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isSending = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                shape = RoundedCornerShape(8.dp),
                enabled = !isSending
            ) {
                Text(
                    text = if (isSending) "MENYIMPAN..." else "[ SAHKAN KEHADIRAN & SUBJEK ($jumlahSiap/$sasaranSubjekHariIni) ]",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            TextButton(onClick = onDismiss, enabled = !isSending) {
                Text("BATAL", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =====================================================================
// 📊 DASHBOARD UTILITY COMPONENTS
// =====================================================================

@Composable
fun DashboardStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    icon: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (icon.isNotEmpty()) {
                    Text(icon, fontSize = 14.sp)
                }
                Text(
                    title,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    unit,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Text(
                subtitle,
                fontSize = 9.sp,
                color = Color.Gray,
                lineHeight = 12.sp
            )
        }
    }
}

@Composable
fun MetricBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    val numericValue = value.replace("%", "").toDoubleOrNull() ?: 0.0
    val color = when {
        numericValue >= 80 -> Color(0xFF16A34A) // Green
        numericValue >= 50 -> ArchOrange
        numericValue > 0 -> Color.Red
        else -> Color.Gray
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text(
                value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun KerajinanHeatmapCompose(
    senaraiRekod: List<RekodKerajinanDto>,
    jumlahTugasanLalai: Int = 2
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Text(
            "INDURANS & KERAJINAN (7 HARI TERAKHIR)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Kita tunjukkan 7 slot terakhir
            val calendar = java.util.Calendar.getInstance()
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            
            val last7Days = (0..6).map { i ->
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -i)
                dateFormat.format(cal.time)
            }.reversed()

            last7Days.forEach { tarikh ->
                val rekod = senaraiRekod.find { it.tarikh == tarikh }
                val jumlahSiap = rekod?.tugasan_siap ?: 0
                val intensity = if (jumlahSiap >= jumlahTugasanLalai) 1.0f 
                                else if (jumlahSiap > 0) 0.5f 
                                else 0.1f
                
                val color = if (jumlahSiap > 0) ArchBlue.copy(alpha = intensity) 
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (jumlahSiap > 0) {
                        Text(
                            "$jumlahSiap",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (intensity > 0.6f) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("7 Hari Lepas", fontSize = 8.sp, color = Color.Gray)
            Text("Hari Ini", fontSize = 8.sp, color = Color.Gray)
        }
    }
}
