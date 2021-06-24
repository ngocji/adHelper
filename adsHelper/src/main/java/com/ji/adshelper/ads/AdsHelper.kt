package com.ji.adshelper.ads

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.ji.adshelper.view.NativeTemplateStyle
import com.ji.adshelper.view.TemplateView
import java.util.*

object AdsHelper {
    // region banner
    @JvmStatic
    fun loadNative(containerView: View, templateView: TemplateView) {
        val adLoader = AdLoader.Builder(containerView.context, AdsSDK.nativeId)
            .forNativeAd { nativeAd ->
                val styles = NativeTemplateStyle.Builder().build()
                containerView.visibility = View.VISIBLE
                templateView.visibility = View.VISIBLE
                templateView.setStyles(styles)
                templateView.setNativeAd(nativeAd)
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    containerView.visibility = View.GONE
                    templateView.visibility = View.GONE
                }
            })
            .build()
        adLoader.loadAd(AdRequest.Builder().build())
    }

    @JvmStatic
    fun loadBanner(viewGroup: ViewGroup, adSize: AdSize) {
        val adView = AdView(viewGroup.context)
        adView.adUnitId = AdsSDK.bannerId
        adView.adSize = adSize
        viewGroup.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    // endregion
    // region interstitial
    private val interstitialAdSet = HashMap<Int, InterstitialAd>()

    @JvmStatic
    fun <T> loadInterstitialAd(target: T) {
        val context = getContext(target) ?: return

        InterstitialAd.load(context,
            AdsSDK.interstitialId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    super.onAdLoaded(interstitialAd)
                    interstitialAdSet[target.hashCode()] = interstitialAd
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                }
            }
        )
    }

    @JvmStatic
    fun <T> releaseInterstitialAd(target: T) {
        interstitialAdSet.remove(target.hashCode())
    }

    @JvmStatic
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

            override fun onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent()
            }

            override fun onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent()
                adListener?.onAdRewarded()
                interstitialAdSet.remove(target.hashCode())
                if (cacheNew) loadInterstitialAd(target)
            }

            override fun onAdImpression() {
                super.onAdImpression()
            }
        }
        ins.show(getActivity(target) ?: return)
    }

    // endregion
    private val rewardAdSet = HashMap<Int, RewardedAd>()

    @JvmStatic
    fun <T> loadRewardAd(target: T) {
        val context = getContext(target) ?: return
        RewardedAd.load(
            context,
            AdsSDK.rewardedId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    super.onAdLoaded(rewardedAd)
                    rewardAdSet[target.hashCode()] = rewardedAd
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                }
            })
    }

    @JvmStatic
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
}