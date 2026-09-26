package com.albabacademy.rulafhub.ui.Rvids

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.albabacademy.rulafhub.data.remote.RetrofitClient
import com.albabacademy.rulafhub.data.remote.RulafVideoDto
import com.albabacademy.rulafhub.dapatkanNamaFailFizikal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ArchBlue = Color(0xFF1793D1)

// =====================================================================
// 🎬 CIPTA VIDEO (RVIDS CREATOR) - untuk Guru / Admin
// =====================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CiptaVideoScreen(userRole: String, userEmail: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tajuk by remember { mutableStateOf("") }
    var kategori by remember { mutableStateOf("Jawi") }
    var urlVideo by remember { mutableStateOf<String?>(null) }
    var namaFailDipilih by remember { mutableStateOf<String?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var mesej by remember { mutableStateOf<String?>(null) }

    val senaraiKategori = listOf(
        "Jawi", "Ibadat", "Bahasa Arab", "Sirah", "Tauhid", "Adab", "Leaderboard"
    )

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            urlVideo = null
            scope.launch {
                isUploading = true
                mesej = null
                try {
                    val namaAsal = dapatkanNamaFailFizikal(context, uri)
                    val urlAwam = muatNaikVideoRvidsKeStoran(context, uri, namaAsal)
                    if (urlAwam != null) {
                        urlVideo = urlAwam
                        namaFailDipilih = namaAsal
                    } else {
                        mesej = "Gagal memuat naik video. Sila cuba lagi."
                    }
                } catch (e: Exception) {
                    mesej = "Ralat semasa memuat naik video."
                } finally {
                    isUploading = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "🎬 CIPTA VIDEO RVIDS",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = ArchBlue
                )
                Text(
                    "Muat naik video pendek pengajaran ke modul-rulaf (storan) dan siarkan ke suapan Rvids.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        OutlinedTextField(
            value = tajuk,
            onValueChange = { tajuk = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tajuk Video") },
            singleLine = true
        )

        Text(
            "KATEGORI:",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            senaraiKategori.forEach { cat ->
                androidx.compose.material3.FilterChip(
                    selected = kategori == cat,
                    onClick = { kategori = cat },
                    label = { Text(cat, fontSize = 9.sp) }
                )
            }
        }

        if (isUploading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp))
                Text("Memuat naik video ke storan awan...", fontSize = 11.sp)
            }
        } else {
            OutlinedButton(
                onClick = { videoPicker.launch("video/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (namaFailDipilih != null) "✓ $namaFailDipilih" else "Pilih Video MP4 Dari Telefon")
            }
        }

        mesej?.let {
            Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
        }

        Button(
            onClick = {
                if (tajuk.isBlank()) {
                    Toast.makeText(context, "Sila isi tajuk video.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (urlVideo == null) {
                    Toast.makeText(context, "Sila pilih & muat naik video dahulu.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    mesej = null
                    try {
                        val dto = RulafVideoDto(
                            tajuk_video = tajuk.trim(),
                            pautan_video = urlVideo!!,
                            kategori = kategori,
                            pencipta = userEmail,
                            is_aktif = true
                        )
                        val res = withContext(Dispatchers.IO) {
                            RetrofitClient.api.publishRulafVideo(body = dto)
                        }
                        if (res.isSuccessful) {
                            Toast.makeText(context, "✓ Video berjaya disiarkan ke Rvids!", Toast.LENGTH_SHORT).show()
                            tajuk = ""
                            urlVideo = null
                            namaFailDipilih = null
                        } else {
                            Toast.makeText(context, "Ralat Pelayan: ${res.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Ralat rangkaian semasa menyiarkan!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text("Siarkan Ke Rvids", fontWeight = FontWeight.Bold)
        }
    }
}