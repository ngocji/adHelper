package com.ji.adshelper.ads

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback

class OpenAdsHelper(private val application: Application) : ActivityLifecycleCallbacks,
    LifecycleObserver {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var currentActivity: Activity? = null

    val isAdAvailable: Boolean get() = appOpenAd != null
    var pendingShowAds = false

    fun fetchAd(onAdListener: OnAdListener? = null) {
        // Fetch a new ad if we are not fetching them and there is no loaded ad available.
        if (isAdAvailable) {
            onAdListener?.onAdLoaded()
            return
        }

        if (isLoadingAd) {
            onAdListener?.onAdLoading()
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            application,
            AdsSDK.openAdId,
            request,
            AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    this@OpenAdsHelper.appOpenAd = appOpenAd
                    isLoadingAd = false
                    onAdListener?.onAdLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAd = false
                    onAdListener?.onAdLoadFailed(loadAdError.code)
                }
            })
    }

    fun showAdIfAvailable(adListener: OnAdListener? = null) {
        // Show ad if an ad is available and no open ad is showing currently
        if (!isShowingAd && isAdAvailable) {
            val fullScreenContentCallback: FullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null
                        isShowingAd = false
                        fetchAd()
                        sendEvent(ACTION_CLOSE)
                        adListener?.onAdClosed()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        sendEvent(ACTION_ERROR)
                        adListener?.onAdLoadFailed(adError.code)
                    }

                    override fun onAdShowedFullScreenContent() {
                        isShowingAd = true
                    }
                }
            appOpenAd?.fullScreenContentCallback = fullScreenContentCallback
            appOpenAd?.show(currentActivity ?: return)
        } else {
            //fetch a new ad if needed
            fetchAd(adListener)
            sendEvent(ACTION_ERROR)
        }
    }

    // region override lifecycle
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {
        currentActivity = null
    }

    /**
     * LifecycleObserver methods
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        // automatically show an app open ad when the application starts or reopens from background
        if (pendingShowAds) {
            pendingShowAds = false
            return
        }
        showAdIfAvailable()
    }

    // endregion
    private fun sendEvent(action: String) {
        LocalBroadcastManager.getInstance(application).sendBroadcast(Intent(action))
    }

    companion object {
        const val ACTION_CLOSE = "openAdsHelper_ActionClose"
        const val ACTION_ERROR = "openAdsHelper_ActionError"

        @JvmStatic
        fun init(application: Application): OpenAdsHelper {
            return OpenAdsHelper(application)
        }
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
}