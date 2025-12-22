package com.digitalturbine.promptnews.util

import android.content.Context
import android.content.SharedPreferences

object HomePrefs {
    private const val FILE = "home_prefs"
    const val KEY_LOCATION = "location" // e.g., "Overland Park, Kansas"
    private const val KEY_LOCATION_PROMPTED = "location_prompted"

    fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getLocation(context: Context): String {
        val p: SharedPreferences = getPrefs(context)
        return p.getString(KEY_LOCATION, "") ?: ""
    }

    fun setLocation(context: Context, value: String) {
        getPrefs(context).edit().putString(KEY_LOCATION, value).apply()
    }

    fun wasLocationPrompted(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_LOCATION_PROMPTED, false)
    }

    fun setLocationPrompted(context: Context, value: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_LOCATION_PROMPTED, value).apply()
    }
}
