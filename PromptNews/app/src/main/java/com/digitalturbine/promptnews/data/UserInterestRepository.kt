package com.digitalturbine.promptnews.data

import android.content.Context

interface UserInterestRepository {
    fun getSelectedInterests(): List<Interest>
    fun saveSelectedInterests(interests: List<Interest>)
    fun isOnboardingComplete(): Boolean
}

class UserInterestRepositoryImpl private constructor(
    private val context: Context
) : UserInterestRepository {

    companion object {
        private const val FILE = "user_interests"
        private const val KEY_SELECTED = "selected_interest_ids"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_completed"

        @Volatile
        private var instance: UserInterestRepositoryImpl? = null

        fun getInstance(context: Context): UserInterestRepositoryImpl {
            return instance ?: synchronized(this) {
                instance ?: UserInterestRepositoryImpl(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    override fun getSelectedInterests(): List<Interest> {
        val ids = prefs.getStringSet(KEY_SELECTED, emptySet()).orEmpty()
        if (ids.isEmpty()) return emptyList()
        return InterestCatalog.interests.filter { ids.contains(it.id) }
    }

    override fun saveSelectedInterests(interests: List<Interest>) {
        val ids = interests.map { it.id }.toSet()
        prefs.edit()
            .putStringSet(KEY_SELECTED, ids)
            .putBoolean(KEY_ONBOARDING_COMPLETE, true)
            .apply()
    }

    override fun isOnboardingComplete(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun clearSelections() {
        prefs.edit().remove(KEY_SELECTED).remove(KEY_ONBOARDING_COMPLETE).apply()
    }
}
