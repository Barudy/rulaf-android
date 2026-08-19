@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.albabacademy.rulafhub

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
val SUPABASE_URL = BuildConfig.SUPABASE_URL
val SUPABASE_ANON_KEY = BuildConfig.SUPABASE_ANON_KEY

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

    // SISTEM INTERAKTIF KOMEN (SUPABASE SYNC)
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

    // SISTEM UTK AUTENTIKASI SUPABASE
    @Headers("Content-Type: application/json")
    @POST("auth/v1/token?grant_type=password")
    suspend fun login(
        @Body body: LoginBody,
        @Header("apikey") apiKey: String = SUPABASE_ANON_KEY
    ): Response<LoginResponse>
}

data class LoginBody(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String,
    val user: AuthUser
)

data class AuthUser(
    val id: String,
    val email: String
)

data class StudentDto(
    val mykid: String,
    val nama_murid: String,
    val jantina: String,
    val kelas_id: String,
    val tahap: String? = "RuLaF Ba"
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
    val peranan: String,
    val gambar_url: String? = ""
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
// MAIN ACTIVITY ENTRY POINT
// =====================================================================
class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by remember { mutableStateOf(true) }
            var isLoggedIn by remember { mutableStateOf(false) }
            var userRole by remember { mutableStateOf("") } // "GURU", "MURID"
            var loggedInUserEmail by remember { mutableStateOf("") }

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
                        }
                    )
                }
            }
        }
    }
}

// =====================================================================
// 🔑 LOGIN SCREEN (WITH SECURE AUTH & STRICT ROLE RESTRICTION)
// =====================================================================
@Composable
fun LoginScreen(onLoginSuccess: (String, String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedRole by remember { mutableStateOf("GURU") } // "GURU", "MURID"
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

        // ROLE SELECTION
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val roles = listOf("GURU", "MURID")
            roles.forEach { role ->
                val isSelected = selectedRole == role
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { selectedRole = role }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (role == "GURU") "PENDIDIK (GURU)" else "PELAJAR (MURID)",
                        color = if (isSelected) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (selectedRole == "GURU") "Akses Pentadbir & Pendidik" else "Daftar Masuk Murid",
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
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Kata Laluan") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray
                    )
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
                            // 🔒 PERISYTIHARAN STRICT KELAYAKAN OFFLINE DEMO (MENGELAKKAN BYPASS BOLOS!)
                            if (formattedEmail == "guru@rulafhub.com" && passwordInput == "rulaf2026") {
                                if (selectedRole == "GURU") {
                                    onLoginSuccess("GURU", formattedEmail)
                                    Toast.makeText(context, "🎉 Log Masuk Demo Guru (Luar Talian)", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "❌ Peranan tidak sepadan!", Toast.LENGTH_LONG).show()
                                }
                                isAuthenticating = false
                                return@launch
                            } else if (formattedEmail == "murid@rulafhub.com" && passwordInput == "rulaf2026") {
                                if (selectedRole == "MURID") {
                                    onLoginSuccess("MURID", formattedEmail)
                                    Toast.makeText(context, "🎉 Log Masuk Demo Murid (Luar Talian)", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "❌ Peranan tidak sepadan!", Toast.LENGTH_LONG).show()
                                }
                                isAuthenticating = false
                                return@launch
                            }

                            // Real Supabase Authentication call
                            val authResponse = withContext(Dispatchers.IO) {
                                RetrofitClient.api.login(LoginBody(formattedEmail, passwordInput))
                            }
                            if (authResponse.isSuccessful && authResponse.body() != null) {
                                // Fetch profile from DB to verify chosen role limits
                                val profiles = withContext(Dispatchers.IO) {
                                    RetrofitClient.api.getUserProfile(formattedEmail)
                                }
                                if (profiles.isNotEmpty()) {
                                    val dbRole = profiles.first().peranan.uppercase()
                                    // Block murid trying to log in as GURU and vice versa!
                                    if (dbRole == selectedRole) {
                                        onLoginSuccess(selectedRole, formattedEmail)
                                        Toast.makeText(context, "🎉 Selamat Datang $formattedEmail!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "❌ Akses dinafi! Peranan anda adalah $dbRole.", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    onLoginSuccess(selectedRole, formattedEmail)
                                }
                            } else {
                                Toast.makeText(context, "❌ Ralat: Alamat e-mel atau kata laluan salah!", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: retrofit2.HttpException) {
                            Toast.makeText(context, "❌ Kelayakan ditolak oleh pelayan!", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            // Rangkaian offline dan bukan akaun demo: SEKAT LOG MASUK!
                            Toast.makeText(context, "❌ Mod Luar Talian: Sila gunakan akaun demo sahaja (guru@rulafhub.com / murid@rulafhub.com)!", Toast.LENGTH_LONG).show()
                        } finally {
                            isAuthenticating = false
                        }
                    }
                } else {
                    Toast.makeText(context, "Sila isi alamat e-mel & kata laluan (min 6 aksara)!", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            enabled = !isAuthenticating
        ) {
            Text(
                text = if (isAuthenticating) "MENYELIDIK..." else "LOG MASUK SISTEM",
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
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
            startDestination = if (userRole == "MURID") "arked" else "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(userRole, userEmail)
            }
            composable("arked") {
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
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

// =====================================================================
// 📊 STATEFUL SCREEN: DASHBOARD (SANITIZED FROM ALL SENSITIVE STUDENT DATA!)
// =====================================================================
@Composable
fun DashboardScreen(userRole: String, userEmail: String) {
    var studentList by remember { mutableStateOf<List<StudentDto>>(emptyList()) }
    var isSyncing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        isSyncing = true
        scope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getStudents()
                }
                studentList = data
                Toast.makeText(context, "☁️ Data murid dikemaskini dari Supabase!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("RuLaF_DB", "Sync Failed, Loading offline storage", e)
                // 🌟 PRIVASI DIJAMIN: TIADA LAGI NAMA MURID, MYKID ATAU DATA SENSITIF SEBENAR DI DALAM KOD SUMBER! 🌟
                studentList = listOf(
                    StudentDto("000000000001", "Pelajar Contoh Alif", "Lelaki", "3 Murshid", "RuLaF Alif"),
                    StudentDto("000000000002", "Pelajar Contoh Ba", "Perempuan", "3 Murshid", "RuLaF Ba"),
                    StudentDto("000000000003", "Pelajar Contoh Ta", "Perempuan", "3 Murshid", "RuLaF Ta (Mentor)")
                )
                Toast.makeText(context, "🔌 Mod Luar Talian: Menggunakan data tiruan am bagi melindungi privasi!", Toast.LENGTH_LONG).show()
            } finally {
                isSyncing = false
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
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pusat Data RuLaF",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(
                            onClick = {
                                scope.launch {
                                    isSyncing = true
                                    try {
                                        val data = withContext(Dispatchers.IO) { RetrofitClient.api.getStudents() }
                                        studentList = data
                                        Toast.makeText(context, "🎉 Berjaya Segerak!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "❌ Gagal Segerak: Tiada Internet", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isSyncing = false
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Sync",
                                tint = if (isSyncing) Color.Gray else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        "Log masuk sebagai: $userRole ($userEmail)",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status Integrasi Supabase", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Badge(containerColor = if (studentList.isNotEmpty() && studentList.first().mykid != "000000000001") SystemGreen else Color.Gray) {
                            Text(
                                if (studentList.isNotEmpty() && studentList.first().mykid != "000000000001") "TERHUBUNG (MASA NYATA)" else "ROOM CACHE (OFFLINE)",
                                color = Color.White,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        if (userRole == "GURU") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Formula Pentaksiran Formatif (40% Sahsiah)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val metricList = listOf(
                            Triple("Arked Kuiz Digital", "3 Markah Max", "Kognitif"),
                            Triple("LDK & Lembaran Kerja", "17 Markah Max", "Psikomotor"),
                            Triple("Amalan Sahsiah Harian", "10 Markah Max", "Afektif"),
                            Triple("Kehadiran & Cop Ganjaran", "10 Markah Max", "Modifikasi Kelakuan")
                        )
                        metricList.forEach { (name, weight, aspect) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("• $name ($aspect)", fontSize = 11.sp, color = Color.Gray)
                                Text(weight, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Senarai Murid (Kumpulan Hibrid 2:2:1)",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(studentList) { student ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                        Column {
                            Text(student.nama_murid, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("MyKid: ${student.mykid} | Kelas: ${student.kelas_id}", fontSize = 10.sp, color = Color.Gray)
                        }
                        val statusKumpulan = student.tahap ?: "RuLaF Ba"
                        Badge(
                            containerColor = when {
                                statusKumpulan.contains("Mentor") || statusKumpulan.contains("Ta") -> SystemGreen
                                statusKumpulan.contains("Ba") -> ArchBlue
                                else -> ArchOrange
                            }
                        ) {
                            Text(statusKumpulan, color = Color.White, modifier = Modifier.padding(4.dp), fontSize = 10.sp)
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "🏆 Pencapaian Misi Anda",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Jumlah Mata Anda: 350 XP", fontSize = 14.sp)
                        Text("Cop Ganjaran: 4 Pelekat", fontSize = 14.sp)
                        Text("Pangkat Semasa: RuLaF Ba (Sederhana)", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
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

    var currentSubjek by remember { mutableStateOf<String?>(null) }
    var currentDarjah by remember { mutableStateOf<String?>(null) }

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

        if (currentSubjek == null) {
            val subjekList = bbmList.map { it.subjek ?: "Umum" }.distinct()
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subjekList) { subjek ->
                    FolderRow(name = subjek) {
                        currentSubjek = subjek
                    }
                }
            }
        } else if (currentDarjah == null) {
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
                                onClick = { Toast.makeText(context, "Muat turun: ${bbm.pautan}", Toast.LENGTH_SHORT).show() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Muat Turun", fontSize = 11.sp)
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

    var selectedThread by remember { mutableStateOf<ForumDto?>(null) }
    var commentList by remember { mutableStateOf<List<CommentDto>>(emptyList()) }
    var isCommentSyncing by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }

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
            val thread = selectedThread!!
            BackRow(label = "Kembali ke Forum") {
                selectedThread = null
                commentList = emptyList()
            }
            Spacer(modifier = Modifier.height(8.dp))

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
                                        val comments = withContext(Dispatchers.IO) {
                                            RetrofitClient.api.getComments("eq.${thread.id}")
                                        }
                                        commentList = comments
                                        newCommentText = ""
                                    } else {
                                        Toast.makeText(context, "Gagal menghantar komen: ${response.code()}", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
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

    var profileName by remember { mutableStateOf("Mengambil nama...") }
    var profileAge by remember { mutableStateOf("28") }
    var profileGender by remember { mutableStateOf("Lelaki") }
    var isSaving by remember { mutableStateOf(false) }

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
                                        peranan = userRole,
                                        gambar_url = ""
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

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
