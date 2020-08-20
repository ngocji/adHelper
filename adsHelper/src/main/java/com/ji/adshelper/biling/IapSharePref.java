package com.ji.adshelper.biling;

import android.content.Context;
import android.content.SharedPreferences;

public class IapSharePref {
    private static IapSharePref instance;

    public static IapSharePref getInstance(Context context) {
        if (instance == null) {
            instance = new IapSharePref(context);
        }
        return instance;
    }

    private SharedPreferences sharedPreferences;

    private IapSharePref(Context context) {
        sharedPreferences = context.getSharedPreferences("iap_share_pref", Context.MODE_PRIVATE);
    }

    public boolean isPurchased(String productId) {
        return sharedPreferences.getBoolean(productId, false);
    }

    public void onPurchased(String productId) {
        sharedPreferences.edit().putBoolean(productId, true).apply();
    }

    public void cancelPurchased(String productId) {
        sharedPreferences.edit().putBoolean(productId, false).apply();
    }
}
