package com.example.novacircle.di

import com.example.novacircle.data.repository.SupabaseAuthRepository
import com.example.novacircle.data.repository.SupabaseCircleRepository
import com.example.novacircle.data.repository.SupabaseInviteRepository
import com.example.novacircle.data.repository.SupabaseLocationRepository
import com.example.novacircle.data.repository.SupabaseProfileRepository
import com.example.novacircle.domain.repository.AuthRepository
import com.example.novacircle.domain.repository.CircleRepository
import com.example.novacircle.domain.repository.InviteRepository
import com.example.novacircle.domain.repository.LocationRepository
import com.example.novacircle.domain.repository.ProfileRepository
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
    abstract fun bindAuthRepository(
        impl: SupabaseAuthRepository
    ): AuthRepository

    @Binds @Singleton
    abstract fun bindCircleRepository(impl: SupabaseCircleRepository): CircleRepository

    @Binds @Singleton
    abstract fun bindProfileRepository(impl: SupabaseProfileRepository): ProfileRepository

    @Binds @Singleton
    abstract fun bindInviteRepository(impl: SupabaseInviteRepository): InviteRepository

    @Binds @Singleton
    abstract fun bindLocationRepository(impl: SupabaseLocationRepository): LocationRepository
}