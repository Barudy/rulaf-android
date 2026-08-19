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
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.albabacademy.rulafhub.data.remote.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.nio.charset.Charset

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

fun kiraPrestasiHolistik(data: MarkahMuridDto): PrestasiPaparan {
    val ujianBertulis = data.ujian_bertulis ?: 0.0
    val markahJawi = data.markah_jawi ?: 0.0
    val purataAkademik = if (ujianBertulis > 0.0) {
        ((ujianBertulis + markahJawi) / 200.0) * 100.0
    } else {
        markahJawi
    }

    val akhlak = data.akhlak ?: 0.0
    val kerajinan = data.kerajinan_usaha ?: 0.0
    val kerjasama = data.kerjasama_kumpulan ?: 0.0
    val hariHadir = data.hari_hadir ?: data.kehadiran ?: 0.0
    val jumlahHari = (data.jumlah_hari_sekolah ?: 140.0).coerceAtLeast(1.0)

    val kehadiranSkala = ((hariHadir / jumlahHari) * 100.0) / 10.0
    val jumlahSahsiah = akhlak + kerajinan + kerjasama + kehadiranSkala
    val peratusSahsiah = (jumlahSahsiah / 40.0) * 100.0

    val skorKeseluruhan = (purataAkademik * 0.6) + (peratusSahsiah * 0.4)

    return PrestasiPaparan(
        namaMurid = data.nama_murid ?: "Tiada Rekod",
        kelasId = data.kelas_id ?: "-",
        bulanTahun = data.bulan_tahun ?: "Ogos 2026",
        tahapRulaf = data.tahap_rulaf ?: "Belum Ditetapkan",
        nilaiAkademik = String.format("%.1f%%", purataAkademik),
        nilaiSahsiah = String.format("%.1f%%", peratusSahsiah),
        skorAkhir = String.format("%.2f", skorKeseluruhan),
        bacaanQuran = data.bacaan_quran ?: "-",
        hafazan = data.hafazan ?: "-"
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
            var isDarkMode by remember { mutableStateOf(true) }
            var isLoggedIn by remember { mutableStateOf(false) }
            var userRole by remember { mutableStateOf("") }

            LaunchedEffect(userRole) {
                userRoleGlobal = userRole
            }

            var loggedInUserEmail by remember { mutableStateOf("") }
            val context = LocalContext.current
            val sharedPrefs = remember { context.getSharedPreferences("RuLaF_Prefs", Context.MODE_PRIVATE) }

            // Log Masuk Automatik Tempatan
            LaunchedEffect(Unit) {
                val isAutoLoginEnabled = sharedPrefs.getBoolean("auto_login_enabled", false)
                val savedEmail = sharedPrefs.getString("saved_email", "") ?: ""
                val savedRole = sharedPrefs.getString("saved_role", "") ?: ""
                val useBiometric = sharedPrefs.getBoolean("biometric_login_enabled", false)

                if (isAutoLoginEnabled && savedEmail.isNotEmpty() && savedRole.isNotEmpty()) {
                    if (useBiometric) {
                        authenticateWithBiometric(this@MainActivity) { success ->
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
                        isDarkMode = isDarkMode,
                        onThemeToggle = { isDarkMode = !isDarkMode },
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
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
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
        bottomBar = { RuLaFBottomNavigationBar(navController, userRole) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(userRole, userEmail, onNavigateToSemakan = {
                    navController.navigate("dashboard")
                })
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
                    isDarkMode = isDarkMode,
                    onThemeToggle = onThemeToggle,
                    onLogout = onLogout,
                    onNavigateToSemakan = { navController.navigate("dashboard") }
                )
            }
        }
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
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            )
        }
    }
}

// =====================================================================
// 📊 DASHBOARD SCREEN
// =====================================================================
@Composable
fun DashboardScreen(
    userRole: String,
    userEmail: String,
    onNavigateToSemakan: () -> Unit = {}
) {
    val isGuru = userRole.equals("Guru", ignoreCase = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var studentList by remember { mutableStateOf<List<StudentDto>>(emptyList()) }
    var tahapMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var markahMap by remember { mutableStateOf<Map<String, MarkahMuridDto>>(emptyMap()) }
    var isSyncing by remember { mutableStateOf(false) }
    var senaraiKerajinanMurid by remember { mutableStateOf<List<RekodKerajinanDto>>(emptyList()) }

    var paparanMurid by remember { mutableStateOf<PrestasiPaparan?>(null) }
    var statusMesejMurid by remember { mutableStateOf<String?>(null) }
    var isLoadingMurid by remember { mutableStateOf(false) }

    var filterDarjah by remember { mutableStateOf("Semua") }
    var filterTahap by remember { mutableStateOf("Semua") }
    var filterBulan by remember { mutableStateOf("Ogos 2026") }

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

    LaunchedEffect(userRole, userEmail) {
        if (isGuru) {
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
            } catch (e: Exception) {
                studentList = emptyList()
            } finally {
                isSyncing = false
            }
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
                        paparanMurid = kiraPrestasiHolistik(grades.first())
                    }

                    val rekodNfc = withContext(Dispatchers.IO) {
                        try {
                            RetrofitClient.api.getRekodKerajinanMurid("eq.$userMyKid")
                        } catch (e: Exception) {
                            emptyList<RekodKerajinanDto>()
                        }
                    }
                    senaraiKerajinanMurid = rekodNfc
                }
            } catch (e: Exception) {
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
        if (isGuru) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "🛠 PAPAN KAWALAN PENTADBIR :: RULAFHUB",
                            fontSize = 14.sp,
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
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardStatCard(modifier = Modifier.weight(1f), title = "MURID BERDAFTAR", value = "58", unit = "Orang", subtitle = "Darjah 3 & Darjah 5 (Active)", icon = "🎓")
                        DashboardStatCard(modifier = Modifier.weight(1f), title = "PENGGUNA AKTIF", value = "5", unit = "🟢", subtitle = "Sedang memantau & menguji", icon = "")
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DashboardStatCard(modifier = Modifier.weight(1f), title = "SIRI PERMAINAN", value = "2", unit = "Kuiz", subtitle = "Modular Tadrij (Active)", icon = "🎮")
                        DashboardStatCard(modifier = Modifier.weight(1f), title = "SUMBANGAN BBM", value = "7", unit = "Bahan", subtitle = "Bahan sokongan terbuka", icon = "📁")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🚀 PINTASAN PANTAS PANEL (QUICK ACTIONS)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {}, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = ArchBlue), shape = RoundedCornerShape(4.dp)) {
                                Text("🎮 [ Cipta Misi ]", fontSize = 10.sp, maxLines = 1)
                            }
                            Button(onClick = {}, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = SystemGreen), shape = RoundedCornerShape(4.dp)) {
                                Text("📋 [ Pengurusan ]", fontSize = 10.sp, maxLines = 1)
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
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🤖 ++ EJEN AI ANALISIS PRESTASI (NL2SQL)", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Tanya soalan bahasa tabii. Ejen AI akan menjana kod SQL untuk merungkai pangkalan data.", fontSize = 10.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = "",
                                onValueChange = {},
                                placeholder = { Text("Cth: senarai murid 3 Murshid yang lancar", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = ArchBlue), shape = RoundedCornerShape(4.dp)) {
                                Text("[ TANYA AI ]", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

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

                        Spacer(modifier = Modifier.height(10.dp))

                        Text("PILIHAN DARJAH:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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

                        Spacer(modifier = Modifier.height(6.dp))

                        Text("KUMPULAN RULAF:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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

            items(filteredStudents) { student ->
                val mykidBersih = student.mykid.trim()
                val tahapSebenar = tahapMap[mykidBersih] ?: student.tahap ?: "RuLaF Ba"

                var isExpanded by remember { mutableStateOf(false) }
                var isManualMarked by remember { mutableStateOf(false) }
                var isSendingData by remember { mutableStateOf(false) }

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

                val lencanaWarna = when {
                    tahapSebenar.contains("Ta", ignoreCase = true) -> SystemGreen
                    tahapSebenar.contains("Ba", ignoreCase = true) -> ArchBlue
                    tahapSebenar.contains("Alif", ignoreCase = true) -> ArchOrange
                    tahapSebenar.contains("Khas", ignoreCase = true) -> Color.Red
                    else -> Color.Gray
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(10.dp),
                    border = if (isExpanded) BorderStroke(1.dp, ArchBlue) else null
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
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = lencanaWarna,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = tahapSebenar,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (!isSendingData) {
                                            isSendingData = true
                                            scope.launch {
                                                try {
                                                    val tarikhHariIni = java.text.SimpleDateFormat(
                                                        "yyyy-MM-dd",
                                                        java.util.Locale.getDefault()
                                                    ).format(java.util.Date())

                                                    val ringkasanSubjek = if (subjekSelesaiSet.isNotEmpty()) {
                                                        subjekSelesaiSet.joinToString(", ")
                                                    } else {
                                                        "Hadir Kelas"
                                                    }

                                                    val payload = HantarKerajinanRequest(
                                                        tarikh = tarikhHariIni,
                                                        mykid = mykidBersih,
                                                        subjek = ringkasanSubjek,
                                                        tugasan_siap = jumlahSiap,
                                                        status_hadir = true
                                                    )

                                                    val res = withContext(Dispatchers.IO) {
                                                        RetrofitClient.api.hantarRekodKerajinan(body = payload)
                                                    }

                                                    if (res.isSuccessful) {
                                                        isManualMarked = true
                                                        Toast.makeText(context, "✓ Hadir: ${student.nama_murid} direkodkan!", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Ralat sambungan pangkalan data.", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isSendingData = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.height(30.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isManualMarked) SystemGreen else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(4.dp),
                                    enabled = !isSendingData
                                ) {
                                    Text(
                                        text = if (isManualMarked) "HADIR ✓" else "+ HADIR",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isManualMarked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

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
                                    Text(
                                        text = "Jantina: ${student.jantina}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "Penjaga: ${student.no_tel?.ifBlank { "Tiada No. Tel" } ?: "Tiada No. Tel"}",
                                        fontSize = 11.sp,
                                        color = ArchBlue,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

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

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("TANDAKAN SUBJEK SIAP:", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Surface(
                                        color = warnaSkor,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "$jumlahSiap / $sasaranSubjekHariIni SIAP",
                                            color = if (peratusSiap >= 50f && peratusSiap < 100f) Color.Black else Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

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

                                Button(
                                    onClick = {
                                        if (!isSendingData) {
                                            isSendingData = true
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
                                                        mykid = mykidBersih,
                                                        subjek = ringkasanSubjek,
                                                        tugasan_siap = jumlahSiap,
                                                        status_hadir = true
                                                    )

                                                    val res = withContext(Dispatchers.IO) {
                                                        RetrofitClient.api.hantarRekodKerajinan(body = payload)
                                                    }

                                                    if (res.isSuccessful) {
                                                        isManualMarked = true
                                                        Toast.makeText(context, "✓ Rekod tugasan ($jumlahSiap/$sasaranSubjekHariIni) disimpan!", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Ralat: Kod ${res.code()}", Toast.LENGTH_SHORT).show()
                                                    }
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Gagal mengemas kini tugasan.", Toast.LENGTH_SHORT).show()
                                                } finally {
                                                    isSendingData = false
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                                    shape = RoundedCornerShape(4.dp),
                                    enabled = !isSendingData
                                ) {
                                    Text(
                                        text = if (isSendingData) "MENYIMPAN..." else "[ SIMPAN SUBJEK & KEHADIRAN ]",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // 🔑 DIBAIKI: Dibungkus dengan Column yang kemas
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
                                            Text(text = tahapSebenar, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = lencanaWarna)
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
            // Paparan Murid
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
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Semakan Prestasi RuLaF", fontSize = 20.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface)
                            Text("[ DASHBOARD PINTAR PELAJAR ]", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color.Gray)
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
                                    Text(p.tahapRulaf, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
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
                }
            }
        }
    }
}

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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                if (icon.isNotEmpty()) Text(icon, fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Text(unit, fontSize = 11.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, fontSize = 9.sp, color = Color.Gray, maxLines = 1)
        }
    }
}

@Composable
fun MetricBox(modifier: Modifier = Modifier, title: String, value: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray, maxLines = 1)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
fun KerajinanHeatmapCompose(
    senaraiRekod: List<RekodKerajinanDto>,
    jumlahTugasanLalai: Int = 3
) {
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
                    text = "KALENDAR KERAJINAN (NFC STREAK)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = ArchBlue
                )
                Text(
                    text = "Bulan Semasa",
                    fontSize = 9.sp,
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                senaraiRekod.takeLast(10).forEach { item ->
                    val peratus = if (jumlahTugasanLalai > 0) {
                        (item.tugasan_siap.toFloat() / jumlahTugasanLalai.toFloat()) * 100f
                    } else 0f

                    val warnaKotak = when {
                        !item.status_hadir -> Color.DarkGray
                        item.tugasan_siap == 0 -> Color(0xFF374151)
                        peratus >= 100f -> Color(0xFF10B981)
                        peratus >= 50f -> Color(0xFFFBBF24)
                        else -> Color(0xFFEF4444)
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(warnaKotak),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (item.tugasan_siap > 0) "${item.tugasan_siap}" else "",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (peratus >= 50f && peratus < 100f) Color.Black else Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "* Petak memaparkan bilangan tugasan selesai per hari.",
                fontSize = 9.sp,
                color = Color.Gray
            )
        }
    }
}

// =====================================================================
// 📁 REPOSITORY SCREEN
// =====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryScreen(userRole: String, userEmail: String = "Guru") {
    val isGuru = userRole.equals("Guru", ignoreCase = true) || userEmail.contains("admin", ignoreCase = true)
    var bbmList by remember { mutableStateOf<List<BbmDto>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var currentFolderId by remember { mutableStateOf<Int?>(null) }
    val folderHistory = remember { mutableStateListOf<Int>() }
    val folderTitles = remember { mutableStateMapOf<Int, String>() }

    var showDialogTambah by remember { mutableStateOf(false) }
    var jenisSumbangan by remember { mutableStateOf("fail") }
    var tajukInput by remember { mutableStateOf("") }
    var pautanInput by remember { mutableStateOf("") }
    var subjekInput by remember { mutableStateOf("Jawi") }
    var darjahInput by remember { mutableStateOf("Darjah 3") }
    var topikInput by remember { mutableStateOf("") }
    var readmeInput by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

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

    val currentFolder = bbmList.firstOrNull { it.id == currentFolderId }
    val filteredList = bbmList.filter { item ->
        if (currentFolderId == null) item.parent_id == null || item.parent_id == 0
        else item.parent_id == currentFolderId
    }

    Scaffold(
        floatingActionButton = {
            if (isGuru) {
                FloatingActionButton(
                    onClick = { showDialogTambah = true },
                    containerColor = ArchBlue,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "📁 REPOSITORI OPEN-BBM",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(18.dp))
            }

            val pathText = "rulaf-hub / " +
                    folderHistory.map { id -> if (id == -1) "" else folderTitles[id]?.let { "$it / " } ?: "" }.joinToString("") +
                    (currentFolderId?.let { folderTitles[it] ?: "" } ?: "")

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = pathText,
                fontSize = 11.sp,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (currentFolderId != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (folderHistory.isNotEmpty()) {
                                val prev = folderHistory.removeAt(folderHistory.size - 1)
                                currentFolderId = if (prev == -1) null else prev
                            } else {
                                currentFolderId = null
                            }
                        }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = ArchBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kembali", color = ArchBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (currentFolder != null && !currentFolder.readme_text.isNullOrBlank()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(currentFolder.tajuk, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("BBM SELAMAT & TERPERCAYA", fontSize = 10.sp, color = ArchBlue, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(currentFolder.readme_text ?: "", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }

                items(filteredList) { bbm ->
                    if (bbm.is_folder == true) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    folderHistory.add(currentFolderId ?: -1)
                                    folderTitles[bbm.id] = bbm.tajuk
                                    currentFolderId = bbm.id
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Folder, contentDescription = null, tint = ArchBlue, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(bbm.tajuk, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("Folder Dokumentasi | Sumbangan: ${bbm.penyumbang ?: "Umum"}", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("📄 ${bbm.tajuk}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${bbm.subjek ?: "Umum"} - ${bbm.darjah ?: "Semua"} | Topik: ${bbm.topik ?: "Umum"}", fontSize = 10.sp, color = Color.Gray)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = {
                                            if (!bbm.pautan.isNullOrBlank()) {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(bbm.pautan))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "Pautan tidak sah!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = ArchBlue)
                                    ) {
                                        Text("Buka", fontSize = 11.sp)
                                    }

                                    IconButton(
                                        onClick = {
                                            bbm.pautan?.let { pautan ->
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, "Bahan BBM RuLaFHub: ${bbm.tajuk}\nPautan: $pautan")
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Kongsi BBM"))
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.Share, contentDescription = null, tint = ArchBlue, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialogTambah) {
        AlertDialog(
            onDismissRequest = { if (!isUploading) showDialogTambah = false },
            confirmButton = {
                Button(
                    onClick = {
                        if (jenisSumbangan == "fail" && (tajukInput.isBlank() || pautanInput.isBlank())) {
                            Toast.makeText(context, "Sila isi Tajuk dan Pautan fail!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (jenisSumbangan == "folder" && tajukInput.isBlank()) {
                            Toast.makeText(context, "Sila masukkan Nama Folder!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isUploading = true
                        scope.launch {
                            try {
                                val payload = if (jenisSumbangan == "fail") {
                                    TambahBbmRequest(
                                        tajuk = tajukInput.trim(),
                                        pautan = pautanInput.trim(),
                                        penyumbang = userEmail,
                                        subjek = subjekInput,
                                        darjah = darjahInput,
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
                                    topikInput = ""
                                    readmeInput = ""
                                    muatDataRepo()
                                } else {
                                    Toast.makeText(context, "Ralat pelayan: ${res.code()}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Ralat: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            } finally {
                                isUploading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                    enabled = !isUploading
                ) {
                    Text(if (isUploading) "Menerbitkan..." else "Terbitkan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialogTambah = false }, enabled = !isUploading) {
                    Text("Batal")
                }
            },
            title = {
                Text("Sumbangan BBM Guru", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
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
                            label = { Text("📁 Cipta Folder", fontSize = 11.sp) }
                        )
                    }

                    OutlinedTextField(
                        value = tajukInput,
                        onValueChange = { tajukInput = it },
                        label = { Text(if (jenisSumbangan == "fail") "Tajuk Bahan" else "Nama Folder") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    if (jenisSumbangan == "fail") {
                        OutlinedTextField(
                            value = pautanInput,
                            onValueChange = { pautanInput = it },
                            label = { Text("Pautan (Google Drive / Canva)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = subjekInput,
                                onValueChange = { subjekInput = it },
                                label = { Text("Subjek") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = darjahInput,
                                onValueChange = { darjahInput = it },
                                label = { Text("Darjah") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        OutlinedTextField(
                            value = topikInput,
                            onValueChange = { topikInput = it },
                            label = { Text("Topik / Bab") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    } else {
                        OutlinedTextField(
                            value = readmeInput,
                            onValueChange = { readmeInput = it },
                            label = { Text("Penerangan Folder (README)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                    }
                }
            }
        )
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
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "rulaf-config(1) - PROFIL & TETAPAN",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
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

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Peti Simpanan Kelayakan & Keselamatan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                                    authenticateWithBiometric(activity) { success ->
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

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode, contentDescription = "Theme")
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Dwi-Tema (Dark/Light)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Switch(checked = isDarkMode, onCheckedChange = { onThemeToggle() })
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

fun Context.findActivity(): FragmentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}

fun authenticateWithBiometric(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(false)
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(true)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(false)
            }
        })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Keselamatan RuLaFHub")
        .setSubtitle("Sahkan identiti anda menggunakan sidik jari.")
        .setNegativeButtonText("Batal")
        .build()

    try {
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        onResult(false)
    }
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
                                        "✓ [Hadir] $namaPaparan: $jumlahSiap/$sasaranSubjekHariIni subjek siap ($ringkasanSubjek)",
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
                    text = if (isSending) "MENYIMPAN..." else "[ SAHKAN KEHADIRAN & SUBJEK ]",
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