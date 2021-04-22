package com.ji.adshelper.ads;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.ji.adshelper.view.NativeTemplateStyle;
import com.ji.adshelper.view.TemplateView;

import java.util.HashMap;

public class AdsHelper {

    // region banner
    public static void loadNative(View containerView, TemplateView templateView) {
        AdLoader adLoader = new AdLoader.Builder(containerView.getContext(), AdsSDK.nativeId)
                .forNativeAd(nativeAd -> {
                    NativeTemplateStyle styles = new NativeTemplateStyle.Builder().build();
                    containerView.setVisibility(View.VISIBLE);
                    templateView.setVisibility(View.VISIBLE);
                    templateView.setStyles(styles);
                    templateView.setNativeAd(nativeAd);
                })
                .withAdListener(new com.google.android.gms.ads.AdListener() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
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
        InterstitialAd.load(context,
                AdsSDK.interstitialId,
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        super.onAdLoaded(interstitialAd);
                        interstitialAdSet.put(target.hashCode(), interstitialAd);
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                    }
                }
        );
    }

    public static <T> void releaseInterstitialAd(@NonNull T target) {
        interstitialAdSet.remove(target.hashCode());
    }

    public static <T> void showInterstitialAd(@NonNull T target, boolean cacheNew, AdListener adListener) {
        InterstitialAd ins = interstitialAdSet.get(target.hashCode());
        if (ins == null) {
            if (adListener != null) {
                adListener.onAdLoadFailed();
            }

            loadInterstitialAd(target);
            return;
        }

        ins.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                super.onAdFailedToShowFullScreenContent(adError);
                if (adListener != null) {
                    adListener.onAdLoadFailed();
                }
                interstitialAdSet.remove(target.hashCode());
                if (cacheNew) loadInterstitialAd(target);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                super.onAdShowedFullScreenContent();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                super.onAdDismissedFullScreenContent();
                if (adListener != null) {
                    adListener.onAdRewarded();
                }

                interstitialAdSet.remove(target.hashCode());
                if (cacheNew) loadInterstitialAd(target);
            }

            @Override
            public void onAdImpression() {
                super.onAdImpression();
            }
        });

        ins.show(getActivity(target));
    }

    // endregion
    private static HashMap<Integer, RewardedAd> rewardAdSet = new HashMap<>();

    public static <T> void loadRewardAd(@NonNull T target) {
        RewardedAd.load(getContext(target), AdsSDK.rewardedId, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                super.onAdLoaded(rewardedAd);
                rewardAdSet.put(target.hashCode(), rewardedAd);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                super.onAdFailedToLoad(loadAdError);
            }
        });
    }

    public static <T> void showRewardAd(@NonNull T target, boolean cacheNew, AdListener adListener) {
        RewardedAd rewardedAd = rewardAdSet.get(target.hashCode());
        if (rewardedAd == null) {
            if (adListener != null) {
                adListener.onAdLoadFailed();
            }
            loadRewardAd(target);
            return;
        }

        rewardedAd.show(getActivity(target), rewardItem -> {
            adListener.onAdRewarded();
            rewardAdSet.remove(target.hashCode());
            if (cacheNew) loadRewardAd(target);
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

    private static <T> Activity getActivity(@NonNull T target) {
        if (target instanceof Fragment) {
            return ((Fragment) target).requireActivity();
        } else if (target instanceof FragmentActivity) {
            return (Activity) target;
        } else {
            throw new NullPointerException("Load interestitial ad null target");
        }
    }
    // endregion

    public static abstract class AdListener {
        public abstract void onAdLoadFailed();

        public abstract void onAdRewarded();
    }
}
