package com.aldiandrew.statusbarplus

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
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

    override fun onCreate(savedInstanceState: Bundle?) {
        applyThemeMode()
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        buildUi()
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        if (::daySwitch.isInitialized) updateState()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(MaterialToolbar(this).apply {
            title = getString(R.string.app_name)
            subtitle = getString(R.string.subtitle)
        }, LinearLayout.LayoutParams(-1, dp(76f)))

        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20f), dp(18f), dp(20f), dp(32f))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        content.addView(card().apply {
            addView(box().apply {
                addView(text(getString(R.string.hero_title), 27f, true), lp())
                addView(text(getString(R.string.hero_body), 15f, false), lp(8))
            })
        }, lp())

        content.addView(card().apply {
            addView(box().apply {
                addView(text(getString(R.string.preview_title), 18f, true), lp())
                preview = text("", 18f, false)
                addView(preview, lp(10))
                addView(text(getString(R.string.preview_body), 13f, false), lp(4))
            })
        }, lp(14))
        updatePreview()

        content.addView(card().apply {
            addView(box().apply {
                daySwitch = MaterialSwitch(this@MainActivity).apply {
                    text = getString(R.string.show_day)
                    textSize = 17f
                    setOnCheckedChangeListener { _, checked -> setEnabled(checked) }
                }
                addView(daySwitch, lp())
                status = text("", 13f, false)
                addView(status, lp(3))
                addView(MaterialButton(this@MainActivity).apply {
                    text = getString(R.string.notification_settings)
                    setOnClickListener {
                        startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        })
                    }
                }, lp(12))
            })
        }, lp(14))

        content.addView(card().apply {
            addView(box().apply {
                addView(text(getString(R.string.display_title), 18f, true), lp())
                addView(MaterialSwitch(this@MainActivity).apply {
                    text = getString(R.string.short_day)
                    isChecked = prefs.getBoolean("short_day", true)
                    setOnCheckedChangeListener { _, checked ->
                        prefs.edit().putBoolean("short_day", checked).apply()
                        DayNotificationManager.show(this@MainActivity)
                        updatePreview()
                    }
                }, lp(8))
                sizeLabel = text("", 15f, true)
                addView(sizeLabel, lp(12))
                addView(Slider(this@MainActivity).apply {
                    valueFrom = 9f
                    valueTo = 18f
                    value = prefs.getFloat("text_size", 13f)
                    stepSize = 1f
                    addOnChangeListener { _, value, _ ->
                        prefs.edit().putFloat("text_size", value).apply()
                        sizeLabel.text = getString(R.string.text_size, value.toInt())
                        DayNotificationManager.show(this@MainActivity)
                        updatePreview()
                    }
                }, lp(2))
                sizeLabel.text = getString(R.string.text_size, prefs.getFloat("text_size", 13f).toInt())
            })
        }, lp(14))

        content.addView(card().apply {
            addView(box().apply {
                addView(text(getString(R.string.theme_title), 18f, true), lp())
                addView(MaterialButton(this@MainActivity).apply {
                    text = themeLabel()
                    setOnClickListener { showThemeChooser() }
                }, lp(10))
            })
        }, lp(14))

        content.addView(text(getString(R.string.note), 13f, false), lp(18))
        setContentView(root)
        updateState()
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
        val pattern = if (prefs.getBoolean("short_day", true)) "EEE" else "EEEE"
        val day = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
        preview.text = "22:12   $day"
        preview.textSize = prefs.getFloat("text_size", 13f)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 100)
    }

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun showThemeChooser() {
        val choices = arrayOf(getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark))
        val selected = when (prefs.getString("theme_mode", "system")) {
            "light" -> 1
            "dark" -> 2
            else -> 0
        }
        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.theme_title))
            .setSingleChoiceItems(choices, selected) { dialog, which ->
                prefs.edit().putString("theme_mode", when (which) { 1 -> "light"; 2 -> "dark"; else -> "system" }).apply()
                dialog.dismiss()
                applyThemeMode()
                recreate()
            }.show()
    }

    private fun themeLabel() = when (prefs.getString("theme_mode", "system")) {
        "light" -> getString(R.string.theme_light)
        "dark" -> getString(R.string.theme_dark)
        else -> getString(R.string.theme_system)
    }

    private fun applyThemeMode() {
        AppCompatDelegate.setDefaultNightMode(when (prefs.getString("theme_mode", "system")) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        })
    }

    private fun card() = MaterialCardView(this).apply { radius = dp(28f).toFloat(); strokeWidth = dp(1f) }
    private fun box() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20f), dp(18f), dp(20f), dp(18f)) }
    private fun text(value: String, size: Float, bold: Boolean) = TextView(this).apply {
        text = value; textSize = size; gravity = Gravity.START
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(top.toFloat()) }
    private fun dp(value: Float) = (value * resources.displayMetrics.density + 0.5f).toInt()
}