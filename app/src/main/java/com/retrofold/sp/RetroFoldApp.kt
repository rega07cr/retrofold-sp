package com.retrofold.sp

import android.app.Application
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Application.dataStore by preferencesDataStore(name = "retrofold_sp")

class RetroFoldApp : Application() {
    companion object {
        val SKIN_KEY = stringPreferencesKey("skin_id")
        val ROM_KEY = stringPreferencesKey("last_rom_id")
        val FILTER_KEY = stringPreferencesKey("ags101")
    }
}
