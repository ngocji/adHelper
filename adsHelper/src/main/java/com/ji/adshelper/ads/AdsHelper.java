package com.ji.adshelper.ads;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdCallback;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.ji.adshelper.view.NativeTemplateStyle;
import com.ji.adshelper.view.TemplateView;

import java.util.HashMap;

public class AdsHelper {

    // region banner
    public static void loadNative(View containerView, TemplateView templateView) {
        AdLoader adLoader = new AdLoader.Builder(containerView.getContext(), AdsSDK.nativeId)
                .forUnifiedNativeAd(unifiedNativeAd -> {
                    NativeTemplateStyle styles = new NativeTemplateStyle.Builder().build();
                    containerView.setVisibility(View.VISIBLE);
                    templateView.setVisibility(View.VISIBLE);
                    templateView.setStyles(styles);
                    templateView.setNativeAd(unifiedNativeAd);

                }).withAdListener(new AdListener() {
                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        containerView.setVisibility(View.GONE);
                        templateView.setVisibility(View.GONE);
                    }
                })
                .build();

        adLoader.loadAd(new AdRequest.Builder().build());
    }

    public static void loadBanner(ViewGroup viewGroup, AdSize adSize) {
        AdView adView = new AdView(viewGroup.getContext());
        adView.setAdUnitId(AdsSDK.bannerId);
        adView.setAdSize(adSize);
        viewGroup.addView(adView);

        adView.loadAd(new AdRequest.Builder().build());
    }

    // endregion


    // region interstitial
    private static HashMap<Integer, InterstitialAd> interstitialAdSet = new HashMap<>();

    public static <T> void loadInterstitialAd(@NonNull T target) {
        Context context = getContext(target);
        InterstitialAd ins = new InterstitialAd(context);
        ins.setAdUnitId(AdsSDK.interstitialId);
        interstitialAdSet.put(target.hashCode(), ins);

        ins.loadAd(new AdRequest.Builder().build());
    }

    public static <T> void releaseInterstitialAd(@NonNull T target) {
        interstitialAdSet.remove(target.hashCode());
    }

    public static <T> void showInterstitialAd(@NonNull T target, AdListener adListener) {
        InterstitialAd ins = interstitialAdSet.get(target.hashCode());
        if (ins == null || !ins.isLoaded()) {
            if (adListener != null) {
                adListener.onAdFailedToLoad(null);
            }

            loadInterstitialAd(target);
            return;
        }

        ins.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                super.onAdClosed();
                ins.loadAd(new AdRequest.Builder().build()); // cache new
                if (adListener != null) {
                    adListener.onAdClosed();
                }
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
                if (adListener != null) {
                    adListener.onAdFailedToLoad(loadAdError);
                }
            }

            @Override
            public void onAdClicked() {
                super.onAdClicked();
                if (adListener != null) {
                    adListener.onAdClicked();
                }
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
                if (adListener != null) {
                    adListener.onAdImpression();
                }
            }

            @Override
            public void onAdLoaded() {
                super.onAdLoaded();
                if (adListener != null) {
                    adListener.onAdLoaded();
                }
            }

            @Override
            public void onAdOpened() {
                super.onAdOpened();
                if (adListener != null) {
                    adListener.onAdOpened();
                }
            }
        });
        ins.show();
    }

    // endregion
    private static HashMap<Integer, RewardedAd> rewardAdSet = new HashMap<>();

    public static <T> void loadRewardAd(@NonNull T target) {
        RewardedAd rewardedAd = new RewardedAd(getContext(target), AdsSDK.rewardedId);
        rewardAdSet.put(target.hashCode(), rewardedAd);
        rewardedAd.loadAd(new AdRequest.Builder().build(), new RewardedAdLoadCallback());
    }

    public static <T> void showRewardAd(@NonNull T target, RewardedAdCallback callback) {
        RewardedAd rewardedAd = rewardAdSet.get(target.hashCode());
        if (rewardedAd == null || !rewardedAd.isLoaded()) {
            if (callback != null) {
                callback.onRewardedAdFailedToShow(null);
            }
            loadRewardAd(target);
            return;
        }

        rewardedAd.show(getActivityFromTarget(target), new RewardedAdCallback() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                if (callback != null) {
                    callback.onUserEarnedReward(rewardItem);
                }
            }

            @Override
            public void onRewardedAdClosed() {
                super.onRewardedAdClosed();
                rewardedAd.loadAd(new AdRequest.Builder().build(), new RewardedAdLoadCallback());
                if (callback != null) {
                    callback.onRewardedAdClosed();
                }
            }

            @Override
            public void onRewardedAdFailedToShow(AdError adError) {
                super.onRewardedAdFailedToShow(adError);
                if (callback != null) {
                    callback.onRewardedAdFailedToShow(adError);
                }
            }

            @Override
            public void onRewardedAdOpened() {
                super.onRewardedAdOpened();
                if (callback != null) {
                    callback.onRewardedAdOpened();
                }
            }
        });
    }

    public static <T> void releaseRewardAd(@NonNull T target) {
        rewardAdSet.remove(target.hashCode());
    }

    // region video reward


    // endregion

    // region private method helper
    private static <T> Context getContext(@NonNull T target) {
        if (target instanceof Fragment) {
            return ((Fragment) target).getContext();
        } else if (target instanceof FragmentActivity) {
            return (Context) target;
        } else {
            throw new NullPointerException("Load interestitial ad null target");
        }
    }

    private static <T> FragmentActivity getActivityFromTarget(@NonNull T target) {
        if (target instanceof Fragment) {
            return ((Fragment) target).getActivity();
        } else if (target instanceof FragmentActivity) {
            return (FragmentActivity) target;
        } else {
            throw new NullPointerException("Load reward ad null target");
        }
    }
    // endregion
}
