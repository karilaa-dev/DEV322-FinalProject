package com.bananaginger.noisedetector.data

import android.content.Context
import java.util.UUID

class InstallIdProvider(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "install_identity",
        Context.MODE_PRIVATE
    )

    fun getInstallId(): String {
        val existing = preferences.getString(KEY_INSTALL_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val created = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_INSTALL_ID, created).apply()
        return created
    }

    companion object {
        private const val KEY_INSTALL_ID = "install_id"
    }
}
