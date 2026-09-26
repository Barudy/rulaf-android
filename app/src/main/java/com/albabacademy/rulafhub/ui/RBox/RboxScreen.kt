package com.albabacademy.rulafhub.ui.RBox

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.albabacademy.rulafhub.MainActivity
import com.albabacademy.rulafhub.data.remote.BbmDto
import com.albabacademy.rulafhub.data.remote.RetrofitClient
import com.albabacademy.rulafhub.data.remote.RulafBoxEdaranDto
import com.albabacademy.rulafhub.data.remote.UserProfileDto
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ArchBlue = Color(0xFF1793D1)
private val SystemGreen = Color(0xFF16A34A)

// =====================================================================
// 📦 RBOX (RULAFBOX) - Pengimbas NFC / QR untuk edaran BBM & modul
//    Privasi pertama: kad HANYA mengandungi token (RULAF-SEC-xxxx).
// =====================================================================
@Composable
fun RboxScreen(userRole: String, userEmail: String) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    val scope = rememberCoroutineScope()

    var sedangCari by remember { mutableStateOf(false) }
    var profilDitemui by remember { mutableStateOf<UserProfileDto?>(null) }
    var tokenAktif by remember { mutableStateOf<String?>(null) }
    var ralat by remember { mutableStateOf<String?>(null) }

    var senaraiBbm by remember { mutableStateOf<List<BbmDto>>(emptyList()) }
    var bahanDipilih by remember { mutableStateOf<BbmDto?>(null) }
    var statusSiapModul by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var kaedahImbas by remember { mutableStateOf("nfc") }

    // Muat senarai BBM untuk pilihan edaran
    LaunchedEffect(Unit) {
        try {
            senaraiBbm = withContext(Dispatchers.IO) { RetrofitClient.api.getBbmMaterials() }
                .sortedBy { it.tajuk }
        } catch (_: Exception) {}
    }

    fun cariMurid(token: String, kaedah: String, onJumpa: (UserProfileDto, String) -> Unit = { _, _ -> }) {
        scope.launch {
            sedangCari = true
            ralat = null
            try {
                val profil = withContext(Dispatchers.IO) {
                    RetrofitClient.api.getUserProfileByTokenId("eq.$token")
                }.firstOrNull()

                if (profil != null) {
                    onJumpa(profil, token)
                } else {
                    ralat = "Tiada murid dijumpai untuk token: $token"
                }
            } catch (e: Exception) {
                ralat = "Ralat memuat profil dari pelayan."
            } finally {
                sedangCari = false
            }
        }
    }

    // Kesan imbasan NFC token daripada MainActivity
    val tokenNFC = activity?.lastScannedToken
    LaunchedEffect(tokenNFC) {
        if (profilDitemui == null) {
            tokenNFC?.let { token ->
                kaedahImbas = "nfc"
                cariMurid(token, "nfc") { profile: UserProfileDto, _: String ->
                    profilDitemui = profile
                    tokenAktif = token
                }
            }
        }
    }

    val qrLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            val nilai = result.contents!!.trim()
            if (nilai.startsWith("RULAF-SEC", ignoreCase = true)) {
                kaedahImbas = "qr"
                cariMurid(nilai, "qr") { profile: UserProfileDto, token: String ->
                    profilDitemui = profile
                    tokenAktif = token
                }
            } else {
                ralat = "Kod QR tidak mengandungi token yang sah (RULAF-SEC-xxxxxx)."
            }
        } else {
            ralat = "Imbasan QR dibatalkan."
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
                    "📦 RBOX :: RU.LA.F.BOX",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = ArchBlue
                )
                Text(
                    "Imbas kad NFC atau kod QR murid untuk merekod edaran bahan BBM dan pengesahan siap modul.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Kotak pengimbas
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (tokenAktif == null) MaterialTheme.colorScheme.surfaceVariant else SystemGreen.copy(alpha = 0.1f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (sedangCari) {
                    CircularProgressIndicator(color = ArchBlue)
                    Text("Mencari murid...", fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                } else {
                    Text(
                        if (tokenAktif == null) "🪪" else "✅",
                        fontSize = 40.sp
                    )
                    Text(
                        text = if (tokenAktif == null) "Dekatkan kad NFC / imbas QR di bawah"
                        else "Murid Ditemui! Sila sahkan edaran.",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (tokenAktif == null) Color.Gray else SystemGreen,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Row(
                        modifier = Modifier.padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val options = ScanOptions()
                                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    .setPrompt("Imbas kod QR kad murid")
                                    .setCameraId(0)
                                    .setBeepEnabled(true)
                                qrLauncher.launch(options)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("📷 Imbas QR", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        if (tokenAktif != null) {
                            Button(
                                onClick = {
                                    profilDitemui = null
                                    tokenAktif = null
                                    bahanDipilih = null
                                    statusSiapModul = false
activity?.let { it.lastScannedToken = null }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Batal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        ralat?.let {
            Text(
                it,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
                fontFamily = FontFamily.Monospace
            )
        }

        // Petunjuk format token
        OutlinedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    "🔐 PRIVASI TOKEN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = ArchBlue,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Kad NFC & QR hanya menyimpan token (cth: RULAF-SEC-8F3A2C). Tiada MyKid atau nombor telefon pada kad. Sistem memetakan token ke profil murid secara selamat melalui Supabase.",
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }

    // ==========================================================
    // Lembaran bawah: sahkan edaran BBM + siap modul
    // ==========================================================
    if (profilDitemui != null && tokenAktif != null) {
        val profil = profilDitemui!!
        RulafBoxEdaranSheet(
            profil = profil,
            token = tokenAktif!!,
            kaedah = kaedahImbas,
            senaraiBbm = senaraiBbm.filter { it.is_folder != true },
            onDismiss = {
                profilDitemui = null
                tokenAktif = null
                activity?.lastScannedToken = null
            },
            onSahkan = { bahan, siapModul ->
                scope.launch {
                    isSaving = true
                    try {
                        val tarikh = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val body = RulafBoxEdaranDto(
                            token_id = tokenAktif,
                            mykid = profil.mykid ?: "",
                            nama_murid = profil.nama ?: profil.email ?: "",
                            bahan_bbm_id = bahan?.id?.toLong(),
                            bahan_bbm_tajuk = bahan?.tajuk,
                            tarikh_edaran = tarikh,
                            status_siap_modul = siapModul,
                            kaedah = kaedahImbas,
                            diimbas_oleh = userEmail
                        )
                        val res = withContext(Dispatchers.IO) {
                            RetrofitClient.api.logRulafBoxEdaran(body = body)
                        }
                        if (res.isSuccessful) {
                            Toast.makeText(context, "✓ Edaran BBM direkodkan!", Toast.LENGTH_SHORT).show()
                            profilDitemui = null
                            tokenAktif = null
                            bahanDipilih = null
                            statusSiapModul = false
                            activity?.lastScannedToken = null
                        } else {
                            Toast.makeText(context, "Ralat Pelayan: ${res.code()}", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Ralat rangkaian semasa menyimpan!", Toast.LENGTH_LONG).show()
                    } finally {
                        isSaving = false
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulafBoxEdaranSheet(
    profil: UserProfileDto,
    token: String,
    kaedah: String,
    senaraiBbm: List<BbmDto>,
    isSaving: Boolean = false,
    onDismiss: () -> Unit,
    onSahkan: (BbmDto?, Boolean) -> Unit
) {
    var bahanDipilih by remember { mutableStateOf<BbmDto?>(null) }
    var statusSiapModul by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("✅ Murid Disahkan", fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(
                "${profil.nama ?: profil.email ?: "-"} • MyKid: ${profil.mykid ?: "-"}",
                fontSize = 12.sp,
                color = SystemGreen,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "Token: $token  [$kaedah]",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontFamily = FontFamily.Monospace
            )

            Text(
                "PILIH BAHAN BBM DIEDAR:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = ArchBlue,
                fontFamily = FontFamily.Monospace
            )

            if (senaraiBbm.isEmpty()) {
                Text("Tiada bahan BBM tersedia.", fontSize = 11.sp, color = Color.Gray)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    senaraiBbm.take(8).forEach { bbm ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = bahanDipilih?.id == bbm.id,
                                onClick = { bahanDipilih = bbm }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bbm.tajuk, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text(
                                    "${bbm.subjek ?: ""} • ${bbm.darjah ?: ""}",
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = statusSiapModul,
                    onCheckedChange = { statusSiapModul = it }
                )
                Text("Lagakan: Modul Siap Dilengkapkan", fontSize = 12.sp)
            }

            Button(
                onClick = { onSahkan(bahanDipilih, statusSiapModul) },
                modifier = Modifier.fillMaxWidth().height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ArchBlue),
                shape = RoundedCornerShape(8.dp),
                enabled = !isSaving
            ) {
                Text(
                    if (isSaving) "MENYIMPAN..." else "✓ Sahkan & Rekod Edaran",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}