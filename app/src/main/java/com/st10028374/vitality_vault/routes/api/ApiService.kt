package com.st10028374.vitality_vault.routes.api

import com.google.gson.GsonBuilder
import com.st10028374.vitality_vault.routes.models.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * Retrofit API Interface
 * Define all API endpoints here
 */
interface FitnessApiService {

    @POST("routes")
    suspend fun saveRoute(@Body request: RouteRequest): Response<RouteResponse>

    @GET("routes/user/{userId}")
    suspend fun getUserRoutes(
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 50,
        @Query("skip") skip: Int = 0
    ): Response<RoutesListResponse>

    @GET("routes/{routeId}")
    suspend fun getRouteById(@Path("routeId") routeId: String): Response<SingleRouteResponse>

    @PUT("routes/{routeId}")
    suspend fun updateRoute(
        @Path("routeId") routeId: String,
        @Body request: RouteRequest
    ): Response<RouteResponse>

    @DELETE("routes/{routeId}")
    suspend fun deleteRoute(@Path("routeId") routeId: String): Response<RouteResponse>

    @GET("routes/search/{userId}")
    suspend fun searchRoutes(
        @Path("userId") userId: String,
        @Query("query") searchQuery: String
    ): Response<RoutesListResponse>

    @GET("stats/{userId}")
    suspend fun getUserStats(@Path("userId") userId: String): Response<StatsResponse>
}

/**
 * Retrofit Client Singleton
 * Handles API configuration and instance creation
 */
object RetrofitClient {

    // TODO: Replace with your actual backend URL
    // For testing, you can use: https://your-backend.herokuapp.com/api/v1/
    // Or use ngrok for local testing: https://xxxxx.ngrok.io/api/v1/
    private const val BASE_URL = "https://your-api-backend.com/api/v1/"

    // Logging interceptor for debugging
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // OkHttp Client configuration
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
            // Add authentication token if needed
            // .header("Authorization", "Bearer ${getAuthToken()}")

            val request = requestBuilder.build()
            chain.proceed(request)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    // Gson configuration
    private val gson = GsonBuilder()
        .setLenient()
        .create()

    // Retrofit instance
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    // API Service instance
    val apiService: FitnessApiService = retrofit.create(FitnessApiService::class.java)
}

/**
 * API Response wrapper for handling different states
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

/**
 * Extension function to handle API responses safely
 */
suspend fun <T> safeApiCall(apiCall: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful && response.body() != null) {
            ApiResult.Success(response.body()!!)
        } else {
            ApiResult.Error(
                message = response.message() ?: "Unknown error occurred",
                code = response.code()
            )
        }
    } catch (e: Exception) {
        ApiResult.Error(
            message = e.localizedMessage ?: "Network error occurred"
        )
    }
}