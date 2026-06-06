package com.txwstudio.app.whatthefit.di

import android.content.Context
import androidx.room.Room
import com.txwstudio.app.whatthefit.data.dao.CategoryDao
import com.txwstudio.app.whatthefit.data.dao.ClothingItemDao
import com.txwstudio.app.whatthefit.data.dao.CrossRefDao
import com.txwstudio.app.whatthefit.data.dao.OotdDao
import com.txwstudio.app.whatthefit.data.dao.TagCrossRefDao
import com.txwstudio.app.whatthefit.data.dao.TagDao
import com.txwstudio.app.whatthefit.data.db.WtfDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WtfDatabase =
        Room.databaseBuilder(context, WtfDatabase::class.java, WtfDatabase.NAME)
            .addCallback(WtfDatabase.seedCallback(context))
            .build()

    @Provides
    fun provideCategoryDao(db: WtfDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideClothingItemDao(db: WtfDatabase): ClothingItemDao = db.clothingItemDao()

    @Provides
    fun provideCrossRefDao(db: WtfDatabase): CrossRefDao = db.crossRefDao()

    @Provides
    fun provideTagDao(db: WtfDatabase): TagDao = db.tagDao()

    @Provides
    fun provideTagCrossRefDao(db: WtfDatabase): TagCrossRefDao = db.tagCrossRefDao()

    @Provides
    fun provideOotdDao(db: WtfDatabase): OotdDao = db.ootdDao()
}
