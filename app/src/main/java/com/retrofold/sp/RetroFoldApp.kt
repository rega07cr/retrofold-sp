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
        // Layout gamepad custom: opacità + offset gruppi tasti (dp).
        val PAD_OPACITY = androidx.datastore.preferences.core.floatPreferencesKey("pad_opacity")
        val PAD_DPAD_X = androidx.datastore.preferences.core.floatPreferencesKey("pad_dpad_x")
        val PAD_DPAD_Y = androidx.datastore.preferences.core.floatPreferencesKey("pad_dpad_y")
        val PAD_AB_X = androidx.datastore.preferences.core.floatPreferencesKey("pad_ab_x")
        val PAD_AB_Y = androidx.datastore.preferences.core.floatPreferencesKey("pad_ab_y")
        val PAD_SYS_X = androidx.datastore.preferences.core.floatPreferencesKey("pad_sys_x")
        val PAD_SYS_Y = androidx.datastore.preferences.core.floatPreferencesKey("pad_sys_y")
        // Stili nascosti: id separati da virgola + flag nascondi striscia.
        val HIDDEN_SKINS = stringPreferencesKey("hidden_skins")
        val HIDE_THUMBS = androidx.datastore.preferences.core.booleanPreferencesKey("hide_thumbs")
        // Layout per-gioco: se true, pad+schermo salvati per ogni ROM.
        val PER_GAME = androidx.datastore.preferences.core.booleanPreferencesKey("per_game_layout")
        val GAME_SCALE = androidx.datastore.preferences.core.floatPreferencesKey("game_scale")
        val GAME_DX = androidx.datastore.preferences.core.floatPreferencesKey("game_dx")
        val GAME_DY = androidx.datastore.preferences.core.floatPreferencesKey("game_dy")
        val LR_SCALE = androidx.datastore.preferences.core.floatPreferencesKey("lr_scale")
        val LR_DX = androidx.datastore.preferences.core.floatPreferencesKey("lr_dx")
        val LR_DY = androidx.datastore.preferences.core.floatPreferencesKey("lr_dy")
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
