package com.example.hitster

import android.app.Application
import com.example.hitster.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class HitsterApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin{
            androidLogger()
            androidContext(this@HitsterApplication)
            modules(appModule)
        }
    }
}