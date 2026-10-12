package dev.boop.android.data.remote

import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Builds Retrofit-backed API clients. */
object ApiFactory {

    val json: Json = Json { ignoreUnknownKeys = true }

    /** Pretty printer for showing raw event JSON. */
    val prettyJson: Json = Json { prettyPrint = true }

    // Shared connection pool and dispatcher; each client only adds an auth
    // interceptor, so building one per call stays cheap.
    private val baseClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /** A [BoopApi] for [baseUrl]; [credential] is sent as a bearer token when set. */
    fun create(baseUrl: String, credential: String? = null): BoopApi {
        val client = baseClient.newBuilder()
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
