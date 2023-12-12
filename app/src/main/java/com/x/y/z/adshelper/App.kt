package com.x.y.z.adshelper

import android.app.Application
import com.ji.adshelper.ads.AdsSDK

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AdsSDK.init(
            this,
            "",
            "",
            "",
            "",
            "",
            BuildConfig.DEBUG
        )
    }
}