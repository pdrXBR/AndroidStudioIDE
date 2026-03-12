package com.androidide.di

import android.content.Context
import androidx.room.Room
import com.androidide.data.local.AppDatabase
import com.androidide.data.local.dao.ChatHistoryDao
import com.androidide.data.local.dao.RecentFileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })
            .build()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "androidide.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideRecentFileDao(db: AppDatabase): RecentFileDao = db.recentFileDao()

    @Provides
    fun provideChatHistoryDao(db: AppDatabase): ChatHistoryDao = db.chatHistoryDao()
}
