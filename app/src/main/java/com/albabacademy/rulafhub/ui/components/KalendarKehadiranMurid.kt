package com.albabacademy.rulafhub.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.albabacademy.rulafhub.data.remote.RekodKerajinanDto
import java.text.SimpleDateFormat
import java.util.*

private val ArchBlue = Color(0xFF1793D1)
private val ArchOrange = Color(0xFFE95420)
private val SystemGreen = Color(0xFF10B981)

@Composable
fun KalendarKehadiranMurid(
    senaraiRekod: List<RekodKerajinanDto>,
    modifier: Modifier = Modifier
) {
    var calendarState by remember { mutableStateOf(Calendar.getInstance()) }
    var tarikhDipilih by remember { mutableStateOf<String?>(null) }

    val bulanTahunFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("ms", "MY")) }
    val yyyyMmDdFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val tarikhHariIni = remember { yyyyMmDdFormat.format(Date()) }

    // Petakan tarikh (yyyy-MM-dd) kepada rekod kehadiran
    val rekodMap = remember(senaraiRekod) {
        senaraiRekod.associateBy { it.tarikh.trim() }
    }

    // Kira struktur hari dalam bulan semasa
    val cal = remember(calendarState) {
        (calendarState.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val hariPertamaMinggu = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0: Ahad, 1: Isnin, dsb.
    val jumlahHariBulan = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val tahunSemasa = cal.get(Calendar.YEAR)
    val bulanSemasa = cal.get(Calendar.MONTH)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, ArchBlue.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 1. Header Kalendar (Tajuk & Butang Tukar Bulan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "📅 KALENDAR KEHADIRAN RULAF",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = ArchBlue
                    )
                    Text(
                        text = bulanTahunFormat.format(cal.time).uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(
                        onClick = {
                            val c = calendarState.clone() as Calendar
                            c.add(Calendar.MONTH, -1)
                            calendarState = c
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Bulan Lepas", tint = ArchBlue)
                    }
                    IconButton(
                        onClick = {
                            val c = calendarState.clone() as Calendar
                            c.add(Calendar.MONTH, 1)
                            calendarState = c
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Bulan Hadapan", tint = ArchBlue)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Bar Hari (Ahad - Sabtu)
            val namaHari = listOf("AH", "IS", "SE", "RA", "KH", "JU", "SA")
            Row(modifier = Modifier.fillMaxWidth()) {
                namaHari.forEach { h ->
                    Text(
                        text = h,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (h == "JU" || h == "AH") ArchOrange else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Divider(color = Color.Gray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(6.dp))

            // 3. Grid Tarikh
            val jumlahSlot = ((hariPertamaMinggu + jumlahHariBulan + 6) / 7) * 7
            for (minggu in 0 until (jumlahSlot / 7)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (hariIndeks in 0..6) {
                        val slotNombor = (minggu * 7) + hariIndeks
                        val hariBulan = slotNombor - hariPertamaMinggu + 1

                        if (hariBulan in 1..jumlahHariBulan) {
                            val tarikhString = String.format(Locale.US, "%04d-%02d-%02d", tahunSemasa, bulanSemasa + 1, hariBulan)
                            val rekod = rekodMap[tarikhString]
                            val isHariIni = tarikhString == tarikhHariIni
                            val isDipilih = tarikhDipilih == tarikhString
                            val isLatihTubiSahaja = rekod?.status_kehadiran == "latih_tubi"

                            // Warna status kehadiran
                            val warnaStatus = when (rekod?.status_kehadiran) {
                                "hadir" -> SystemGreen
                                "lewat" -> Color(0xFF8B5CF6)
                                "sakit" -> Color(0xFF38BDF8)
                                "berkenyataan" -> ArchOrange
                                "ponteng" -> Color(0xFFEF4444)
                                else -> if (rekod?.status_hadir == true && !isLatihTubiSahaja) SystemGreen else null
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when {
                                            isDipilih -> ArchBlue.copy(alpha = 0.25f)
                                            warnaStatus != null -> warnaStatus.copy(alpha = 0.15f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        BorderStroke(
                                            width = if (isHariIni) 1.5.dp else if (isDipilih) 1.dp else 0.5.dp,
                                            color = if (isHariIni) ArchBlue else if (isDipilih) ArchBlue else Color.Gray.copy(alpha = 0.15f)
                                        ),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .clickable { tarikhDipilih = tarikhString },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$hariBulan",
                                        fontSize = 11.sp,
                                        fontWeight = if (isHariIni || warnaStatus != null) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            warnaStatus != null -> warnaStatus
                                            isHariIni -> ArchBlue
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )

                                    if (warnaStatus != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(warnaStatus)
                                        )
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4. Perincian Tarikh yang Ditekan
            val rekodPilihan = tarikhDipilih?.let { rekodMap[it] }
            if (tarikhDipilih != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Status: $tarikhDipilih",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (rekodPilihan?.status_kehadiran) {
                                    "hadir" -> "● Hadir Penuh ke Kelas"
                                    "lewat" -> "● Hadir Lewat"
                                    "sakit" -> "● Cuti Sakit (MC)"
                                    "berkenyataan" -> "● Cuti Kebenaran Penjaga"
                                    "ponteng" -> "● Tidak Hadir (Tanpa Sebab)"
                                    "latih_tubi" -> "⭐ Latih Tubi Kendiri (+${rekodPilihan.tugasan_siap} Markah Kerajinan)"
                                    else -> "Tiada rekod persekolahan rasmi"
                                },
                                fontSize = 10.sp,
                                color = if (rekodPilihan?.status_kehadiran == "latih_tubi") ArchBlue else Color.Gray,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (rekodPilihan != null && rekodPilihan.tugasan_siap > 0) {
                            Text(
                                text = "Siap ${rekodPilihan.tugasan_siap} Subjek",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = ArchBlue
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 5. Petunjuk Warna (Legend)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem(color = SystemGreen, text = "Hadir")
                LegendItem(color = Color(0xFF8B5CF6), text = "Lewat")
                LegendItem(color = Color(0xFF38BDF8), text = "MC")
                LegendItem(color = ArchOrange, text = "Izin")
                LegendItem(color = Color(0xFFEF4444), text = "TH")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text = text, fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}