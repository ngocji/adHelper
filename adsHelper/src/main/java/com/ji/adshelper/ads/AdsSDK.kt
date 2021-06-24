package com.ji.adshelper.ads;

import android.Manifest;
import android.content.Context;

import androidx.annotation.RequiresPermission;

import com.google.android.gms.ads.MobileAds;

public class AdsSDK {
    public static String bannerId;
    public static String nativeId;
    public static String interstitialId;
    public static String rewardedId;
    public static String openAdId;


    @RequiresPermission(Manifest.permission.INTERNET)
    public static void init(Context context, String bannerId, String nativeId, String interstitialId, String rewardedId, String openAdId, boolean isDebug) {
        if (isDebug) {
            AdsSDK.bannerId = "ca-app-pub-3940256099942544/6300978111";
            AdsSDK.nativeId = "ca-app-pub-3940256099942544/2247696110";
            AdsSDK.interstitialId = "ca-app-pub-3940256099942544/1033173712";
            AdsSDK.rewardedId = "ca-app-pub-3940256099942544/5224354917";
            AdsSDK.openAdId = "ca-app-pub-3940256099942544/1033173712";
        } else {
            AdsSDK.bannerId = bannerId;
            AdsSDK.nativeId = nativeId;
            AdsSDK.interstitialId = interstitialId;
            AdsSDK.rewardedId = rewardedId;
            AdsSDK.openAdId = openAdId;
        }

        MobileAds.initialize(context);
    }
}
