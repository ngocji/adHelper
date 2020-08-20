package com.ji.adshelper;

import android.Manifest;
import android.content.Context;

import androidx.annotation.RequiresPermission;

import com.google.android.gms.ads.MobileAds;

public class AdsSDK {
    public static String bannerId;
    public static String nativeId;
    public static String interstitialId;
    public static String rewardedId;


    @RequiresPermission(Manifest.permission.INTERNET)
    public static void init(Context context, String bannerId, String nativeId, String interstitialId, String rewardedId, boolean isDebug) {
        if (isDebug) {
            AdsSDK.bannerId = "ca-app-pub-3940256099942544/6300978111";
            AdsSDK.nativeId = "ca-app-pub-3940256099942544/2247696110";
            AdsSDK.interstitialId = "ca-app-pub-3940256099942544/1033173712";
            AdsSDK.rewardedId = "ca-app-pub-3940256099942544/5224354917";
        } else {
            AdsSDK.bannerId = bannerId;
            AdsSDK.nativeId = nativeId;
            AdsSDK.interstitialId = interstitialId;
            AdsSDK.rewardedId = rewardedId;
        }
        MobileAds.initialize(context);
    }
}
