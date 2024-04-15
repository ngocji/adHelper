package com.ji.adshelper.ads

import android.Manifest
import android.app.Application
import androidx.annotation.RequiresPermission
import com.google.android.gms.ads.MobileAds
import com.ji.adshelper.consent.ConsentInfo

object AdsSDK {
    var bannerId: String = ""
    var nativeId: String = ""
    var interstitialId: String = ""
    var rewardedId: String = ""
    var openAdId: String = ""
    var needRequireConsent = true
    var targetOpenAdVersion: Int = 0
    private var isInstalled = false

    @RequiresPermission(Manifest.permission.INTERNET)
    @JvmStatic
    fun init(
        application: Application,
        bannerId: String,
        nativeId: String,
        interstitialId: String,
        rewardedId: String,
        openAdId: String,
        isDebug: Boolean,
        onSuccess: () -> Unit
    ) {
        if (isDebug) {
            AdsSDK.bannerId = "ca-app-pub-3940256099942544/6300978111"
            AdsSDK.nativeId = "ca-app-pub-3940256099942544/2247696110"
            AdsSDK.interstitialId = "ca-app-pub-3940256099942544/1033173712"
            AdsSDK.rewardedId = "ca-app-pub-3940256099942544/5224354917"
            AdsSDK.openAdId = "ca-app-pub-3940256099942544/9257395921"
        } else {
            AdsSDK.bannerId = bannerId
            AdsSDK.nativeId = nativeId
            AdsSDK.interstitialId = interstitialId
            AdsSDK.rewardedId = rewardedId
            AdsSDK.openAdId = openAdId
        }


        if (AdsSDK.openAdId.isNotBlank()) {
            OpenAdsHelper.init(application, version = targetOpenAdVersion)
        }

        ConsentInfo.init(application) {
            if (!isInstalled) {
                isInstalled = true
                MobileAds.initialize(application)
                onSuccess()
            }
        }
    }

    @JvmStatic
    fun setTargetOpenAdVersion(version: Int): AdsSDK {
        OpenAdsHelper.setTargetVersion(version)
        return this
    }

    @JvmStatic
    fun setRequireConsent(use: Boolean): AdsSDK {
        needRequireConsent = use
        return this
    }
}