package com.digitalturbine.promptnews.util

import android.content.Context
import android.content.SharedPreferences
import com.digitalturbine.promptnews.data.UserLocation

object HomePrefs {
    private const val FILE = "home_prefs"
    const val KEY_LOCATION = "location" // e.g., "Overland Park, Kansas"
    const val KEY_LOCATION_CITY = "location_city"
    const val KEY_LOCATION_STATE = "location_state"
    private const val KEY_LOCATION_PROMPTED = "location_prompted"
    const val KEY_USER_NAME = "user_name"
    private const val KEY_HAS_SEEN_NAME_PROMPT = "has_seen_name_prompt"

    fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getLocation(context: Context): String {
        val userLocation = getUserLocation(context)
        return if (userLocation != null) {
            "${userLocation.city}, ${userLocation.state}"
        } else {
            val p: SharedPreferences = getPrefs(context)
            p.getString(KEY_LOCATION, "") ?: ""
        }
    }

    fun getUserLocation(context: Context): UserLocation? {
        val prefs = getPrefs(context)
        val city = prefs.getString(KEY_LOCATION_CITY, "").orEmpty()
        val state = prefs.getString(KEY_LOCATION_STATE, "").orEmpty()
        if (city.isBlank() || state.isBlank()) {
            return null
        }
        return UserLocation(city = city, state = state)
    }

    fun setUserLocation(context: Context, location: UserLocation?) {
        val editor = getPrefs(context).edit()
        if (location == null) {
            editor.remove(KEY_LOCATION_CITY)
            editor.remove(KEY_LOCATION_STATE)
            editor.putString(KEY_LOCATION, "")
        } else {
            editor.putString(KEY_LOCATION_CITY, location.city)
            editor.putString(KEY_LOCATION_STATE, location.state)
            editor.putString(KEY_LOCATION, "${location.city}, ${location.state}")
        }
        editor.apply()
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

    fun getUserName(context: Context): String? {
        val name = getPrefs(context).getString(KEY_USER_NAME, "").orEmpty().trim()
        return name.ifBlank { null }
    }

    fun saveUserName(context: Context, name: String) {
        getPrefs(context).edit().putString(KEY_USER_NAME, name.trim()).apply()
    }

    fun hasSeenNamePrompt(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_HAS_SEEN_NAME_PROMPT, false)
    }

    fun setHasSeenNamePrompt(context: Context, value: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_HAS_SEEN_NAME_PROMPT, value).apply()
    }
}
