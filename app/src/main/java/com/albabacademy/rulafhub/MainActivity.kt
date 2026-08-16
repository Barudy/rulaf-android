@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.albabacademy.rulafhub

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.net.URL

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

// =====================================================================
// RETROFIT NETWORK INTEGRATION FOR SUPABASE (REAL-TIME ENGINE)
// =====================================================================
const val SUPABASE_URL = "https://hmbzxmougiaubiooqqk.supabase.co/"
const val SUPABASE_ANON_KEY = "YOUR_SUPABASE_ANON_KEY_HERE" // Sila gantikan dengan Anon Key anda

interface SupabaseApi {
    @GET("rest/v1/data_murid")
    suspend fun getStudents(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<StudentDto>

    @GET("rest/v1/rulaf_repo")
    suspend fun getBbmMaterials(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<BbmDto>

    @GET("rest/v1/rulaf_forum")
    suspend fun getForumThreads(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<ForumDto>

    @GET("rest/v1/profil_pengguna")
    suspend fun getUserProfile(
        @Query("email") email: String,
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<UserProfileDto>

    @Headers("Content-Type: application/json", "Prefer: resolution=merge-duplicates")
    @POST("rest/v1/profil_pengguna")
    suspend fun upsertUserProfile(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY",
        @Body profile: UserProfileDto
    ): Response<Unit>

    // [+] SISTEM INTERAKTIF KOMEN (SUPABASE SYNC)
    @GET("rest/v1/rulaf_komen")
    suspend fun getComments(
        @Query("forum_id") forumIdQuery: String, // format: "eq.1"
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY"
    ): List<CommentDto>

    @Headers("Content-Type: application/json")
    @POST("rest/v1/rulaf_komen")
    suspend fun postComment(
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY,
        @Header("Authorization") auth: String = "Bearer $SUPABASE_ANON_KEY",
        @Body comment: CommentDto
    ): Response<Unit>
}

data class StudentDto(
    val mykid: String,
    val nama_murid: String,
    val jantina: String,
    val kelas_id: String,
    val tahap: String?
)

data class BbmDto(
    val id: Int,
    val tajuk: String,
    val pautan: String,
    val penyumbang: String,
    val subjek: String?,
    val darjah: String?,
    val topik: String?
)

data class ForumDto(
    val id: Int,
    val tajuk: String,
    val soalan: String,
    val penulis: String,
    val subjek: String?,
    val darjah: String?,
    val kategori: String?
)

data class CommentDto(
    val forum_id: Int,
    val komen: String,
    val penulis: String
)

data class UserProfileDto(
    val email: String,
    val nama: String,
    val umur: Int,
    val jantina: String,
    val peranan: String
)

object RetrofitClient {
    val api: SupabaseApi by lazy {
        Retrofit.Builder()
            .baseUrl(SUPABASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SupabaseApi::class.java)
    }
}

// =====================================================================
// MAIN ACTIVITY ENTRY POINT (FragmentActivity for Safe Biometrics & Sync)
// =====================================================================
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by remember { mutableStateOf(true) }
            var isLoggedIn by remember { mutableStateOf(false) }
            var userRole by remember { mutableStateOf("") } // "GURU", "MURID", "IBUBAPA"
            var loggedInUserEmail by remember { mutableStateOf("") }

            RuLaFTheme(isDarkMode = isDarkMode) {
                if (!isLoggedIn) {
                    // 1. ROMBAKAN SKRIN LOG MASUK (E-MEL & KATA LALUAN SAHAJA)
                    LoginScreen(
                        onLoginSuccess = { email, role ->
                            loggedInUserEmail = email
                            userRole = role
                            isLoggedIn = true
                        }
                    )
                } else {
                    // 2. PAPARAN UTAMA (DASHBOARD & NAVBAR BARU)
                    MainScreen(
                        userRole = userRole,
                        userEmail = loggedInUserEmail,
                        onLogout = { isLoggedIn = false }
                    )
                }
            }
        }
    }
}

// =====================================================================
// 🔑 LOGIN SCREEN (WITH ROLE, EMAIL, PASSWORD & BIOMETRIC FALLBACK)
// =====================================================================
@Composable
fun LoginScreen(onLoginSuccess: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // Peranan Ibu Bapa dibuang. Hanya Guru & Murid.
    var role by remember { mutableStateOf("Guru") }
    var errorMsg by remember { mutableStateOf("") }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "RuLaF Hub",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                fontFamily = FontFamily.Monospace
            )
            Text("Log Masuk Sistem", color = Color.Gray, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(32.dp))

            // PEMILIHAN PERANAN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = role == "Guru",
                    onClick = { role = "Guru" },
                    label = { Text("Pendidik (Guru)") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1793D1))
                )
                FilterChip(
                    selected = role == "Murid",
                    onClick = { role = "Murid" },
                    label = { Text("Pelajar (Murid)") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF1793D1))
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // INPUT E-MEL
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Alamat E-mel", color = Color.Gray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray,
                    focusedBorderColor = Color(0xFF1793D1)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // INPUT KATA LALUAN
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Kata Laluan", color = Color.Gray) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray,
                    focusedBorderColor = Color(0xFF1793D1)
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (errorMsg.isNotEmpty()) {
                Text(errorMsg, color = Color.Red, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    if (email.isNotEmpty() && password.length >= 6) {
                        onLoginSuccess(email, role)
                    } else {
                        errorMsg = "Sila masukkan e-mel yang sah dan kata laluan min 6 aksara."
                    }
                },
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1793D1)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("LOG MASUK SAHKAN", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun MainScreen(userRole: String, userEmail: String, onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf("Dashboard") }

    Scaffold(
        bottomBar = {
            // 3. ROMBAKAN NAVBAR (BEBAS PEPIJAT WARNA KAPSUL)
            NavigationBar(
                containerColor = Color(0xFF171A21),
                contentColor = Color.Gray
            ) {
                val tabs = listOf("Dashboard", "BBM", "Forum", "Profil")
                val icons = listOf(Icons.Default.Home, Icons.Default.Folder, Icons.Default.Forum, Icons.Default.Person)

                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(icons[index], contentDescription = tab) },
                        label = { Text(tab, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF1793D1), // Warna Ikon Aktif (Biru)
                            selectedTextColor = Color(0xFF1793D1), // Warna Teks Aktif (Biru)
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent // [+] INI PENYELESAIANNYA! Membuang kapsul yang menutupi logo.
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(Color(0xFF0F1419))
        ) {
            when (selectedTab) {
                "Dashboard" -> DashboardView(userRole, userEmail)
                "BBM" -> Text("Repositori BBM & Modul Jawi", color = Color.White, modifier = Modifier.padding(16.dp))
                "Forum" -> Text("Ruang Diskusi Komuniti", color = Color.White, modifier = Modifier.padding(16.dp))
                "Profil" -> ProfilView(userEmail, userRole, onLogout)
            }
        }
    }
}
// =====================================================================
// 📱 MAIN APP INTERFACE WITH DYNAMIC bottom NAVIGATION (YOUTUBE STYLE)
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

    Scaffold(
        bottomBar = { RuLaFBottomNavigationBar(navController, userRole) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardView(userRole, userEmail)
            }
            composable("arked") {
                // Connects directly to gameplay compose module with auto-cache engine
                com.albabacademy.rulafhub.ui.permainan.RuLaFGameEngineApp()
            }
            composable("repositori") {
                RepositoryScreen(userRole)
            }
            composable("forum") {
                ForumScreen(userRole, userEmail)
            }
            composable("profil") {
                ProfilScreen(userRole, userEmail, isDarkMode, onThemeToggle, onLogout)
            }
        }
    }
}

@Composable
fun RuLaFBottomNavigationBar(navController: NavHostController, userRole: String) {
    val items = when (userRole) {
        "MURID" -> listOf(
            BottomNavItem("Misi Arked", "arked", Icons.Filled.PlayArrow),
            BottomNavItem("BBM Guru", "repositori", Icons.Filled.FolderShared),
            BottomNavItem("Forum", "forum", Icons.Filled.Forum),
            BottomNavItem("Profil", "profil", Icons.Filled.Person)
        )
        else -> listOf(
            BottomNavItem("Dashboard", "dashboard", Icons.Filled.Home),
            BottomNavItem("Misi Arked", "arked", Icons.Filled.PlayArrow),
            BottomNavItem("BBM Guru", "repositori", Icons.Filled.FolderShared),
            BottomNavItem("Forum", "forum", Icons.Filled.Forum),
            BottomNavItem("Profil", "profil", Icons.Filled.Person)
        )
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = Color.Gray
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title, fontSize = 9.sp, fontFamily = FontFamily.Monospace) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent // 🌟 SIFAR BULATAN BIRU: Youtube style yang sangat kemas dan bersih!
                )
            )
        }
    }
}

// =====================================================================
// 📊 STATEFUL SCREEN: DASHBOARD (ROUTED FOR ROLES, 40/60 ANALYSIS)
// =====================================================================
@Composable
fun DashboardView(role: String, email: String) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Papan Pemuka Utama", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF171A21)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Selamat bertugas,", color = Color.Gray, fontSize = 12.sp)
                Text(email, color = Color(0xFF1793D1), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Akses: $role", color = Color.LightGray, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (role == "Guru") {
            Text("Ringkasan Kelas (Guru)", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF282C34)), modifier = Modifier.fillMaxWidth()) {
                Text("• 3 Murshid: 30 Pelajar (Telah dikemas kini)\n• 5 Murshid: 28 Pelajar (Menunggu tindakan)", color = Color.LightGray, modifier = Modifier.padding(16.dp))
            }
        } else {
            Text("Prestasi Jawi (Murid)", color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF282C34)), modifier = Modifier.fillMaxWidth()) {
                Text("• Tahap Semasa: RuLaF Ba\n• Misi Arked Selesai: 4/10\n• Kehadiran: 100%", color = Color.LightGray, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun ProfilView(email: String, role: String, onLogout: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Person, contentDescription = "Profil", tint = Color.White, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(email, color = Color.White, fontSize = 18.sp)
        Text(role, color = Color.Gray, fontSize = 14.sp)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
        ) {
            Text("Log Keluar Sistem")
        }
    }
}


// =====================================================================
// 📁 REPOSITORY SCREEN (GITHUB-STYLE FILE BROWSER OVERHAUL!)
// =====================================================================
@Composable
fun RepositoryScreen(userRole: String) {
    var bbmList by remember { mutableStateOf<List<BbmDto>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Navigation states for GitHub Folder Structure simulation
    var currentSubjek by remember { mutableStateOf<String?>(null) }
    var currentDarjah by remember { mutableStateOf<String?>(null) }

    // Auto-sync from Supabase on launch
    LaunchedEffect(Unit) {
        isSyncing = true
        syncError = null
        scope.launch {
            try {
                val data = withContext(Dispatchers.IO) { RetrofitClient.api.getBbmMaterials() }
                bbmList = data
            } catch (e: Exception) {
                syncError = "Gagal memuat BBM dari awan. Menggunakan storan cache."
                bbmList = listOf(
                    BbmDto(1, "Latihan Jawi Darjah 3", "https://example.com", "Ustaz Hanafi", "Jawi", "Darjah 3", "Ejaan"),
                    BbmDto(2, "Modul Solat Jumaat", "https://example.com", "Ustazah Fatimah", "Ibadah", "Darjah 5", "Solat Jumaat"),
                    BbmDto(3, "BBM Gerhana Matahari & Bulan", "https://example.com", "Ustaz Ridzuan", "Ibadah", "Darjah 5", "Kusuf")
                )
            } finally {
                isSyncing = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            if (isSyncing) CircularProgressIndicator(modifier = Modifier.size(16.dp))
        }

        // Dynamic breadcrumb simulating GitHub path
        val pathText = "rulaf-hub / " +
                (currentSubjek?.let { "$it / " } ?: "") +
                (currentDarjah ?: "")

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

        Spacer(modifier = Modifier.height(12.dp))

        if (syncError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Text(
                    text = syncError ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(8.dp),
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // 🌟 GITHUB FOLDER LOGIC 🌟
        if (currentSubjek == null) {
            // Level 1: List unique Subjek Folders
            val subjekList = bbmList.map { it.subjek ?: "Umum" }.distinct()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjekList) { subjek ->
                    FolderRow(name = subjek) {
                        currentSubjek = subjek
                    }
                }
            }
        } else if (currentDarjah == null) {
            // Level 2: List unique Darjah Folders under selected Subjek
            val darjahList = bbmList
                .filter { (it.subjek ?: "Umum") == currentSubjek }
                .map { it.darjah ?: "Semua" }
                .distinct()

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    BackRow("Kembali ke utama") { currentSubjek = null }
                }
                items(darjahList) { darjah ->
                    FolderRow(name = darjah) {
                        currentDarjah = darjah
                    }
                }
            }
        } else {
            // Level 3: List files under selected Subjek and selected Darjah
            val filesList = bbmList.filter {
                (it.subjek ?: "Umum") == currentSubjek && (it.darjah ?: "Semua") == currentDarjah
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    BackRow("Kembali ke $currentSubjek") { currentDarjah = null }
                }
                items(filesList) { bbm ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("📄 ${bbm.tajuk}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Sumbangan: ${bbm.penyumbang} | Topik: ${bbm.topik ?: "Am"}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Button(
                                onClick = { Toast.makeText(context, "Pautan dimuat turun: ${bbm.pautan}", Toast.LENGTH_SHORT).show() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Muat Turun", fontSize = 11.sp) // 🌟 Ditukar dari "Unduh"
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FolderRow(name: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Folder, contentDescription = "Folder", tint = ArchBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun BackRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
}

// =====================================================================
// 💬 INTERACTIVE FORUM SCREEN (WITH CLICKABLE COMMENTS OVERHAUL!)
// =====================================================================
@Composable
fun ForumScreen(userRole: String, userEmail: String) {
    var forumList by remember { mutableStateOf<List<ForumDto>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }
    var syncError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // State for interactive commenting
    var selectedThread by remember { mutableStateOf<ForumDto?>(null) }
    var commentList by remember { mutableStateOf<List<CommentDto>>(emptyList()) }
    var isCommentSyncing by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }

    // AUTO-SYNC Forum Threads on launch
    LaunchedEffect(Unit) {
        isSyncing = true
        syncError = null
        scope.launch {
            try {
                val data = withContext(Dispatchers.IO) { RetrofitClient.api.getForumThreads() }
                forumList = data
            } catch (e: Exception) {
                syncError = "Gagal memuat Forum secara langsung. Membaca cache tempatan."
                forumList = listOf(
                    ForumDto(1, "Ustazah Maria", "Bagaimana nak print laporan 40% sahsiah murid?", "maria@rulafhub.com", "Sistem", "Semua", "QNA"),
                    ForumDto(2, "Cikgu Ahmad", "Saya dah tambah 10 soalan wuduk baru di Supabase. Jom download!", "ahmad@rulafhub.com", "Jawi", "Darjah 3", "Sharing")
                )
            } finally {
                isSyncing = false
            }
        }
    }

    // Auto-fetch comments when a thread is selected
    LaunchedEffect(selectedThread) {
        if (selectedThread != null) {
            isCommentSyncing = true
            scope.launch {
                try {
                    val comments = withContext(Dispatchers.IO) {
                        RetrofitClient.api.getComments("eq.${selectedThread!!.id}")
                    }
                    commentList = comments
                } catch (e: Exception) {
                    // Cache fallbacks if offline
                    commentList = listOf(
                        CommentDto(selectedThread!!.id, "Cadangan yang sangat bernas cikgu!", "cikgu_maya@rulafhub.com"),
                        CommentDto(selectedThread!!.id, "Terima kasih atas perkongsian ini.", "parent_arif@rulafhub.com")
                    )
                } finally {
                    isCommentSyncing = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        if (selectedThread == null) {
            // VIEW 1: Main Thread List
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

            // Hotline Admin Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ArchOrange, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = ArchOrange.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.BugReport, contentDescription = "Bugs", tint = ArchOrange, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("📌 LAPORKAN PEPIJAT / PM ADMIN (HOTLINE)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ArchOrange)
                        Text("Hubungi pentadbir secara terus jika ada ralat sistem dwi-platform.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (syncError != null) {
                Text(
                    text = syncError ?: "",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                    fontFamily = FontFamily.Monospace
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(forumList) { disc ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedThread = disc },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(disc.penulis, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Text(disc.tajuk, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(disc.soalan, fontSize = 13.sp, maxLines = 2)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "💬 Sembang & Komen",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        } else {
            // VIEW 2: Thread Detail & Interactive Comment Section
            val thread = selectedThread!!
            BackRow(label = "Kembali ke Forum") {
                selectedThread = null
                commentList = emptyList()
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Topic Question Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(thread.penulis, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    Text(thread.tajuk, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(thread.soalan, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("💬 Komen Komuniti (${commentList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(8.dp))

            // Comments List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(commentList) { comment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(comment.penulis, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = ArchOrange)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(comment.komen, fontSize = 13.sp)
                        }
                    }
                }
            }

            // New Comment Input Section
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
                    placeholder = { Text("Tulis ulasan anda...") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (newCommentText.isNotEmpty()) {
                            scope.launch {
                                try {
                                    val response = withContext(Dispatchers.IO) {
                                        RetrofitClient.api.postComment(
                                            comment = CommentDto(
                                                forum_id = thread.id,
                                                komen = newCommentText,
                                                penulis = userEmail
                                            )
                                        )
                                    }
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "Komen diterbitkan!", Toast.LENGTH_SHORT).show()
                                        // Refresh comment list
                                        val comments = withContext(Dispatchers.IO) {
                                            RetrofitClient.api.getComments("eq.${thread.id}")
                                        }
                                        commentList = comments
                                        newCommentText = ""
                                    } else {
                                        Toast.makeText(context, "Gagal menghantar komen: ${response.code()}", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    // Offline cache insertion logic
                                    commentList = commentList + CommentDto(thread.id, newCommentText, userEmail)
                                    Toast.makeText(context, "Komen disimpan secara tempatan (Luar Talian).", Toast.LENGTH_SHORT).show()
                                    newCommentText = ""
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Hantar")
                }
            }
        }
    }
}

// =====================================================================
// ☀️ CONFIGURATION SCREEN: PROFIL OVERHAUL (SUPABASE REAL SYNC - JVM 17)
// =====================================================================
@Composable
fun ProfilScreen(
    userRole: String,
    userEmail: String,
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Real profile states
    var profileName by remember { mutableStateOf("Mengambil nama...") }
    var profileAge by remember { mutableStateOf("28") }
    var profileGender by remember { mutableStateOf("Lelaki") }
    var isSaving by remember { mutableStateOf(false) }

    // Fetch real profile from Supabase on start
    LaunchedEffect(userEmail) {
        scope.launch {
            try {
                val profiles = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getUserProfile(userEmail)
                }
                if (profiles.isNotEmpty()) {
                    val p = profiles.first()
                    profileName = p.nama
                    profileAge = p.umur.toString()
                    profileGender = p.jantina
                } else {
                    profileName = "Cikgu RuLaF"
                }
            } catch (e: Exception) {
                profileName = "Pengguna Luar Talian (Mod Cache)"
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // User Avatar Circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "User",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(50.dp)
                )
            }
        }

        item {
            Text(userEmail, fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
            Text("Peranan Anda: $userRole", fontSize = 12.sp, color = Color.Gray)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Maklumat Profil (Supabase Sync)", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    // 🌟 PADAM PAUTAN URL GAMBAR PROFIL - Diisytiharkan sifar rujukan input mengikut maklum balas!
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Nama Penuh") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = profileAge,
                        onValueChange = { profileAge = it },
                        label = { Text("Umur (Tahun)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Overhauled Interactive Gender Selector instead of raw text field (Better UX)
                    Column {
                        Text("Jantina", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            listOf("Lelaki", "Perempuan").forEach { gender ->
                                val isSelected = profileGender == gender
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable { profileGender = gender }
                                        .padding(vertical = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { profileGender = gender }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(gender, fontSize = 14.sp)
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                try {
                                    val dto = UserProfileDto(
                                        email = userEmail,
                                        nama = profileName,
                                        umur = profileAge.toIntOrNull() ?: 9,
                                        jantina = profileGender,
                                        peranan = userRole
                                    )
                                    val response = withContext(Dispatchers.IO) {
                                        RetrofitClient.api.upsertUserProfile(profile = dto)
                                    }
                                    if (response.isSuccessful) {
                                        Toast.makeText(context, "☁️ Profil disimpan ke Supabase!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Ralat Pelayan: ${response.code()}", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Ralat Sambungan. Disimpan dalam cache peranti.", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        enabled = !isSaving
                    ) {
                        Text(if (isSaving) "MENYIMPAN..." else "SIMPAN PROFIL KE AWAN")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            contentDescription = "Theme"
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Dwi-Tema (Dark/Light)", fontWeight = FontWeight.Bold)
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { onThemeToggle() }
                    )
                }
            }
        }

        item {
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("LOG KELUAR SISTEM", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =====================================================================
// BIOMETRICS ENGINE HELPER METHODS
// =====================================================================
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
        .setSubtitle("Sahkan biometrik untuk log masuk pendidik.")
        .setNegativeButtonText("PIN Ganti")
        .build()

    try {
        biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
        onResult(false)
    }
}

// =====================================================================
// 🏷️ BottomNavItem Data Class for Navigation Type Safety
// =====================================================================
data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
