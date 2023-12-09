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
    private var installSdk: Runnable? = null

    // calling in application
    @JvmStatic
    fun init(context: Context, installSdk: Runnable) {
        consentInformation = UserMessagingPlatform.getConsentInformation(context)
        if (consentInformation.canRequestAds()) {
            installSdk.run()
        } else {
            this.installSdk = installSdk
        }
    }


    @JvmStatic
    fun isAcceptedConsent() =
        this::consentInformation.isInitialized && consentInformation.canRequestAds()

    @JvmStatic
    fun requestIfNeed(
        activity: Activity,
        testDeviceHashedId: String? = null,
        testFromEEARegion: Boolean = true,
        showIfRequire: Boolean = true,
        onFailed: () -> Unit = {},
        onLoadAd: () -> Unit,
    ) {
        if (isAcceptedConsent()) {
            initMobileSdkAccepted(onLoadAd = onLoadAd)
        } else {
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
                        Log.e("Ads", "2")
                        initMobileSdkAccepted(onLoadAd)
                    }
                },
                { error ->
                    Log.e("Ads", "Error request consentInfo: ${error.message}")
                    onFailed()
                }
            )
        }
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
            startInstallSDK()
            onLoadAd?.invoke()
        }
    }

    private fun startInstallSDK() {
        installSdk?.run()
        installSdk = null
    }
}