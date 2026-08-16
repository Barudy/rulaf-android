package com.albabacademy.rulafhub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.albabacademy.rulafhub.ui.permainan.GamePlayCompose

// =====================================================================
// THEME COLORS
// =====================================================================
private val DarkBg = Color(0xFF0F1419)
private val DarkCard = Color(0xFF171A21)
private val ArchBlue = Color(0xFF1793D1)

@Composable
fun RuLaFTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = ArchBlue,
            background = DarkBg,
            surface = DarkCard,
            onBackground = Color(0xFFA5B2D9),
            onSurface = Color.White
        ),
        content = content
    )
}

// =====================================================================
// MAIN ACTIVITY (KOTLIN ENTRY POINT)
// =====================================================================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RuLaFTheme {
                val navController = rememberNavController()

                Scaffold(
                    bottomBar = { RuLaFBottomNavigationBar(navController) }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") { DashboardPengurusanScreen() }
                        composable("arked") { GamePlayCompose() }
                        composable("repositori") { RepositoriKomunitiScreen() }
                        composable("profil") { ProfilGuruScreen() }
                    }
                }
            }
        }
    }
}

// =====================================================================
// BOTTOM NAVIGATION BAR (YOUTUBE STYLE)
// =====================================================================
@Composable
fun RuLaFBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("Dashboard", "dashboard", Icons.Filled.Home),
        BottomNavItem("Arked", "arked", Icons.Filled.PlayArrow),
        BottomNavItem("Repositori", "repositori", Icons.Filled.Share),
        BottomNavItem("Profil", "profil", Icons.Filled.Person)
    )

    NavigationBar(
        containerColor = DarkCard,
        contentColor = Color.Gray
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                label = { Text(text = item.title, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ArchBlue,
                    unselectedIconColor = Color.Gray,
                    indicatorColor = ArchBlue.copy(alpha = 0.2f)
                )
            )
        }
    }
}

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

// =====================================================================
// PLACEHOLDER SCREENS FOR NAVIGATION COMPILATION
// =====================================================================
@Composable
fun DashboardPengurusanScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Text("Dashboard Pengurusan (Data Supabase & Room DB)", color = Color.White, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun RepositoriKomunitiScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Text("Repositori Komuniti (GitHub Pendidik)", color = Color.White, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun ProfilGuruScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(DarkBg),
        contentAlignment = Alignment.Center
    ) {
        Text("Profil & Konfigurasi Guru", color = Color.White, fontFamily = FontFamily.Monospace)
    }
}
