package com.example.hitster.di

import com.example.hitster.data.AccessTokenProvider
import com.example.hitster.game.usecase.ConnectToSpotifyUseCase
import com.example.hitster.MainViewModel
import com.example.hitster.game.GameViewModel
import com.example.hitster.game.usecase.FetchPlaylistTracksUseCase
import com.example.hitster.game.usecase.FetchTrackDetailsUseCase
import com.example.hitster.game.usecase.FindEarliestReleaseYearUseCase
import com.example.hitster.home.HomeViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    singleOf(::AccessTokenProvider)
    viewModelOf(::MainViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::GameViewModel)

    // Use cases
    factory { ConnectToSpotifyUseCase(context = get()) }
    factoryOf(::FindEarliestReleaseYearUseCase)
    factoryOf(::FetchPlaylistTracksUseCase)
    factoryOf(::FetchTrackDetailsUseCase)
}