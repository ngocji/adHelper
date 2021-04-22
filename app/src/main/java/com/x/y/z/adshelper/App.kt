package com.x.y.z.adshelper

import android.app.Application
import com.ji.adshelper.ads.AdsSDK
import com.ji.adshelper.ads.AdsSDK.init
import com.ji.adshelper.ads.OpenAdsHelper

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        AdsSDK.init(
            this,
            getString(R.string.banner_ad_unit_id),
            getString(R.string.goc_ad_unit_id),
            "ca-app-pub-3940256099942544/1033173712",
            getString(R.string.reward_ad_unit_id),
            "ca-app-pub-3940256099942544/5662855259",
            BuildConfig.DEBUG
        )

        OpenAdsHelper.init(this)
    }
}