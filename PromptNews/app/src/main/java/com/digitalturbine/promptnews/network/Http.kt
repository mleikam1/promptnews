package com.digitalturbine.promptnews.data.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object Http {

    private const val UA = "PromptNews/1.0 (Android)"
    // flip to true if you want wire logs
    private const val ENABLE_LOGGING = false

    val client: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (ENABLE_LOGGING) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }

        OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", UA)
                    .build()
                chain.proceed(req)
            }
            .addInterceptor(logging)
            .build()
    }

    fun req(url: String, extraHeaders: Map<String, String> = emptyMap()): Request {
        val b = Request.Builder().url(url).header("User-Agent", UA)
        extraHeaders.forEach { (k, v) -> b.header(k, v) }
        return b.build()
    }
}
