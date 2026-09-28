package com.aldiandrew.statusbarplus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
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
    private val prefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }
    private lateinit var daySwitch: MaterialSwitch
    private lateinit var status: TextView
    private lateinit var preview: TextView
    private lateinit var sizeLabel: TextView
    private lateinit var displayModeButton: MaterialButton
    private lateinit var backgroundButton: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeMode()
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        buildUi()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        if (::daySwitch.isInitialized) {
            updateState()
            updateBackgroundState()
        }
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
                setTitleTextAppearance(this@MainActivity, com.google.android.material.R.style.TextAppearance_Material3_TitleLarge)
                setSubtitleTextAppearance(this@MainActivity, com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
                elevation = 0f
            },
            LinearLayout.LayoutParams(-1, dp(64f))
        )

        val scroll = ScrollView(this).apply { clipToPadding = false }
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
                    addView(text(getString(R.string.hero_body), 14f, false), lp(4))
                    preview = text("", 18f, true).apply {
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
                        text = getString(R.string.show_day)
                        textSize = 16f
                        setOnCheckedChangeListener { _, checked -> setEnabled(checked) }
                    }
                    addView(daySwitch, lp())
                    status = text("", 13f, false)
                    addView(status, lp(2))
                    addView(
                        MaterialButton(this@MainActivity).apply {
                            text = getString(R.string.notification_settings)
                            setOnClickListener {
                                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                                })
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
                    addView(
                        MaterialSwitch(this@MainActivity).apply {
                            text = getString(R.string.short_day)
                            isChecked = prefs.getBoolean("short_day", true)
                            setOnCheckedChangeListener { _, checked ->
                                prefs.edit().putBoolean("short_day", checked).apply()
                                DayNotificationManager.show(this@MainActivity)
                                updatePreview()
                            }
                        },
                        lp(4)
                    )
                    displayModeButton = MaterialButton(this@MainActivity).apply {
                        text = displayModeLabel()
                        setOnClickListener { showDisplayModeChooser() }
                    }
                    addView(displayModeButton, lp(4))
                    sizeLabel = text("", 14f, true)
                    addView(sizeLabel, lp(8))
                    addView(
                        Slider(this@MainActivity).apply {
                            valueFrom = 14f
                            valueTo = 28f
                            value = prefs.getFloat("text_size", 18f).coerceIn(14f, 28f)
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
                        prefs.getFloat("text_size", 18f).coerceIn(14f, 28f).toInt()
                    )
                })
            },
            lp(10)
        )

        content.addView(
            card().apply {
                addView(box().apply {
                    addView(text(getString(R.string.background_title), 17f, true), lp())
                    addView(text(getString(R.string.background_body), 13f, false), lp(3))
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

    private fun setEnabled(enabled: Boolean) {
        if (enabled && !hasNotificationPermission()) {
            requestNotificationPermissionIfNeeded()
            window.decorView.postDelayed({ updateState() }, 700)
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
            !hasNotificationPermission() -> getString(R.string.permission_needed)
            enabled -> getString(R.string.active)
            else -> getString(R.string.inactive)
        }
    }

    private fun updatePreview() {
        if (!::preview.isInitialized) return
        val locale = Locale.getDefault()
        val pattern = if (prefs.getBoolean("short_day", true)) "EEE" else "EEEE"
        val day = SimpleDateFormat(pattern, locale).format(Date())
        val date = SimpleDateFormat("d", locale).format(Date())
        val month = SimpleDateFormat("MMM", locale).format(Date())
        preview.text = when (prefs.getString("display_mode", "day")) {
            "day_date" -> "12:34   $day\n             $date"
            "day_date_month" -> "12:34   $day\n             $date $month"
            "date_month" -> "12:34   $date\n             $month"
            else -> "12:34   $day"
        }
        preview.typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.NORMAL
        )
        preview.textSize = prefs.getFloat("text_size", 18f).coerceIn(14f, 28f)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        }
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val pm = getSystemService(PowerManager::class.java)
        return pm.isIgnoringBatteryOptimizations(packageName)
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
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        } catch (_: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
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
        "day_date_month" -> getString(R.string.mode_day_date_month)
        "date_month" -> getString(R.string.mode_date_month)
        else -> getString(R.string.mode_day)
    }

    private fun showDisplayModeChooser() {
        val choices = arrayOf(
            getString(R.string.mode_day),
            getString(R.string.mode_day_date),
            getString(R.string.mode_day_date_month),
            getString(R.string.mode_date_month)
        )
        val selected = when (prefs.getString("display_mode", "day")) {
            "day_date" -> 1
            "day_date_month" -> 2
            "date_month" -> 3
            else -> 0
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.display_mode_title))
            .setSingleChoiceItems(choices, selected) { dialog, which ->
                val mode = when (which) {
                    1 -> "day_date"
                    2 -> "day_date_month"
                    3 -> "date_month"
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