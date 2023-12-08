package com.ji.adshelper.ads

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback

class OpenAdsHelper(private val application: Application) : ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var currentActivity: Activity? = null
    private var validShowAds: ((Activity?) -> Boolean)? = null

    val isAdAvailable: Boolean get() = appOpenAd != null
    var pendingShowAds = true // pending first from splash

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
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        currentActivity = activity
    }

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
        if (currentActivity == activity) {
            currentActivity = null
        }
    }

    /**
     * LifecycleObserver methods
     */

    override fun onStart(owner: LifecycleOwner) {
        // automatically show an app open ad when the application starts or reopens from background
        if (pendingShowAds) {
            pendingShowAds = false
            return
        }

        if (validShowAds != null && validShowAds?.invoke(currentActivity) == false) return
        showAdIfAvailable()
    }

    // endregion
    private fun sendEvent(action: String) {
        application.sendBroadcast(Intent(action))
    }

    fun setValidShowAds(action: (Activity?) -> Boolean) {
        validShowAds = action
    }

    companion object {
        const val ACTION_CLOSE = "openAdsHelper_ActionClose"
        const val ACTION_ERROR = "openAdsHelper_ActionError"
        private var instance: OpenAdsHelper? = null

        @JvmStatic
        fun init(application: Application): OpenAdsHelper {
            return OpenAdsHelper(application).also {
                instance = it
            }
        }

        @JvmStatic
        fun getInstance() = instance
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
}