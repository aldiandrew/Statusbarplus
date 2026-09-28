package com.aldiandrew.statusbarplus

import android.Manifest
import android.app.NotificationManager
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

    private lateinit var serviceSwitch: MaterialSwitch
    private lateinit var serviceStatus: TextView
    private lateinit var previewText: TextView
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
        if (::serviceSwitch.isInitialized) updateState()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val toolbar = MaterialToolbar(this).apply {
            title = getString(R.string.app_name)
            subtitle = getString(R.string.subtitle)
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
        heroBox.addView(text(getString(R.string.hero_title), 27f, true), lp())
        heroBox.addView(
            text(getString(R.string.hero_body), 15f, false),
            lp(8)
        )
        content.addView(hero, lp())

        val previewCard = card()
        val previewBox = box()
        previewCard.addView(previewBox)
        previewBox.addView(text(getString(R.string.preview_title), 18f, true), lp())
        previewText = text("", 18f, false)
        previewBox.addView(previewText, lp(10))
        previewBox.addView(
            text(getString(R.string.preview_body), 13f, false),
            lp(4)
        )
        content.addView(previewCard, lp(14))
        updatePreview()

        val serviceCard = card()
        val serviceBox = box()
        serviceCard.addView(serviceBox)

        serviceSwitch = MaterialSwitch(this).apply {
            text = getString(R.string.show_day)
            textSize = 17f
            setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    if (!hasNotificationPermission()) {
                        requestNotificationPermissionIfNeeded()
                        postDelayedCheck()
                    } else {
                        prefs.edit().putBoolean("enabled", true).apply()
                        DayNotificationManager.show(this@MainActivity)
                    }
                } else {
                    prefs.edit().putBoolean("enabled", false).apply()
                    DayNotificationManager.cancel(this@MainActivity)
                }
                updateState()
            }
        }
        serviceBox.addView(serviceSwitch, lp())

        serviceStatus = text("", 13f, false)
        serviceBox.addView(serviceStatus, lp(3))

        val settingsButton = MaterialButton(this).apply {
            text = getString(R.string.notification_settings)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                })
            }
        }
        serviceBox.addView(settingsButton, lp(12))
        content.addView(serviceCard, lp(14))

        val formatCard = card()
        val formatBox = box()
        formatCard.addView(formatBox)
        formatBox.addView(text(getString(R.string.display_title), 18f, true), lp())

        val shortSwitch = MaterialSwitch(this).apply {
            text = getString(R.string.short_day)
            isChecked = prefs.getBoolean("short_day", true)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("short_day", checked).apply()
                DayNotificationManager.show(this@MainActivity)
                updatePreview()
            }
        }
        formatBox.addView(shortSwitch, lp(8))

        sizeLabel = text("", 15f, true)
        formatBox.addView(sizeLabel, lp(12))

        val sizeSlider = Slider(this).apply {
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
        }
        sizeLabel.text = getString(R.string.text_size, sizeSlider.value.toInt())
        formatBox.addView(sizeSlider, lp(2))
        content.addView(formatCard, lp(14))

        val themeCard = card()
        val themeBox = box()
        themeCard.addView(themeBox)
        themeBox.addView(text(getString(R.string.theme_title), 18f, true), lp())
        val themeButton = MaterialButton(this).apply {
            text = themeLabel()
            setOnClickListener { showThemeChooser() }
        }
        themeBox.addView(themeButton, lp(10))
        content.addView(themeCard, lp(14))

        content.addView(
            text(getString(R.string.note), 13f, false),
            lp(18)
        )

        setContentView(root)
        updateState()
    }

    private fun postDelayedCheck() {
        window.decorView.postDelayed({ updateState() }, 700)
    }

    private fun updateState() {
        val enabled = prefs.getBoolean("enabled", false)
        serviceSwitch.setOnCheckedChangeListener(null)
        serviceSwitch.isChecked = enabled
        serviceSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) {
                if (!hasNotificationPermission()) {
                    requestNotificationPermissionIfNeeded()
                    postDelayedCheck()
                } else {
                    prefs.edit().putBoolean("enabled", true).apply()
                    DayNotificationManager.show(this)
                }
            } else {
                prefs.edit().putBoolean("enabled", false).apply()
                DayNotificationManager.cancel(this)
            }
            updateState()
        }

        serviceStatus.text = when {
            !hasNotificationPermission() -> getString(R.string.permission_needed)
            enabled -> getString(R.string.active)
            else -> getString(R.string.inactive)
        }
    }

    private fun updatePreview() {
        if (!::previewText.isInitialized) return
        val short = prefs.getBoolean("short_day", true)
        val pattern = if (short) "EEE" else "EEEE"
        val day = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
        previewText.text = "22:12   $day"
        previewText.textSize = prefs.getFloat("text_size", 13f)
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

    private fun showThemeChooser() {
        val choices = arrayOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        val current = prefs.getString("theme_mode", "system")
        val selected = when (current) {
            "light" -> 1
            "dark" -> 2
            else -> 0
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.theme_title))
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

    private fun lp(top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(top.toFloat())
        }

    private fun dp(value: Float): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()
}
