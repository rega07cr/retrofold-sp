package com.retrofold.sp

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import java.util.Date

val Application.dataStore by preferencesDataStore(name = "retrofold_sp")

class RetroFoldApp : Application() {
    companion object {
        val SKIN_KEY = stringPreferencesKey("skin_id")
        val ROM_KEY = stringPreferencesKey("last_rom_id")
        val FILTER_KEY = stringPreferencesKey("ags101")
        val LANG_KEY = stringPreferencesKey("lang")
        const val CRASH_FILE = "crash_last.txt"
    }

    override fun onCreate() {
        super.onCreate()
        // Reporter crash Java/Kotlin: salva lo stacktrace su file prima di morire,
        // così al riavvio l'app può mostrarlo e l'utente può inviarlo.
        // (I crash nativi mGBA vengono localizzati via file di stage, vedi bridge.)
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                File(filesDir, CRASH_FILE).writeText(
                    "${Date()} | ${Build.MANUFACTURER} ${Build.MODEL} " +
                        "| Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})\n" +
                        "Thread: ${t.name}\n$e\n" +
                        Log.getStackTraceString(e).take(12000)
                )
            } catch (_: Exception) {}
            prev?.uncaughtException(t, e)
        }
    }
}
