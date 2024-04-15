package com.x.y.z.adshelper

import android.app.Application
import com.ji.adshelper.ads.AdsSDK
import com.ji.adshelper.ads.OpenAdsHelper

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        AdsSDK
            .setRequireConsent(false)
            .init(
                this,
                "",
                "",
                "",
                "",
                "",
                BuildConfig.DEBUG
            ) {
            }
    }
}