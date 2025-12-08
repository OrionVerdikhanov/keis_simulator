package com.giftfest.game

import android.app.Application
import com.giftfest.game.data.GameDatabase

class GiftFestApplication : Application() {

    val database: GameDatabase by lazy {
        GameDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: GiftFestApplication
            private set
    }
}
