package com.digitalturbine.promptnews.util

import android.content.Context
import android.content.SharedPreferences

object HomePrefs {
    private const val FILE = "home_prefs"
    private const val KEY_LOCATION = "location" // e.g., "Overland Park, Kansas"

    fun getLocation(context: Context): String {
        val p: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return p.getString(KEY_LOCATION, "Overland Park, Kansas") ?: "Overland Park, Kansas"
    }

    fun setLocation(context: Context, value: String) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit().putString(KEY_LOCATION, value).apply()
    }
}
