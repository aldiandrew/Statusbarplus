package com.aldiandrew.statusbarplus

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {
    private const val FORMAT_VERSION = 1
    private const val SETTINGS_ENTRY = "settings.json"
    private const val FONT_ENTRY = "custom-font"

    fun writeBackup(context: Context, uri: Uri): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri)?.use { output ->
            ZipOutputStream(output).use { zip ->
                zip.putNextEntry(ZipEntry(SETTINGS_ENTRY))
                zip.write(settingsJson(context).toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()

                val font = java.io.File(context.filesDir, "fonts/custom-font")
                if (font.isFile && font.length() > 0L) {
                    zip.putNextEntry(ZipEntry(FONT_ENTRY))
                    font.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: return false
        true
    }.getOrDefault(false)

    fun restoreBackup(context: Context, uri: Uri): Boolean = runCatching {
        var settings: JSONObject? = null
        var fontBytes: ByteArray? = null

        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    when (entry.name) {
                        SETTINGS_ENTRY -> {
                            settings = JSONObject(
                                BufferedReader(InputStreamReader(zip, Charsets.UTF_8)).readText()
                            )
                        }
                        FONT_ENTRY -> if (!entry.isDirectory) {
                            fontBytes = zip.readBytes()
                        }
                    }
                    zip.closeEntry()
                }
            }
        } ?: return false

        val json = settings ?: return false
        if (json.optInt("formatVersion", -1) != FORMAT_VERSION) return false
        val values = json.optJSONObject("values") ?: return false

        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val editor = prefs.edit().clear()
        values.keys().forEach { key ->
            val item = values.optJSONObject(key) ?: return@forEach
            when (item.optString("type")) {
                "boolean" -> editor.putBoolean(key, item.optBoolean("value"))
                "float" -> editor.putFloat(key, item.optDouble("value").toFloat())
                "int" -> editor.putInt(key, item.optInt("value"))
                "long" -> editor.putLong(key, item.optLong("value"))
                "string" -> editor.putString(key, item.optString("value"))
            }
        }
        editor.apply()

        val target = java.io.File(context.filesDir, "fonts/custom-font")
        if (fontBytes != null) {
            target.parentFile?.mkdirs()
            target.outputStream().use { it.write(fontBytes) }
            val valid = runCatching {
                android.graphics.Typeface.createFromFile(target)
            }.isSuccess
            if (!valid) {
                target.delete()
                prefs.edit().putString("font_key", "system").apply()
            }
        } else {
            target.delete()
            if (prefs.getString("font_key", "system") == "custom") {
                prefs.edit().putString("font_key", "system").apply()
            }
        }

        true
    }.getOrDefault(false)

    private fun settingsJson(context: Context): JSONObject {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val values = JSONObject()

        prefs.all.forEach { (key, value) ->
            val item = JSONObject()
            when (value) {
                is Boolean -> item.put("type", "boolean").put("value", value)
                is Float -> item.put("type", "float").put("value", value.toDouble())
                is Int -> item.put("type", "int").put("value", value)
                is Long -> item.put("type", "long").put("value", value)
                is String -> item.put("type", "string").put("value", value)
                is Double -> item.put("type", "float").put("value", value)
                else -> return@forEach
            }
            values.put(key, item)
        }

        return JSONObject()
            .put("formatVersion", FORMAT_VERSION)
            .put("values", values)
            .put(
                "hasCustomFont",
                java.io.File(context.filesDir, "fonts/custom-font").isFile
            )
    }
}
