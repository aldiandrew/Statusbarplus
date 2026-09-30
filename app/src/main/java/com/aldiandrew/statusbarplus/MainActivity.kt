package com.aldiandrew.statusbarplus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.widget.Toast
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
    companion object {
        private const val REQUEST_CREATE_BACKUP = 400
        private const val REQUEST_RESTORE_BACKUP = 401
    }
    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }
    private lateinit var daySwitch: MaterialSwitch
    private lateinit var status: TextView
    private lateinit var preview: TextView
    private lateinit var sizeLabel: TextView
    private lateinit var displayModeButton: MaterialButton
    private lateinit var backgroundButton: MaterialButton
    private lateinit var fontButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeMode()
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        migrateLegacyPreferences()
        configureSystemBars()
        buildUi()
        showFirstUseGuideIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        if (::daySwitch.isInitialized) {
            if (prefs.getBoolean("enabled", false) && Settings.canDrawOverlays(this)) {
                DayNotificationManager.show(this)
            }
            updateState()
            updateBackgroundState()
            updatePreview()
        }
    }

    private fun migrateLegacyPreferences() {
        prefs.edit()
            .remove("date_format")
            .remove("layout_preset")
            .remove("text_alignment")
            .apply()

        if (prefs.getString("display_mode", "day") == "day_date_month") {
            prefs.edit().putString("display_mode", "day_date").apply()
        }
    }

    private fun configureSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val dark = when (prefs.getString("theme_mode", "system")) {
            "dark" -> true
            "light" -> false
            else -> (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        controller.isAppearanceLightStatusBars = !dark
        controller.isAppearanceLightNavigationBars = !dark
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(resolveColor(com.google.android.material.R.attr.colorSurface))
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, bars.top, view.paddingRight, bars.bottom)
            insets
        }

        root.addView(
            MaterialToolbar(this).apply {
                title = getString(R.string.app_name)
                subtitle = getString(R.string.subtitle)
                setTitleTextAppearance(
                    this@MainActivity,
                    com.google.android.material.R.style.TextAppearance_Material3_TitleLarge
                )
                setSubtitleTextAppearance(
                    this@MainActivity,
                    com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
                )
                elevation = 0f
            },
            LinearLayout.LayoutParams(-1, dp(64f))
        )

        val scroll = ScrollView(this).apply {
            clipToPadding = false
            overScrollMode = ScrollView.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16f), dp(8f), dp(16f), dp(20f))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        content.addView(
            card().apply {
                addView(box().apply {
                    addView(text(getString(R.string.hero_title), 22f, true), lp())
                    addView(text(getString(R.string.hero_body), 14f, false), lp(8))
                    preview = text("", 22f, true).apply {
                        gravity = Gravity.CENTER
                        setPadding(0, dp(12f), 0, dp(4f))
                    }
                    addView(preview, lp())
                })
            },
            lp()
        )
        updatePreview()

        content.addView(
            card().apply {
                addView(box().apply {
                    daySwitch = MaterialSwitch(this@MainActivity).apply {
                        // Material 3 switch without ON/OFF text labels.
                        showText = false
                        minWidth = dp(52f)
                        minimumWidth = dp(52f)
                        minHeight = dp(32f)
                        minimumHeight = dp(32f)
                        isChecked = prefs.getBoolean("enabled", false)
                        setOnCheckedChangeListener { _, checked -> setEnabled(checked) }
                    }
                    addView(daySwitch, lp())
                    status = text("", 13f, false)
                    addView(status, lp(2))
                    addView(
                        MaterialButton(this@MainActivity).apply {
                            text = getString(R.string.notification_settings)
                            setOnClickListener {
                                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                            }
                        },
                        lp(8)
                    )
                })
            },
            lp(10)
        )

        content.addView(
            card().apply {
                addView(box().apply {
                    addView(text(getString(R.string.display_title), 17f, true), lp())
                    addView(text(getString(R.string.display_body), 13f, false), lp(8))
                    displayModeButton = MaterialButton(this@MainActivity).apply {
                        text = displayModeLabel()
                        setOnClickListener { showDisplayModeChooser() }
                    }
                    addView(displayModeButton, lp(4))
                    sizeLabel = text("", 14f, true)
                    addView(sizeLabel, lp(8))
                    addView(
                        Slider(this@MainActivity).apply {
                            valueFrom = 12f
                            valueTo = 22f
                            value = prefs.getFloat("text_size", 20f).coerceIn(12f, 22f)
                            stepSize = 1f
                            addOnChangeListener { _, value, _ ->
                                prefs.edit().putFloat("text_size", value).apply()
                                sizeLabel.text = getString(R.string.text_size, value.toInt())
                                DayNotificationManager.show(this@MainActivity)
                                updatePreview()
                            }
                        },
                        lp()
                    )
                    sizeLabel.text = getString(
                        R.string.text_size,
                        prefs.getFloat("text_size", 20f).coerceIn(12f, 22f).toInt()
                    )
                })
            },
            lp(10)
        )

        content.addView(
            card().apply {
                addView(box().apply {
                    addView(text(getString(R.string.font_title), 17f, true), lp())
                    addView(text(getString(R.string.font_body), 13f, false), lp(8))
                    fontButton = MaterialButton(this@MainActivity).apply {
                        text = fontLabel()
                        setOnClickListener { showFontChooser() }
                    }
                    addView(fontButton, lp(8))
                })
            },
            lp(10)
        )

        content.addView(
            card().apply {
                addView(box().apply {
                    addView(text(getString(R.string.layout_title), 17f, true), lp())
                    addView(text(getString(R.string.layout_body), 13f, false), lp(8))
                    addLayoutSlider(this, R.string.horizontal_offset, "horizontal_offset", -12f, 12f, 0f)
                    addLayoutSlider(this, R.string.vertical_offset, "vertical_offset", -12f, 12f, 0f)
                    addLayoutSlider(this, R.string.layout_padding, "layout_padding", 0f, 18f, 4f)
                    addLayoutSlider(this, R.string.line_spacing, "line_spacing", -6f, 12f, 0f)
                    addView(
                        MaterialButton(this@MainActivity).apply {
                            text = getString(R.string.reset_layout)
                            setOnClickListener { resetTextLayout() }
                        },
                        lp(10)
                    )
                })
            },
            lp(10)
        )

        content.addView(
            card().apply {
                addView(box().apply {
                    addView(text(getString(R.string.backup_title), 17f, true), lp())
                    addView(text(getString(R.string.backup_body), 13f, false), lp(8))
                    addView(
                        MaterialButton(this@MainActivity).apply {
                            text = getString(R.string.backup_create)
                            setOnClickListener { createBackup() }
                        },
                        lp(8)
                    )
                    addView(
                        MaterialButton(this@MainActivity).apply {
                            text = getString(R.string.backup_restore)
                            setOnClickListener { chooseBackupToRestore() }
                        },
                        lp(4)
                    )
                })
            },
            lp(10)
        )

        content.addView(
            card().apply {
                addView(box().apply {
                    addView(text(getString(R.string.background_title), 17f, true), lp())
                    addView(text(getString(R.string.background_body), 13f, false), lp(8))
                    backgroundButton = MaterialButton(this@MainActivity).apply {
                        setOnClickListener { requestBatteryExemption() }
                    }
                    addView(backgroundButton, lp(8))
                })
            },
            lp(10)
        )

        content.addView(
            card().apply {
                addView(box().apply {
                    addView(text(getString(R.string.theme_title), 17f, true), lp())
                    addView(text(getString(R.string.theme_body), 13f, false), lp(8))
                    addView(MaterialButton(this@MainActivity).apply {
                        text = themeLabel()
                        setOnClickListener { showThemeChooser() }
                    }, lp(6))
                })
            },
            lp(10)
        )

        content.addView(text(getString(R.string.note), 12f, false), lp(12))
        setContentView(root)
        updateState()
        updateBackgroundState()
    }

    private fun addLayoutSlider(
        parent: LinearLayout,
        labelRes: Int,
        key: String,
        min: Float,
        max: Float,
        default: Float
    ) {
        val valueLabel = text("", 13f, true)
        parent.addView(text(getString(labelRes), 13f, false), lp(10))
        parent.addView(valueLabel, lp(2))
        parent.addView(
            Slider(this).apply {
                valueFrom = min
                valueTo = max
                stepSize = 1f
                value = prefs.getFloat(key, default).coerceIn(min, max)
                valueLabel.text = layoutValue(key, value)
                addOnChangeListener { _, newValue, _ ->
                    prefs.edit().putFloat(key, newValue).apply()
                    valueLabel.text = layoutValue(key, newValue)
                    DayNotificationManager.show(this@MainActivity)
                    updatePreview()
                }
            },
            lp()
        )
    }

    private fun resetTextLayout() {
        prefs.edit()
            .putFloat("horizontal_offset", 0f)
            .putFloat("vertical_offset", 0f)
            .putFloat("layout_padding", 4f)
            .putFloat("line_spacing", 0f)
            .apply()
        DayNotificationManager.show(this)
        recreate()
    }

    private fun layoutValue(key: String, value: Float): String = when (key) {
        "layout_padding" -> getString(R.string.padding_value, value.toInt())
        "line_spacing" -> getString(R.string.spacing_value, value.toInt())
        else -> getString(R.string.offset_value, value.toInt())
    }

    private fun setEnabled(enabled: Boolean) {
        if (enabled && !Settings.canDrawOverlays(this)) {
            prefs.edit().putBoolean("enabled", true).apply()
            Toast.makeText(this, getString(R.string.overlay_permission_needed), Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            updateState()
            return
        }
        prefs.edit().putBoolean("enabled", enabled).apply()
        if (enabled) DayNotificationManager.show(this) else DayNotificationManager.cancel(this)
        updateState()
    }

    private fun updateState() {
        val enabled = prefs.getBoolean("enabled", false)
        daySwitch.setOnCheckedChangeListener(null)
        daySwitch.isChecked = enabled
        daySwitch.setOnCheckedChangeListener { _, checked -> setEnabled(checked) }
        status.text = when {
            enabled && !Settings.canDrawOverlays(this) -> getString(R.string.overlay_permission_needed)
            enabled -> getString(R.string.active)
            else -> getString(R.string.inactive)
        }
    }

    private fun updatePreview() {
        if (!::preview.isInitialized) return
        val locale = Locale.getDefault()
        val now = Date()
        val day = SimpleDateFormat("EEE", locale).format(now)
        val date = SimpleDateFormat("d", locale).format(now)
        val month = SimpleDateFormat("MMM", locale).format(now)
        preview.text = when (prefs.getString("display_mode", "day")) {
            "day_date" -> "$day\n$date"
            "date_month" -> "$date\n$month"
            else -> day
        }
        preview.typeface = android.graphics.Typeface.create(
            FontManager.getTypeface(this),
            android.graphics.Typeface.BOLD
        )
        preview.textSize = prefs.getFloat("text_size", 20f).coerceIn(12f, 22f)
    }

    private fun showFirstUseGuideIfNeeded() {
        if (prefs.getBoolean("first_use_guide_shown", false)) return
        prefs.edit().putBoolean("first_use_guide_shown", true).apply()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.guide_title))
            .setMessage(getString(R.string.guide_body))
            .setPositiveButton(getString(R.string.guide_continue), null)
            .setNegativeButton(getString(R.string.guide_later), null)
            .show()
    }

    private fun requestNotificationPermissionIfNeeded() {
        // The calendar display no longer uses a regular notification.
    }

    private fun hasNotificationPermission(): Boolean = true

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
    }

    private fun updateBackgroundState() {
        if (!::backgroundButton.isInitialized) return
        if (isIgnoringBatteryOptimizations()) {
            backgroundButton.text = getString(R.string.background_allowed)
            backgroundButton.isEnabled = false
        } else {
            backgroundButton.text = getString(R.string.background_allow)
            backgroundButton.isEnabled = true
        }
    }

    private fun requestBatteryExemption() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun createBackup() {
        startActivityForResult(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                putExtra(Intent.EXTRA_TITLE, "Statusbarplus-backup.zip")
            },
            REQUEST_CREATE_BACKUP
        )
    }

    private fun chooseBackupToRestore() {
        startActivityForResult(
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
            },
            REQUEST_RESTORE_BACKUP
        )
    }

    private fun restoreBackupWithConfirmation(uri: Uri) {
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.backup_restore_confirm_title))
            .setMessage(getString(R.string.backup_restore_confirm_body))
            .setNegativeButton(getString(R.string.guide_later), null)
            .setPositiveButton(getString(R.string.backup_restore_confirm)) { _, _ ->
                Thread {
                    val success = BackupManager.restoreBackup(this, uri)
                    runOnUiThread {
                        if (success) {
                            if (prefs.getBoolean("enabled", false) && hasNotificationPermission()) {
                                DayNotificationManager.show(this)
                            } else {
                                DayNotificationManager.cancel(this)
                            }
                            Toast.makeText(
                                this,
                                getString(R.string.backup_restore_success),
                                Toast.LENGTH_SHORT
                            ).show()
                            applyThemeMode()
                            recreate()
                        } else {
                            Toast.makeText(
                                this,
                                getString(R.string.backup_restore_failed),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }.start()
            }
            .show()
    }

    private fun showThemeChooser() {
        val choices = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        val selected = when (prefs.getString("theme_mode", "system")) {
            "light" -> 1
            "dark" -> 2
            else -> 0
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.theme_title))
            .setSingleChoiceItems(choices, selected) { dialog, which ->
                prefs.edit().putString(
                    "theme_mode",
                    when (which) {
                        1 -> "light"
                        2 -> "dark"
                        else -> "system"
                    }
                ).apply()
                dialog.dismiss()
                applyThemeMode()
                recreate()
            }
            .show()
    }

    private fun displayModeLabel(): String = when (prefs.getString("display_mode", "day")) {
        "day_date" -> getString(R.string.mode_day_date)
        "date_month" -> getString(R.string.mode_date_month)
        else -> getString(R.string.mode_day)
    }

    private fun showDisplayModeChooser() {
        val choices = arrayOf(
            getString(R.string.mode_day),
            getString(R.string.mode_day_date),
            getString(R.string.mode_date_month)
        )
        val selected = when (prefs.getString("display_mode", "day")) {
            "day_date" -> 1
            "date_month" -> 2
            else -> 0
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.display_mode_title))
            .setSingleChoiceItems(choices, selected) { dialog, which ->
                val mode = when (which) {
                    1 -> "day_date"
                    2 -> "date_month"
                    else -> "day"
                }
                prefs.edit().putString("display_mode", mode).apply()
                displayModeButton.text = displayModeLabel()
                DayNotificationManager.show(this)
                updatePreview()
                dialog.dismiss()
            }
            .show()
    }

    private fun fontLabel(): String =
        if (prefs.getString("font_key", "system") == "custom") {
            getString(R.string.font_custom)
        } else {
            getString(R.string.font_system)
        }

    private fun showFontChooser() {
        val choices = arrayOf(
            getString(R.string.font_system),
            getString(R.string.font_custom)
        )
        val selected = if (prefs.getString("font_key", "system") == "custom") 1 else 0

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.font_title))
            .setSingleChoiceItems(choices, selected) { dialog, which ->
                if (which == 0) {
                    prefs.edit().putString("font_key", "system").apply()
                    fontButton.text = fontLabel()
                    DayNotificationManager.show(this)
                    updatePreview()
                    dialog.dismiss()
                } else {
                    dialog.dismiss()
                    startActivityForResult(
                        Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "font/*"
                        },
                        300
                    )
                }
            }
            .setNegativeButton(getString(R.string.guide_later), null)
            .show()
    }

    @Deprecated("Deprecated in Android API; kept for compatibility with the app's minSdk.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return

        when (requestCode) {
            300 -> {
                Thread {
                    val success = FontManager.importCustom(this, data.data!!)
                    runOnUiThread {
                        if (success) {
                            prefs.edit().putString("font_key", "custom").apply()
                            fontButton.text = fontLabel()
                            DayNotificationManager.show(this)
                            updatePreview()
                        } else {
                            Toast.makeText(
                                this,
                                getString(R.string.font_invalid),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.start()
            }
            REQUEST_CREATE_BACKUP -> {
                Thread {
                    val success = BackupManager.writeBackup(this, data.data!!)
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            if (success) getString(R.string.backup_create_success)
                            else getString(R.string.backup_create_failed),
                            if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                        ).show()
                    }
                }.start()
            }
            REQUEST_RESTORE_BACKUP -> restoreBackupWithConfirmation(data.data!!)
        }
    }


    private fun themeLabel() = when (prefs.getString("theme_mode", "system")) {
        "light" -> getString(R.string.theme_light)
        "dark" -> getString(R.string.theme_dark)
        else -> getString(R.string.theme_system)
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

    private fun card() = MaterialCardView(this).apply {
        radius = dp(20f).toFloat()
        strokeWidth = 0
        cardElevation = 0f
    }

    private fun box() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16f), dp(14f), dp(16f), dp(14f))
    }

    private fun text(value: String, size: Float, bold: Boolean) = TextView(this).apply {
        text = value
        textSize = size
        gravity = Gravity.START
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }

    private fun lp(top: Int = 0) =
        LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top.toFloat())
        }

    private fun dp(value: Float) =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun resolveColor(attr: Int): Int {
        val typed = obtainStyledAttributes(intArrayOf(attr))
        val color = typed.getColor(0, android.graphics.Color.TRANSPARENT)
        typed.recycle()
        return color
    }
}
