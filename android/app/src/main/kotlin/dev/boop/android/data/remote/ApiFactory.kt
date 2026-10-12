package dev.boop.android.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Builds a Retrofit-backed [BoopApi] for a given server, optionally authenticated. */
object ApiFactory {

    val json: Json = Json { ignoreUnknownKeys = true }

    fun create(baseUrl: String, credential: String? = null): BoopApi {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val builder = chain.request().newBuilder()
                    .header("Accept", "application/json")
                if (credential != null) {
                    builder.header("Authorization", "Bearer $credential")
                }
                chain.proceed(builder.build())
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(withTrailingSlash(baseUrl))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BoopApi::class.java)
    }

    private fun withTrailingSlash(url: String): String =
        if (url.endsWith("/")) url else "$url/"
}
