package com.feuerwehr.checklist.di

import com.feuerwehr.checklist.BuildConfig
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt module for network dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
            .create()
    }
    
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: com.feuerwehr.checklist.data.remote.interceptor.AuthInterceptor
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
        
        if (BuildConfig.DEBUG_MODE) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }
        
        return builder.build()
    }
    
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideChecklistApiService(retrofit: Retrofit): com.feuerwehr.checklist.data.remote.api.ChecklistApiService {
        return retrofit.create(com.feuerwehr.checklist.data.remote.api.ChecklistApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAuthApiService(retrofit: Retrofit): com.feuerwehr.checklist.data.remote.api.AuthApiService {
        return retrofit.create(com.feuerwehr.checklist.data.remote.api.AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideVehicleApiService(retrofit: Retrofit): com.feuerwehr.checklist.data.remote.api.VehicleApiService {
        return retrofit.create(com.feuerwehr.checklist.data.remote.api.VehicleApiService::class.java)
    }
}