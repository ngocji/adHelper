package com.x.y.z.adshelper

import android.app.Application
import com.ji.adshelper.AdsSDK

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        AdsSDK.init(
            this,
            getString(R.string.banner_ad_unit_id),
            getString(R.string.goc_ad_unit_id),
            "ca-app-pub-3940256099942544/1033173712",
            getString(R.string.reward_ad_unit_id),
            BuildConfig.DEBUG
        )
    }
}