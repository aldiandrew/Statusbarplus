package com.aldiandrew.statusbarplus

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.slider.Slider
import com.google.android.material.color.DynamicColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : android.app.Activity() {
    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        if (::root.isInitialized) {
            updateServiceState()
        }
    }

    private lateinit var root: LinearLayout
    private lateinit var serviceSwitch: MaterialSwitch
    private lateinit var serviceStatus: TextView

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFFFDFBFF.toInt())
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "Statusbarplus"
            subtitle = "Hari di status bar"
            elevation = dp(1f).toFloat()
        }
        root.addView(toolbar, LinearLayout.LayoutParams(-1, dp(72f)))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(18f), dp(20f), dp(28f))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        val title = text("Tampilkan hari sebagai teks", 24f, true)
        content.addView(title, lp())

        content.addView(text(
            "Alternatif no-root untuk menambahkan nama hari di sebelah jam. Tidak memakai root, Magisk, LSPosed, atau Xposed.",
            15f, false
        ), lp(0, 6))

        val preview = card()
        val previewBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(16f), dp(18f), dp(16f))
        }
        preview.addView(previewBox)
        previewBox.addView(text("Pratinjau", 17f, true), lp())
        val previewText = text("", 16f, false)
        previewText.tag = "preview"
        previewBox.addView(previewText, lp(0, 8))
        previewBox.addView(text("Contoh tampilan teks status bar", 13f, false), lp())
        content.addView(preview, lp(0, 16))
        updatePreview(previewText)

        val serviceCard = card()
        val serviceBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(16f), dp(18f), dp(16f))
        }
        serviceCard.addView(serviceBox)
        serviceSwitch = MaterialSwitch(this).apply {
            text = "Tampilkan hari"
            textSize = 16f
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium)
            setOnCheckedChangeListener { _, _ ->
                openAccessibilitySettings()
            }
        }
        serviceBox.addView(serviceSwitch, lp())
        serviceStatus = text("", 13f, false)
        serviceBox.addView(serviceStatus, lp(0, 2))
        val openButton = MaterialButton(this).apply {
            text = "Buka pengaturan aksesibilitas"
            setOnClickListener { openAccessibilitySettings() }
        }
        serviceBox.addView(openButton, lp(0, 12))
        content.addView(serviceCard, lp(0, 14))

        val formatCard = card()
        val formatBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(16f), dp(18f), dp(16f))
        }
        formatCard.addView(formatBox)
        formatBox.addView(text("Format hari", 17f, true), lp())
        val shortSwitch = MaterialSwitch(this).apply {
            text = "Gunakan singkatan (Sen)"
            isChecked = prefs.getBoolean("short_day", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("short_day", checked).apply()
                StatusBarAccessibilityService.refresh()
                updatePreview(previewText)
            }
        }
        formatBox.addView(shortSwitch, lp(0, 8))
        content.addView(formatCard, lp(0, 14))

        val offsetLabel = text("", 16f, true)
        content.addView(offsetLabel, lp(0, 12))
        val offsetSlider = Slider(this).apply {
            valueFrom = 35f
            valueTo = 180f
            value = prefs.getFloat("offset_dp", 58f)
            stepSize = 1f
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("offset_dp", value).apply()
                offsetLabel.text = "Posisi dari kiri: " + value.toInt() + " dp"
                StatusBarAccessibilityService.refresh()
            }
        }
        offsetLabel.text = "Posisi dari kiri: " + offsetSlider.value.toInt() + " dp"
        content.addView(offsetSlider, lp(0, 4))

        val sizeLabel = text("", 16f, true)
        content.addView(sizeLabel, lp(0, 12))
        val sizeSlider = Slider(this).apply {
            valueFrom = 9f
            valueTo = 18f
            value = prefs.getFloat("text_size", 13f)
            stepSize = 1f
            addOnChangeListener { _, value, _ ->
                prefs.edit().putFloat("text_size", value).apply()
                sizeLabel.text = "Ukuran teks: " + value.toInt() + " sp"
                StatusBarAccessibilityService.refresh()
                updatePreview(previewText)
            }
        }
        sizeLabel.text = "Ukuran teks: " + sizeSlider.value.toInt() + " sp"
        content.addView(sizeSlider, lp(0, 4))

        val darkSwitch = MaterialSwitch(this).apply {
            text = "Gunakan teks hitam"
            isChecked = prefs.getBoolean("dark_text", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("dark_text", checked).apply()
                StatusBarAccessibilityService.refresh()
            }
        }
        content.addView(darkSwitch, lp(0, 14))

        content.addView(text(
            "Catatan: Android tidak menyediakan API publik untuk mengubah SystemUI asli tanpa hak istimewa. Aplikasi ini menggunakan Accessibility Overlay untuk menggambar teks di atas status bar.",
            13f, false
        ), lp(0, 18))

        setContentView(root)
        updateServiceState()
    }

    private fun updateServiceState() {
        val enabled = isServiceEnabled()
        serviceSwitch.setOnCheckedChangeListener(null)
        serviceSwitch.isChecked = enabled
        serviceSwitch.setOnCheckedChangeListener { _, _ -> openAccessibilitySettings() }
        serviceStatus.text = if (enabled) "Statusbarplus sedang aktif." else "Aktifkan layanan agar hari muncul."
    }

    private fun updatePreview(view: TextView) {
        val short = prefs.getBoolean("short_day", false)
        val day = SimpleDateFormat(
            if (short) "EEE" else "EEEE",
            Locale("id", "ID")
        ).format(Date())
        view.text = "09:41    " + day
        view.textSize = prefs.getFloat("text_size", 13f)
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
            radius = dp(20f).toFloat()
            strokeWidth = dp(1f)
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
