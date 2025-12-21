package com.digitalturbine.promptnews.data.common

import java.security.MessageDigest

object HashUtils {

    fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashed = digest.digest(value.toByteArray(Charsets.UTF_8))
        return hashed.joinToString("") { "%02x".format(it) }
    }
}
