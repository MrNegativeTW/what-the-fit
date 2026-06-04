package com.txwstudio.app.whatthefit.di

import com.txwstudio.app.whatthefit.data.repository.WardrobeRepository
import com.txwstudio.app.whatthefit.data.repository.WardrobeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindWardrobeRepository(impl: WardrobeRepositoryImpl): WardrobeRepository
}
