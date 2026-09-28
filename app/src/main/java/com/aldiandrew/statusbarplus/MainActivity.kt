package com.aldiandrew.statusbarplus

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    private lateinit var root: LinearLayout
    private lateinit var serviceSwitch: MaterialSwitch
    private lateinit var serviceStatus: TextView
    private lateinit var themeButton: MaterialButton
    private lateinit var autoPositionSwitch: MaterialSwitch
    private lateinit var offsetLabel: TextView
    private lateinit var offsetSlider: Slider
    private lateinit var previewText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeMode()
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) updateServiceState()
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "Statusbarplus"
            subtitle = "Hari di status bar"
            elevation = dp(1f).toFloat()
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(76f)))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(18f), dp(20f), dp(32f))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val hero = card()
        val heroBox = box()
        hero.addView(heroBox)
        heroBox.addView(text("Hari, langsung di sebelah jam", 27f, true), lp())
        heroBox.addView(
            text(
                "Posisi dihitung otomatis dari ukuran jam bawaan. Tidak perlu menggeser teks setiap kali format jam berubah.",
                15f, false
            ),
            lp(0, 8)
        )
        content.addView(hero, lp())

        val previewCard = card()
        val previewBox = box()
        previewCard.addView(previewBox)
        previewBox.addView(text("Pratinjau", 18f, true), lp())
        previewText = text("", 16f, false)
        previewBox.addView(previewText, lp(0, 10))
        previewBox.addView(
            text("Nama hari mengikuti bahasa/locale perangkat.", 13f, false),
            lp(0, 4)
        )
        content.addView(previewCard, lp(0, 14))
        updatePreview()

        val serviceCard = card()
        val serviceBox = box()
        serviceCard.addView(serviceBox)

        serviceSwitch = MaterialSwitch(this).apply {
            text = "Tampilkan hari"
            textSize = 17f
            isChecked = isServiceEnabled()
            setOnCheckedChangeListener { _, _ -> openAccessibilitySettings() }
        }
        serviceBox.addView(serviceSwitch, lp())

        serviceStatus = text("", 13f, false)
        serviceBox.addView(serviceStatus, lp(0, 3))

        val openButton = MaterialButton(this).apply {
            text = "Kelola aksesibilitas"
            setOnClickListener { openAccessibilitySettings() }
        }
        serviceBox.addView(openButton, lp(0, 12))
        content.addView(serviceCard, lp(0, 14))

        val positionCard = card()
        val positionBox = box()
        positionCard.addView(positionBox)
        positionBox.addView(text("Posisi status bar", 18f, true), lp())

        autoPositionSwitch = MaterialSwitch(this).apply {
            text = "Sesuaikan otomatis dengan jam"
            isChecked = !prefs.getBoolean("manual_position", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("manual_position", !checked).apply()
                offsetSlider.isEnabled = !checked
                offsetLabel.text = if (checked) {
                    "Otomatis: setelah lebar jam + jarak 7 dp"
                } else {
                    "Posisi manual: ${offsetSlider.value.toInt()} dp"
                }
                StatusBarAccessibilityService.refresh()
            }
        }
        positionBox.addView(autoPositionSwitch, lp(0, 8))

        offsetLabel = text("", 15f, true)
        positionBox.addView(offsetLabel, lp(0, 8))

        offsetSlider = Slider(this).apply {
            valueFrom = 35f
            valueTo = 180f
            value = prefs.getFloat("offset_dp", 58f)
            stepSize = 1f
            isEnabled = prefs.getBoolean("manual_position", false)
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("offset_dp", value).apply()
                offsetLabel.text = "Posisi manual: ${value.toInt()} dp"
                StatusBarAccessibilityService.refresh()
            }
        }
        offsetLabel.text = if (autoPositionSwitch.isChecked) {
            "Otomatis: setelah lebar jam + jarak 7 dp"
        } else {
            "Posisi manual: ${offsetSlider.value.toInt()} dp"
        }
        positionBox.addView(offsetSlider, lp(0, 2))
        content.addView(positionCard, lp(0, 14))

        val formatCard = card()
        val formatBox = box()
        formatCard.addView(formatBox)
        formatBox.addView(text("Tampilan hari", 18f, true), lp())

        val shortSwitch = MaterialSwitch(this).apply {
            text = "Singkat (Sen)"
            isChecked = prefs.getBoolean("short_day", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("short_day", checked).apply()
                StatusBarAccessibilityService.refresh()
                updatePreview()
            }
        }
        formatBox.addView(shortSwitch, lp(0, 8))

        val darkSwitch = MaterialSwitch(this).apply {
            text = "Teks hitam"
            isChecked = prefs.getBoolean("dark_text", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("dark_text", checked).apply()
                StatusBarAccessibilityService.refresh()
            }
        }
        formatBox.addView(darkSwitch, lp(0, 2))

        val sizeLabel = text("", 15f, true)
        formatBox.addView(sizeLabel, lp(0, 12))
        val sizeSlider = Slider(this).apply {
            valueFrom = 9f
            valueTo = 18f
            value = prefs.getFloat("text_size", 13f)
            stepSize = 1f
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("text_size", value).apply()
                sizeLabel.text = "Ukuran teks: ${value.toInt()} sp"
                StatusBarAccessibilityService.refresh()
                updatePreview()
            }
        }
        sizeLabel.text = "Ukuran teks: ${sizeSlider.value.toInt()} sp"
        formatBox.addView(sizeSlider, lp(0, 2))
        content.addView(formatCard, lp(0, 14))

        val themeCard = card()
        val themeBox = box()
        themeCard.addView(themeBox)
        themeBox.addView(text("Tema aplikasi", 18f, true), lp())
        themeButton = MaterialButton(this).apply {
            text = themeLabel()
            setOnClickListener { showThemeChooser() }
        }
        themeBox.addView(themeButton, lp(0, 10))
        content.addView(themeCard, lp(0, 14))

        content.addView(
            text(
                "Catatan: tanpa root/Xposed, Android tidak memberi API publik untuk mengubah SystemUI asli. Statusbarplus memakai Accessibility Overlay. Overlay otomatis disembunyikan saat layar mati atau jendela aktif terdeteksi fullscreen.",
                13f, false
            ),
            lp(0, 18)
        )

        setContentView(root)
        updateServiceState()
    }

    private fun updateServiceState() {
        val enabled = isServiceEnabled()
        serviceSwitch.setOnCheckedChangeListener(null)
        serviceSwitch.isChecked = enabled
        serviceSwitch.setOnCheckedChangeListener { _, _ -> openAccessibilitySettings() }
        serviceStatus.text = if (enabled) {
            "Aktif • posisi dan bahasa menyesuaikan otomatis."
        } else {
            "Belum aktif • hidupkan layanan untuk menampilkan hari."
        }
    }

    private fun updatePreview() {
        if (!::previewText.isInitialized) return
        val short = prefs.getBoolean("short_day", false)
        val locale = Locale.getDefault()
        val day = SimpleDateFormat(if (short) "EEE" else "EEEE", locale).format(Date())
        previewText.text = "21:59   $day"
        previewText.textSize = prefs.getFloat("text_size", 13f)
    }

    private fun showThemeChooser() {
        val choices = arrayOf("Sistem", "Terang", "Gelap")
        val current = prefs.getString("theme_mode", "system")
        val selected = when (current) {
            "light" -> 1
            "dark" -> 2
            else -> 0
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Pilih tema")
            .setSingleChoiceItems(choices, selected) { dialog, which ->
                val mode = when (which) {
                    1 -> "light"
                    2 -> "dark"
                    else -> "system"
                }
                prefs.edit().putString("theme_mode", mode).apply()
                dialog.dismiss()
                applyThemeMode()
                recreate()
            }
            .show()
    }

    private fun themeLabel(): String = when (prefs.getString("theme_mode", "system")) {
        "light" -> "Terang"
        "dark" -> "Gelap"
        else -> "Sistem"
    }

    private fun applyThemeMode() {
        AppCompatDelegate.setDefaultNightMode(
            when (prefs.getString("theme_mode", "system")) {
                "light" -> AppCompatDelegate.MODE_NIGHT_NO
                "dark" -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
        )
    }

    private fun isServiceEnabled(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE)
            as android.view.accessibility.AccessibilityManager
        val expected = ComponentName(this, StatusBarAccessibilityService::class.java)
        return manager.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        ).any { info ->
            info.resolveInfo?.serviceInfo?.let {
                ComponentName(it.packageName, it.name) == expected
            } == true
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun card(): MaterialCardView =
        MaterialCardView(this).apply {
            radius = dp(28f).toFloat()
            strokeWidth = dp(1f)
        }

    private fun box(): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(18f), dp(20f), dp(18f))
        }

    private fun text(value: String, size: Float, bold: Boolean) =
        TextView(this).apply {
            text = value
            textSize = size
            gravity = Gravity.START
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

    private fun lp(top: Int = 0, extraTop: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp((top + extraTop).toFloat())
        }

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
