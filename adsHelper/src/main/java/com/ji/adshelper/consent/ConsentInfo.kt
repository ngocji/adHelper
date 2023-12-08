package com.ji.adshelper.consent

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object ConsentInfo {
    private lateinit var consentInformation: ConsentInformation
    private var installSdk: (() -> Unit)? = null

    // calling in application
    fun init(context: Context, installSdk: () -> Unit) {
        consentInformation = UserMessagingPlatform.getConsentInformation(context)
        if (consentInformation.canRequestAds()) {
            installSdk()
        } else {
            this.installSdk = installSdk
        }
    }

    fun isAcceptedConsent() =
        this::consentInformation.isInitialized && consentInformation.canRequestAds()

    fun requestIfNeed(
        activity: Activity,
        testDeviceHashedId: String? = null,
        testFromEEARegion: Boolean = true,
        showIfRequire: Boolean = true,
        onLoadAd: () -> Unit,
        onFailed: () -> Unit = {}
    ) {
        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(
               if (testFromEEARegion) {
                   ConsentDebugSettings
                       .DebugGeography
                       .DEBUG_GEOGRAPHY_EEA
               } else {
                   ConsentDebugSettings
                       .DebugGeography
                       .DEBUG_GEOGRAPHY_NOT_EEA
               }
            )
            .addTestDeviceHashedId(testDeviceHashedId.orEmpty())
            .build()

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .apply {
                if (!testDeviceHashedId.isNullOrBlank()) {
                    setConsentDebugSettings(debugSettings)
                }
            }
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                if (consentInformation.isConsentFormAvailable) {
                    if (showIfRequire) {
                        showConsentFormIfRequire(activity) {
                            initMobileSdkAccepted(onLoadAd)
                        }
                    } else {
                        onFailed()
                    }
                } else {
                    initMobileSdkAccepted(onLoadAd)
                }
            },
            { error ->
                Log.e("Ads", "Error request consentInfo: ${error.message}")
                onFailed()
            }
        )

        initMobileSdkAccepted(onLoadAd = onLoadAd)
    }

    private fun showConsentFormIfRequire(activity: Activity, onDismissListener: () -> Unit) {
        UserMessagingPlatform.loadAndShowConsentFormIfRequired(
            activity
        ) {
            onDismissListener()
        }
    }

    private fun initMobileSdkAccepted(onLoadAd: (() -> Unit)? = null) {
        if (consentInformation.canRequestAds()) {
            installSdk?.invoke()
            onLoadAd?.invoke()
        }
    }
}