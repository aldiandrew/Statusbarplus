package com.aldiandrew.statusbarplus

import android.content.Context
import android.graphics.Typeface
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

object FontManager {
    data class BuiltInFont(val key: String, val label: String, val url: String, val fileName: String)

    val builtInFonts = listOf(
        BuiltInFont("roboto", "Roboto", "https://raw.githubusercontent.com/google/fonts/main/apache/roboto/Roboto-Regular.ttf", "Roboto-Regular.ttf"),
        BuiltInFont("open_sans", "Open Sans", "https://raw.githubusercontent.com/google/fonts/main/ofl/opensans/OpenSans-Regular.ttf", "OpenSans-Regular.ttf"),
        BuiltInFont("montserrat", "Montserrat", "https://raw.githubusercontent.com/google/fonts/main/ofl/montserrat/Montserrat%5Bwght%5D.ttf", "Montserrat.ttf"),
        BuiltInFont("poppins", "Poppins", "https://raw.githubusercontent.com/google/fonts/main/ofl/poppins/Poppins-Regular.ttf", "Poppins-Regular.ttf"),
        BuiltInFont("lato", "Lato", "https://raw.githubusercontent.com/google/fonts/main/ofl/lato/Lato-Regular.ttf", "Lato-Regular.ttf"),
        BuiltInFont("nunito", "Nunito", "https://raw.githubusercontent.com/google/fonts/main/ofl/nunito/Nunito-Regular.ttf", "Nunito-Regular.ttf"),
        BuiltInFont("fira_sans", "Fira Sans", "https://raw.githubusercontent.com/google/fonts/main/ofl/firasans/FiraSans-Regular.ttf", "FiraSans-Regular.ttf"),
        BuiltInFont("noto_sans", "Noto Sans", "https://raw.githubusercontent.com/google/fonts/main/ofl/notosans/NotoSans-Regular.ttf", "NotoSans-Regular.ttf"),
        BuiltInFont("raleway", "Raleway", "https://raw.githubusercontent.com/google/fonts/main/ofl/raleway/Raleway-Regular.ttf", "Raleway-Regular.ttf"),
        BuiltInFont("oswald", "Oswald", "https://raw.githubusercontent.com/google/fonts/main/ofl/oswald/Oswald-Regular.ttf", "Oswald-Regular.ttf")
    )

    fun getTypeface(context: Context): Typeface {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val selected = prefs.getString("font_key", "system") ?: "system"
        if (selected == "system") return Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        if (selected == "custom") {
            val file = File(context.filesDir, "fonts/custom-font")
            return if (file.isFile) Typeface.createFromFile(file) else Typeface.DEFAULT
        }

        val font = builtInFonts.firstOrNull { it.key == selected } ?: return Typeface.DEFAULT
        val file = File(context.filesDir, "fonts/${font.fileName}")
        return if (file.isFile) Typeface.createFromFile(file) else Typeface.DEFAULT
    }

    fun isDownloaded(context: Context, font: BuiltInFont): Boolean =
        File(context.filesDir, "fonts/${font.fileName}").isFile

    fun download(context: Context, font: BuiltInFont, callback: (Boolean) -> Unit) {
        Thread {
            val dir = File(context.filesDir, "fonts").apply { mkdirs() }
            val target = File(dir, font.fileName)
            val temp = File(dir, "${font.fileName}.part")
            var ok = false
            try {
                val connection = (URL(font.url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    requestMethod = "GET"
                    instanceFollowRedirects = true
                }
                connection.connect()
                if (connection.responseCode in 200..299) {
                    connection.inputStream.use { input ->
                        temp.outputStream().use { output -> input.copyTo(output) }
                    }
                    if (temp.length() > 1024) {
                        if (target.exists()) target.delete()
                        ok = temp.renameTo(target)
                    }
                }
                connection.disconnect()
            } catch (_: Exception) {
                ok = false
            } finally {
                if (temp.exists()) temp.delete()
            }
            callback(ok)
        }.start()
    }

    fun importCustom(context: Context, source: android.net.Uri): Boolean {
        val dir = File(context.filesDir, "fonts").apply { mkdirs() }
        val target = File(dir, "custom-font")
        return try {
            context.contentResolver.openInputStream(source)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return false
            target.length() > 1024 && runCatching { Typeface.createFromFile(target) }.isSuccess
        } catch (_: Exception) {
            false
        }
    }
}
