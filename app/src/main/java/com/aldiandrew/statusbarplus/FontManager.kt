package com.aldiandrew.statusbarplus

import android.content.Context
import android.graphics.Typeface
import java.io.File

object FontManager {
    private const val CUSTOM_FILE = "fonts/custom-font"

    fun getTypeface(context: Context): Typeface {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return if (prefs.getString("font_key", "system") == "custom") {
            val file = File(context.filesDir, CUSTOM_FILE)
            if (file.isFile) runCatching { Typeface.createFromFile(file) }.getOrDefault(Typeface.DEFAULT)
            else Typeface.DEFAULT
        } else {
            Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    }

    fun hasCustomFont(context: Context): Boolean = File(context.filesDir, CUSTOM_FILE).isFile

    fun importCustom(context: Context, source: android.net.Uri): Boolean {
        val dir = File(context.filesDir, "fonts").apply { mkdirs() }
        val target = File(dir, "custom-font")
        return try {
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return false

            target.length() > 1024 &&
                runCatching { Typeface.createFromFile(target) }.isSuccess
        } catch (_: Exception) {
            false
        }
    }
}
