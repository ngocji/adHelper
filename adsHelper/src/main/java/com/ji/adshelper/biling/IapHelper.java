package com.ji.adshelper.biling;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.android.billingclient.api.Purchase;
import com.google.gson.Gson;


public class IapHelper {
    private static final String PREF_SKU = "prefSku";
    private static final String PREF_EXPIRED_TIME = "prefExpiredTime";

    private static IapHelper instance;

    public static IapHelper getInstance(Context context) {
        if (instance == null) {
            instance = new IapHelper(context);
        }
        return instance;
    }

    private SharedPreferences sharedPreferences;
    private Gson gson = new Gson();

    private IapHelper(Context context) {
        sharedPreferences = context.getSharedPreferences("IapPref", Context.MODE_PRIVATE);
    }

    public boolean isPurchase() {
        Purchase purchase = getPurchase();
        return purchase != null && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED &&
                ((purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && purchase.isAutoRenewing()) ||
                        isAvailableExpiredTime());
    }

    public Purchase getPurchase() {
        String endCode = sharedPreferences.getString(PREF_SKU, null);
        try {
            return gson.fromJson(endCode, Purchase.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public long getPurchasedTime() {
        return sharedPreferences.getLong(PREF_EXPIRED_TIME, System.currentTimeMillis());
    }

    public void onPurchasedSuccess(@NonNull Purchase purchase, long expiredTime) {
        String endCode = gson.toJson(purchase);
        sharedPreferences.edit().putString(PREF_SKU, endCode).apply();

        if (purchase.isAutoRenewing()) {
            sharedPreferences.edit().putLong(PREF_EXPIRED_TIME, 0L).apply();
        } else {
            sharedPreferences.edit().putLong(PREF_EXPIRED_TIME, expiredTime).apply();
        }
    }


    public void onCancelPurchase() {
        if (!isAvailableExpiredTime()) {
            sharedPreferences.edit().remove(PREF_SKU).apply();
            sharedPreferences.edit().remove(PREF_EXPIRED_TIME).apply();
        }
    }

    private boolean isAvailableExpiredTime() {
        long expiredTime = sharedPreferences.getLong(PREF_EXPIRED_TIME, 0);
        return expiredTime == 0 || expiredTime > System.currentTimeMillis();
    }
}
