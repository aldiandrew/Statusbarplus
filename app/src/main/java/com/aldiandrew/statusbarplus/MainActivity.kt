package com.aldiandrew.statusbarplus

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StatusbarplusApp() }
    }

    override fun onResume() {
        super.onResume()
        setContent { StatusbarplusApp() }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun isServiceEnabled(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE)
            as android.view.accessibility.AccessibilityManager
        val expected = ComponentName(this, StatusBarAccessibilityService::class.java)
        return manager.getEnabledAccessibilityServiceList(
            android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any { info ->
            info.resolveInfo?.serviceInfo?.let {
                ComponentName(it.packageName, it.name) == expected
            } == true
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun StatusbarplusApp() {
        var enabled by remember { mutableStateOf(isServiceEnabled()) }
        var offset by remember { mutableFloatStateOf(prefs.getFloat("offset_dp", 58f)) }
        var textSize by remember { mutableFloatStateOf(prefs.getFloat("text_size", 13f)) }
        var shortDay by remember { mutableStateOf(prefs.getBoolean("short_day", false)) }
        var darkText by remember { mutableStateOf(prefs.getBoolean("dark_text", false)) }

        val today = remember {
            SimpleDateFormat("EEEE", Locale("id", "ID")).format(Date())
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("Statusbarplus") }) }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Hari di Status Bar", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Tampilkan nama hari sebagai teks di sebelah jam. Tidak membutuhkan root, Magisk, atau Xposed.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Pratinjau", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "09:41    " + if (shortDay) today.take(3) else today,
                            fontSize = textSize.sp
                        )
                        Text("Contoh tampilan status bar", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Tampilkan hari", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    if (enabled) "Layanan aktif" else "Layanan belum diaktifkan",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = enabled,
                                onCheckedChange = {
                                    openAccessibilitySettings()
                                }
                            )
                        }

                        Button(
                            onClick = { openAccessibilitySettings() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (enabled) "Buka pengaturan aksesibilitas" else "Aktifkan Statusbarplus")
                        }
                    }
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Format hari", style = MaterialTheme.typography.titleMedium)
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (shortDay) "Singkat: Sen" else "Lengkap: Senin")
                            Switch(
                                checked = shortDay,
                                onCheckedChange = {
                                    shortDay = it
                                    prefs.edit().putBoolean("short_day", it).apply()
                                    StatusBarAccessibilityService.refresh()
                                }
                            )
                        }
                    }
                }

                Text("Posisi dari kiri: " + offset.toInt() + " dp", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = offset,
                    onValueChange = {
                        offset = it
                        prefs.edit().putFloat("offset_dp", it).apply()
                        StatusBarAccessibilityService.refresh()
                    },
                    valueRange = 35f..180f
                )

                Text("Ukuran teks: " + textSize.toInt() + " sp", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = textSize,
                    onValueChange = {
                        textSize = it
                        prefs.edit().putFloat("text_size", it).apply()
                        StatusBarAccessibilityService.refresh()
                    },
                    valueRange = 9f..18f
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Teks hitam")
                        Text("Matikan jika status bar memakai teks terang.", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = darkText,
                        onCheckedChange = {
                            darkText = it
                            prefs.edit().putBoolean("dark_text", it).apply()
                            StatusBarAccessibilityService.refresh()
                        }
                    )
                }

                Text(
                    "Android tidak menyediakan API publik untuk mengubah SystemUI asli tanpa hak istimewa. Statusbarplus memakai Accessibility Overlay sehingga teks digambar di atas status bar tanpa root/Xposed.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
