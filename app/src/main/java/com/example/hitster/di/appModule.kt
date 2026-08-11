package com.example.hitster.di

import com.example.hitster.data.AccessTokenProvider
import com.example.hitster.data.SpotifyTokenStorage
import com.example.hitster.game.usecase.ConnectToSpotifyUseCase
import com.example.hitster.MainViewModel
import com.example.hitster.game.GameViewModel
import com.example.hitster.game.usecase.FetchPlaylistTracksUseCase
import com.example.hitster.game.usecase.FetchTrackDetailsUseCase
import com.example.hitster.game.usecase.FindEarliestReleaseYearUseCase
import com.example.hitster.home.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { SpotifyTokenStorage(context = androidContext()) }
    // Not singleOf: the dispatcher and clock parameters are meant to keep their defaults.
    single { AccessTokenProvider(storage = get()) }

    viewModelOf(::MainViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::GameViewModel)

    // Use cases
    factory { ConnectToSpotifyUseCase(context = get()) }
    factoryOf(::FindEarliestReleaseYearUseCase)
    factoryOf(::FetchPlaylistTracksUseCase)
    factoryOf(::FetchTrackDetailsUseCase)
}