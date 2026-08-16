package com.albabacademy.rulafhub.ui.profil

import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

// [+] FUNGSI HELPER: Mencari FragmentActivity secara selamat bagi mengelakkan crash ClassCastException
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

@Composable
fun ProfilGuruScreen() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    var isAuthenticated by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isAuthenticated) Icons.Filled.LockOpen else Icons.Filled.Person,
            contentDescription = "Profil",
            modifier = Modifier.size(120.dp),
            tint = if (isAuthenticated) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isAuthenticated) {
            Text("Selamat Datang, Pentadbir!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Akses penuh sistem telah dibuka. Di sini kita akan letakkan konfigurasi sistem (Dwi-Tema) dan integrasi data Supabase.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        } else {
            Text("Sistem Dikunci", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Sila sahkan identiti biometrik anda untuk membuka kunci profil dan tetapan sistem.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(55.dp),
                onClick = {
                    if (activity != null) {
                        authenticateWithBiometric(activity) { success ->
                            isAuthenticated = success
                            if (success) {
                                Toast.makeText(context, "Akses Dibenarkan!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Pengesahan Gagal", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Ralat: FragmentActivity tidak ditemui.", Toast.LENGTH_LONG).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Fingerprint, contentDescription = "Biometrik")
                Spacer(Modifier.width(12.dp))
                Text("Imbas Cap Jari / Face ID", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
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
        .setSubtitle("Sahkan biometrik anda untuk log masuk.")
        .setNegativeButtonText("Batal")
        .build()

    biometricPrompt.authenticate(promptInfo)
}
