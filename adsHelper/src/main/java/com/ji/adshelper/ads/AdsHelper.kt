package com.ji.adshelper.ads

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.ji.adshelper.R
import com.ji.adshelper.consent.ConsentInfo
import com.ji.adshelper.view.NativeTemplateStyle
import com.ji.adshelper.view.TemplateView


object AdsHelper {
    // region banner
    @JvmStatic
    fun loadNative(
        containerView: ViewGroup,
        isMedium: Boolean,
        onAdLoadListener: AdLoadListener?
    ) {
        loadNative(containerView, isMedium, NativeTemplateStyle.Builder().build(), onAdLoadListener)
    }

    @JvmStatic
    fun loadNative(
        containerView: ViewGroup,
        isMedium: Boolean,
        styles: NativeTemplateStyle,
        onAdLoadListener: AdLoadListener?
    ) {
        val templateView = LayoutInflater.from(containerView.context)
            .inflate(
                if (isMedium) R.layout.default_medium_template else R.layout.default_small_template,
                null
            ) as? TemplateView ?: return
        containerView.removeAllViews()
        containerView.addView(templateView)

        loadNative(containerView, templateView, styles, onAdLoadListener)
    }

    @JvmStatic
    fun loadNative(
        containerView: View,
        templateView: TemplateView,
        onAdLoadListener: AdLoadListener? = null
    ) {
        loadNative(
            containerView,
            templateView,
            NativeTemplateStyle.Builder().build(),
            onAdLoadListener
        )
    }

    @JvmStatic
    fun loadNative(
        containerView: View,
        templateView: TemplateView,
        styles: NativeTemplateStyle,
        onAdLoadListener: AdLoadListener? = null
    ) {
        if (AdsSDK.needRequireConsent && !ConsentInfo.isAcceptedConsent()) {
            containerView.isVisible = false
            return
        }

        val adLoader = AdLoader.Builder(containerView.context, AdsSDK.nativeId)
            .forNativeAd { nativeAd ->
                containerView.visibility = View.VISIBLE
                templateView.visibility = View.VISIBLE
                templateView.setStyles(styles)
                templateView.setNativeAd(nativeAd)
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    containerView.visibility = View.GONE
                    templateView.visibility = View.GONE
                    onAdLoadListener?.onAddFailed()
                }

                override fun onAdLoaded() {
                    onAdLoadListener?.onAddLoaded()
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
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

        if (viewGroup.childCount > 0) {
            viewGroup.removeAllViews()
        }

        val adView = AdView(context)
        adView.adUnitId = AdsSDK.bannerId
        adView.setAdSize(adSize)
        viewGroup.addView(adView)
        adView
            .adListener = object : com.google.android.gms.ads.AdListener() {
            override fun onAdFailedToLoad(p0: LoadAdError) {
                onAdLoadListener?.onAddFailed()
            }

            override fun onAdLoaded() {
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

    // endregion
    // region interstitial
    private val interstitialAdSet = HashMap<Int, InterstitialAd>()

    @JvmStatic
    fun <T> isInterstitialAdLoaded(target: T): Boolean {
        return interstitialAdSet[target.hashCode()] != null
    }

    @JvmStatic
    fun <T> loadInterstitialAd(target: T, onAdLoadListener: AdLoadListener? = null) {
        if (AdsSDK.needRequireConsent && !ConsentInfo.isAcceptedConsent()) {
            return
        }

        val context = getContext(target) ?: return

        InterstitialAd.load(context,
            AdsSDK.interstitialId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    super.onAdLoaded(interstitialAd)
                    interstitialAdSet[target.hashCode()] = interstitialAd
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
    fun <T> releaseInterstitialAd(target: T) {
        interstitialAdSet.remove(target.hashCode())
    }

    @JvmStatic
    fun <T> showInterstitialAd(target: T, cacheNew: Boolean) {
        showInterstitialAd(target = target, cacheNew = cacheNew, callback = null)
    }

    @JvmStatic
    fun <T> showInterstitialAd(target: T, cacheNew: Boolean, callback: OnAdCloseListener?) {
        val ins = interstitialAdSet[target.hashCode()]
        if (ins == null) {
            callback?.onClose()
            if (cacheNew) loadInterstitialAd(target)
            return
        }

        ins.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                callback?.onClose()
                interstitialAdSet.remove(target.hashCode())
                if (cacheNew) loadInterstitialAd(target)
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                callback?.onClose()
                interstitialAdSet.remove(target.hashCode())
                if (cacheNew) loadInterstitialAd(target)
            }

        }
        ins.show(getActivity(target) ?: return)
    }

    @JvmStatic
    @Deprecated("")
    fun <T> showInterstitialAd(target: T, cacheNew: Boolean, adListener: AdListener?) {
        val ins = interstitialAdSet[target.hashCode()]
        if (ins == null) {
            adListener?.onAdLoadFailed()
            loadInterstitialAd(target)
            return
        }
        ins.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                adListener?.onAdLoadFailed()
                interstitialAdSet.remove(target.hashCode())
                if (cacheNew) loadInterstitialAd(target)
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                adListener?.onAdRewarded()
                interstitialAdSet.remove(target.hashCode())
                if (cacheNew) loadInterstitialAd(target)
            }

        }
        ins.show(getActivity(target) ?: return)
    }

    @JvmStatic
    fun <T> showOneInterstitialAd(key: Context, target: T) {
        showOneInterstitialAd(key = key, target = target, callback = null)
    }

    @JvmStatic
    fun <T> showOneInterstitialAd(key: Context, target: T, callback: OnAdCloseListener?) {
        val ins = interstitialAdSet[key.hashCode()] ?: return run { callback?.onClose() }
        ins.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                callback?.onClose()
                interstitialAdSet.remove(target.hashCode())
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                callback?.onClose()
                interstitialAdSet.remove(target.hashCode())
            }

        }
        ins.show(getActivity(target) ?: return)
    }

    @JvmStatic
    @Deprecated("")
    fun <T> showOneInterstitialAd(key: Context, target: T, adListener: AdListener?) {
        val ins = interstitialAdSet[key.hashCode()] ?: return
        ins.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                super.onAdFailedToShowFullScreenContent(adError)
                adListener?.onAdLoadFailed()
                interstitialAdSet.remove(target.hashCode())
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                adListener?.onAdRewarded()
                interstitialAdSet.remove(target.hashCode())
            }

        }
        ins.show(getActivity(target) ?: return)
    }

    // endregion
    private val rewardAdSet = HashMap<Int, RewardedAd>()

    @JvmStatic
    fun <T> isRewardAdLoaded(target: T): Boolean {
        return rewardAdSet[target.hashCode()] != null
    }

    @JvmStatic
    fun <T> loadRewardAd(target: T, onAdLoadListener: AdLoadListener? = null) {
        if (AdsSDK.needRequireConsent && !ConsentInfo.isAcceptedConsent()) {
            return
        }

        val context = getContext(target) ?: return
        RewardedAd.load(
            context,
            AdsSDK.rewardedId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    super.onAdLoaded(rewardedAd)
                    rewardAdSet[target.hashCode()] = rewardedAd
                    onAdLoadListener?.onAddLoaded()
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    onAdLoadListener?.onAddFailed()
                }
            })
    }

    @JvmStatic
    fun <T> showRewardAd(target: T, cacheNew: Boolean, @NonNull callback: OnAdCloseListener) {
        val rewardedAd = rewardAdSet[target.hashCode()]
        if (rewardedAd == null) {
            callback.onClose()
            if (cacheNew) loadRewardAd(target)
            return
        }
        rewardedAd.show(getActivity(target) ?: return, OnUserEarnedRewardListener {
            callback.onClose()
            rewardAdSet.remove(target.hashCode())
            if (cacheNew) loadRewardAd(target)
        })
    }

    @JvmStatic
    @Deprecated("")
    fun <T> showRewardAd(target: T, cacheNew: Boolean, adListener: AdListener?) {
        val rewardedAd = rewardAdSet[target.hashCode()]
        if (rewardedAd == null) {
            adListener?.onAdLoadFailed()
            loadRewardAd(target)
            return
        }
        rewardedAd.show(getActivity(target) ?: return, OnUserEarnedRewardListener {
            adListener?.onAdRewarded()
            rewardAdSet.remove(target.hashCode())
            if (cacheNew) loadRewardAd(target)
        })
    }

    @JvmStatic
    fun <T> releaseRewardAd(target: T) {
        rewardAdSet.remove(target.hashCode())
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

    private fun <T> getActivity(target: T): Activity? {
        return when (target) {
            is Fragment -> (target as Fragment).activity
            is FragmentActivity -> target
            else -> null
        }
    }

    // endregion
    abstract class AdListener {
        abstract fun onAdLoadFailed()
        abstract fun onAdRewarded()
    }

    abstract class AdLoadListener {
        abstract fun onAddFailed()
        abstract fun onAddLoaded()
    }
}