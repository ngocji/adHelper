package com.ji.adshelper.ads

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.ji.adshelper.R
import com.ji.adshelper.biling.DefaultBillingProcessor.isPurchased
import com.ji.adshelper.consent.ConsentInfo
import com.ji.adshelper.view.NativeTemplateStyle
import com.ji.adshelper.view.TemplateView
import kotlin.reflect.KClass


object AdsHelper {
    // region banner
    private val connectivityChangeListeners =
        mutableMapOf<String, ConnectivityHelper.OnConnectivityChangeListener>()
    private val attachViewGroupListener = mutableMapOf<String, View.OnAttachStateChangeListener>()

    @JvmStatic
    fun loadNative(
        viewGroup: ViewGroup,
        isMedium: Boolean
    ) {
        loadNative(
            viewGroup = viewGroup,
            isMedium = isMedium,
            styles = null,
            onAdLoadListener = null
        )
    }

    @JvmStatic
    fun loadNative(
        viewGroup: ViewGroup,
        isMedium: Boolean,
        styles: NativeTemplateStyle?
    ) {
        loadNative(
            viewGroup = viewGroup,
            isMedium = isMedium,
            styles = styles,
            onAdLoadListener = null
        )
    }

    @JvmStatic
    fun loadNative(
        viewGroup: ViewGroup,
        isMedium: Boolean,
        styles: NativeTemplateStyle?,
        onAdLoadListener: AdLoadListener?
    ) {
        val templateView = LayoutInflater.from(viewGroup.context)
            .inflate(
                if (isMedium) R.layout.default_medium_template else R.layout.default_small_template,
                null
            ) as? TemplateView ?: return

        loadNative(
            viewGroup = viewGroup,
            templateView = templateView,
            styles = styles,
            onAdLoadListener = onAdLoadListener
        )
    }

    @JvmStatic
    fun loadNative(
        viewGroup: ViewGroup,
        templateView: TemplateView,
        styles: NativeTemplateStyle?,
        onAdLoadListener: AdLoadListener? = null
    ) {
        if (AdsSDK.needRequireConsent && !ConsentInfo.isAcceptedConsent()) {
            viewGroup.isVisible = false
            return
        }

        viewGroup.cleanupNetworkListeners()

        when {
            isPurchased() -> viewGroup.isVisible = false
            !ConnectivityHelper.isNetworkAvailable(viewGroup.context.applicationContext) -> {
                if (AdsSDK.useAdCommonUI) {
                    viewGroup.isVisible = true
                    viewGroup.removeAllViews()
                    viewGroup.addView(getAdErrorUI(viewGroup.context))
                } else {
                    viewGroup.isVisible = false
                }

                viewGroup.onNetworkConnected {
                    loadNative(
                        viewGroup = viewGroup,
                        templateView = templateView,
                        styles = styles,
                        onAdLoadListener = onAdLoadListener
                    )
                }
            }

            else -> {
                viewGroup.isVisible = true
                viewGroup.removeAllViews()
                if (AdsSDK.useAdCommonUI) {
                    viewGroup.addView(getAdLoadingUI(viewGroup.context))
                }

                val adLoader = AdLoader.Builder(viewGroup.context, AdsSDK.nativeId)
                    .forNativeAd { nativeAd ->
                        templateView.visibility = View.VISIBLE
                        templateView.setStyles(styles ?: NativeTemplateStyle.Builder().build())
                        templateView.setNativeAd(nativeAd)
                    }
                    .withAdListener(object : com.google.android.gms.ads.AdListener() {
                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            if (AdsSDK.useAdCommonUI) {
                                viewGroup.removeAllViews()
                                viewGroup.addView(getAdErrorUI(viewGroup.context))
                            } else {
                                viewGroup.isVisible = false
                            }
                            onAdLoadListener?.onAddFailed()
                        }

                        override fun onAdLoaded() {
                            viewGroup.removeAllViews()
                            viewGroup.addView(templateView)
                            onAdLoadListener?.onAddLoaded()
                        }
                    })
                    .build()
                adLoader.loadAd(AdRequest.Builder().build())
            }
        }
    }

    @JvmStatic
    fun loadBanner(
        context: Context,
        viewGroup: ViewGroup,
        adSize: AdSize,
        onAdLoadListener: AdLoadListener? = null
    ) {
        loadBanner(context, viewGroup, adSize, null, onAdLoadListener)
    }

    @JvmStatic
    fun loadBanner(viewGroup: ViewGroup, adSize: AdSize, onAdLoadListener: AdLoadListener? = null) {
        loadBanner(viewGroup.context, viewGroup, adSize, onAdLoadListener)
    }

    @JvmStatic
    fun loadBanner(
        viewGroup: ViewGroup,
        adSize: AdSize,
        collapsibleType: CollapsibleType?,
        onAdLoadListener: AdLoadListener? = null
    ) {
        loadBanner(viewGroup.context, viewGroup, adSize, collapsibleType, onAdLoadListener)
    }

    @JvmStatic
    fun loadBanner(
        context: Context,
        viewGroup: ViewGroup,
        adSize: AdSize,
        collapsibleType: CollapsibleType?,
        onAdLoadListener: AdLoadListener? = null
    ) {
        if (AdsSDK.needRequireConsent && !ConsentInfo.isAcceptedConsent()) {
            viewGroup.isVisible = false
            return
        }

        viewGroup.cleanupNetworkListeners()
        when {
            isPurchased() -> viewGroup.isVisible = false
            !ConnectivityHelper.isNetworkAvailable(viewGroup.context.applicationContext) -> {
                if (AdsSDK.useAdCommonUI) {
                    viewGroup.isVisible = true
                    viewGroup.removeAllViews()
                    viewGroup.addView(getAdErrorUI(viewGroup.context))
                } else {
                    viewGroup.isVisible = false
                }

                viewGroup.onNetworkConnected {
                    loadBanner(
                        context = context,
                        viewGroup = viewGroup,
                        adSize = adSize,
                        onAdLoadListener = onAdLoadListener
                    )
                }
            }

            else -> {
                viewGroup.isVisible = true
                viewGroup.removeAllViews()
                if (AdsSDK.useAdCommonUI) {
                    viewGroup.addView(getAdLoadingUI(viewGroup.context))
                }
                val adView = AdView(context)
                adView.adUnitId = AdsSDK.bannerId
                adView.setAdSize(adSize)
                adView
                    .adListener = object : com.google.android.gms.ads.AdListener() {
                    override fun onAdFailedToLoad(p0: LoadAdError) {
                        if (AdsSDK.useAdCommonUI) {
                            viewGroup.removeAllViews()
                            viewGroup.addView(getAdErrorUI(viewGroup.context))
                        } else {
                            viewGroup.isVisible = false
                        }

                        onAdLoadListener?.onAddFailed()
                    }

                    override fun onAdLoaded() {
                        viewGroup.removeAllViews()
                        viewGroup.addView(adView)
                        onAdLoadListener?.onAddLoaded()
                    }
                }

                val request = AdRequest.Builder()
                    .apply {
                        if (collapsibleType != null) {
                            addNetworkExtrasBundle(
                                AdMobAdapter::class.java, bundleOf(
                                    "collapsible" to collapsibleType.name.lowercase()
                                )
                            )
                        }
                    }
                    .build()

                adView.loadAd(request)
            }
        }
    }

    // endregion
    // region interstitial
    private val interstitialAdSet = HashMap<String, InterstitialAd>()

    @JvmStatic
    fun <T> isInterstitialAdLoaded(target: T): Boolean {
        return interstitialAdSet[target.getKey()] != null
    }

    @JvmStatic
    fun <T> releaseInterstitialAd(target: T) {
        interstitialAdSet.remove(target.getKey())
    }

    @JvmStatic
    fun loadInterstitialAd(context: Context?, key: String, onAdLoadListener: AdLoadListener?) {
        context ?: return run {
            onAdLoadListener?.onAddFailed()
        }

        if (AdsSDK.needRequireConsent && !ConsentInfo.isAcceptedConsent()) {
            return
        }

        InterstitialAd.load(context,
            AdsSDK.interstitialId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    super.onAdLoaded(interstitialAd)
                    interstitialAdSet[key] = interstitialAd
                    onAdLoadListener?.onAddLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    onAdLoadListener?.onAddFailed()
                }
            }
        )
    }

    @JvmStatic
    fun <T> loadInterstitialAd(target: T, onAdLoadListener: AdLoadListener?) {
        val context = getContext(target) ?: return
        loadInterstitialAd(context, target.getKey(), onAdLoadListener)
    }

    @JvmStatic
    fun <T> loadInterstitialAd(target: T) {
        loadInterstitialAd(target, null)
    }

    @JvmStatic
    fun showInterstitialAd(
        activity: FragmentActivity?,
        key: String,
        cacheNew: Boolean,
        callback: OnAdCloseListener?
    ) {
        activity ?: return run {
            callback?.onClose()
        }

        val ins = interstitialAdSet[key]
        if (ins == null) {
            callback?.onClose()
            if (cacheNew) loadInterstitialAd(activity, key, null)
            return
        }

        ins.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                callback?.onClose()
                interstitialAdSet.remove(key)
                if (cacheNew) loadInterstitialAd(activity, key, null)
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                callback?.onClose()
                interstitialAdSet.remove(key)
                if (cacheNew) loadInterstitialAd(activity, key, null)
            }

        }
        ins.show(activity)
    }

    @JvmStatic
    fun <T> showInterstitialAd(target: T) {
        showInterstitialAd(
            activity = getActivity(target),
            key = target.getKey(),
            cacheNew = false,
            callback = null
        )
    }

    @JvmStatic
    fun <T> showInterstitialAd(target: T, cacheNew: Boolean) {
        showInterstitialAd(
            activity = getActivity(target),
            key = target.getKey(),
            cacheNew = cacheNew,
            callback = null
        )
    }

    @JvmStatic
    fun <T> showInterstitialAd(target: T, cacheNew: Boolean, callback: OnAdCloseListener?) {
        showInterstitialAd(
            activity = getActivity(target),
            key = target.getKey(),
            cacheNew = cacheNew,
            callback = callback
        )
    }

    // endregion
    private val rewardAdSet = HashMap<String, RewardedAd>()

    @JvmStatic
    fun <T> isRewardAdLoaded(target: T): Boolean {
        return rewardAdSet[target.getKey()] != null
    }

    @JvmStatic
    fun <T> releaseRewardAd(target: T) {
        rewardAdSet.remove(target.getKey())
    }

    @JvmStatic
    fun loadRewardAd(context: Context?, key: String, onAdLoadListener: AdLoadListener? = null) {
        context ?: return run {
            onAdLoadListener?.onAddFailed()
        }

        if (AdsSDK.needRequireConsent && !ConsentInfo.isAcceptedConsent()) {
            return
        }

        RewardedAd.load(
            context,
            AdsSDK.rewardedId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    super.onAdLoaded(rewardedAd)
                    rewardAdSet[key] = rewardedAd
                    onAdLoadListener?.onAddLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    onAdLoadListener?.onAddFailed()
                }
            })
    }

    @JvmStatic
    fun <T> loadRewardAd(target: T, onAdLoadListener: AdLoadListener?) {
        loadRewardAd(
            context = getContext(target),
            key = target.getKey(),
            onAdLoadListener = onAdLoadListener
        )
    }

    @JvmStatic
    fun <T> loadRewardAd(target: T) {
        loadRewardAd(context = getContext(target), key = target.getKey(), onAdLoadListener = null)
    }

    @JvmStatic
    fun showRewardAd(
        activity: FragmentActivity?,
        key: String,
        cacheNew: Boolean,
        callback: OnAdCloseListener?
    ) {
        val rewardedAd = rewardAdSet[key]
        if (rewardedAd == null || activity == null) {
            callback?.onClose()
            if (cacheNew) loadRewardAd(context = activity, key = key, onAdLoadListener = null)
            return
        }
        rewardedAd.show(activity, OnUserEarnedRewardListener {
            callback?.onClose()
            rewardAdSet.remove(key)
            if (cacheNew) loadRewardAd(context = activity, key = key)
        })
    }

    @JvmStatic
    fun <T> showRewardAd(target: T, cacheNew: Boolean, callback: OnAdCloseListener?) {
        showInterstitialAd(
            activity = getActivity(target),
            key = target.getKey(),
            cacheNew = cacheNew,
            callback = callback
        )
    }

    @JvmStatic
    fun <T> showRewardAd(target: T, callback: OnAdCloseListener?) {
        showInterstitialAd(
            activity = getActivity(target),
            key = target.getKey(),
            cacheNew = false,
            callback = callback
        )
    }

    @JvmStatic
    fun <T> showRewardAd(target: T) {
        showInterstitialAd(
            activity = getActivity(target),
            key = target.getKey(),
            cacheNew = false,
            callback = null
        )
    }

    // region video reward
    // endregion
    // region private method helper
    private fun <T> getContext(target: T): Context? {
        return when (target) {
            is Fragment -> (target as Fragment).context
            is FragmentActivity -> target
            is Context -> target
            else -> null

        }
    }

    private fun <T> T?.getKey(): String {
        return when (this) {
            is Class<*> -> name
            is KClass<*> -> this.java.name
            else -> this?.toString() ?: ""
        }
    }

    private fun <T> getActivity(target: T): FragmentActivity? {
        return when (target) {
            is Fragment -> (target as Fragment).activity
            is FragmentActivity -> target
            else -> null
        }
    }

    private fun getAdLoadingUI(context: Context): View {
        return LayoutInflater.from(context).inflate(
            if (AdsSDK.customAdLoadingId > 0) AdsSDK.customAdLoadingId else R.layout.ad_loading_ui,
            null
        )
    }

    private fun getAdErrorUI(context: Context): View {
        return LayoutInflater.from(context).inflate(
            if (AdsSDK.customAdErrorId > 0) AdsSDK.customAdErrorId else R.layout.ad_error_ui,
            null
        )
    }

    private fun ViewGroup.cleanupNetworkListeners() {
        val key = getKey()
        ConnectivityHelper.removeListener(connectivityChangeListeners.remove(key))
        removeOnAttachStateChangeListener(attachViewGroupListener.remove(key))
    }

    private fun ViewGroup.onNetworkConnected(action: () -> Unit) {
        val key = getKey()
        cleanupNetworkListeners()
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            private val connectivityChangeListener =
                ConnectivityHelper.OnConnectivityChangeListener { isConnected ->
                    if (isConnected) {
                        action()
                        cleanupNetworkListeners()
                    }
                }

            init {
                attachViewGroupListener[key] = this
                connectivityChangeListeners[key] = connectivityChangeListener
            }

            override fun onViewAttachedToWindow(v: View) {
                ConnectivityHelper.addListener(connectivityChangeListener)
            }

            override fun onViewDetachedFromWindow(v: View) {
                cleanupNetworkListeners()
            }
        })
    }

    // endregion

    abstract class AdLoadListener {
        abstract fun onAddFailed()
        abstract fun onAddLoaded()
    }
}