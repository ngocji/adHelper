package com.ji.adshelper.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.ji.adshelper.consent.ConsentInfo

@Suppress("MemberVisibilityCanBePrivate")
class OpenAdsHelper(
    private val application: Application,
    private var targetVersion: Int = CURRENT_VERSION
) : ActivityLifecycleCallbacks,
    LifecycleEventObserver {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var currentActivity: Activity? = null
    private var validShowAds: ((Activity?) -> Boolean)? = null

    val isAdAvailable: Boolean get() = appOpenAd != null
    var pendingShowAds = true // pending first from splash
    private var onAdListener: OnAdListener? = null

    fun setAdListener(callback: OnAdListener): OpenAdsHelper {
        this.onAdListener = callback
        return this
    }

    fun removeAdListener() {
        this.onAdListener = null
    }

    fun fetch() {
        // Fetch a new ad if we are not fetching them and there is no loaded ad available.
        if (AdsSDK.needRequireConsent && !ConsentInfo.isAcceptedConsent()) {
            onAdListener?.onAdLoadFailed(-1)
            return
        }

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

    @Deprecated("use @fetch")
    fun fetchAd(onAdListener: OnAdListener? = null) {
        // Fetch a new ad if we are not fetching them and there is no loaded ad available.
        if (AdsSDK.needRequireConsent && !ConsentInfo.isAcceptedConsent()) {
            onAdListener?.onAdLoadFailed(-1)
            return
        }

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

    fun show(callback: OnAdCloseListener? = null) {
        // Show ad if an ad is available and no open ad is showing currently
        if (!isShowingAd && isAdAvailable) {
            val fullScreenContentCallback: FullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null
                        isShowingAd = false
                        callback?.onClose()
                        fetch()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        callback?.onClose()
                        fetch()
                    }
                }
            appOpenAd?.fullScreenContentCallback = fullScreenContentCallback
            appOpenAd?.show(currentActivity ?: return)
            isShowingAd = true
        } else {
            callback?.onClose()
            fetch()
        }
    }

    @Deprecated("use @show")
    fun showAdIfAvailable(adListener: OnAdListener? = null) {
        // Show ad if an ad is available and no open ad is showing currently
        if (!isShowingAd && isAdAvailable) {
            val fullScreenContentCallback: FullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        appOpenAd = null
                        isShowingAd = false
                        fetchAd()
                        adListener?.onAdClosed()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
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

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (event == Lifecycle.Event.ON_START) {
            // automatically show an app open ad when the application starts or reopens from background
            if (pendingShowAds) {
                pendingShowAds = false
                return
            }

            if (validShowAds != null && validShowAds?.invoke(currentActivity) == false) return
            if (targetVersion > 0) {
                show()
            } else {
                showAdIfAvailable()
            }
        }
    }

    // endregion
    fun setValidShowAds(action: (Activity?) -> Boolean) {
        validShowAds = action
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: OpenAdsHelper? = null

        const val CURRENT_VERSION = 1

        @JvmStatic
        fun init(application: Application): OpenAdsHelper {
            return instance ?: OpenAdsHelper(application).also {
                instance = it
            }
        }

        @JvmStatic
        fun init(application: Application, version: Int): OpenAdsHelper {
            return instance ?: OpenAdsHelper(application, targetVersion = version).also {
                instance = it
            }
        }

        @JvmStatic
        fun setTargetVersion(version: Int) {
            getInstance()?.targetVersion = version
        }

        @JvmStatic
        fun getInstance() = instance
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
}