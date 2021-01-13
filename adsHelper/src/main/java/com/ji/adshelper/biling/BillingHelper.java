package com.ji.adshelper.biling;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;

import java.util.Arrays;
import java.util.List;

public class BillingHelper {
    private String TAG = "PurchaseHelper";

    private Context context;

    private BillingClient mBillingClient;

    private BillingListener billingListener;

    private boolean mIsServiceConnected;

    private int billingSetupResponseCode;

    public BillingHelper(Context context, BillingListener billingListener) {
        this.context = context;
        this.billingListener = billingListener;
        mBillingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(getPurchaseUpdatedListener()).build();
        startConnection(getServiceConnectionRequest());
    }

    public boolean isServiceConnected() {
        return mIsServiceConnected;
    }

    public void endConnection() {
        if (mBillingClient != null && mBillingClient.isReady()) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
    }

    public void getPurchasedItems(@BillingClient.SkuType String skuType) {
        Runnable purchaseHistoryRequest = () -> {
            Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(skuType);
            if (billingListener != null) {
                billingListener.onPurchaseHistoryResponse(purchasesResult.getPurchasesList());
            }
        };

        executeServiceRequest(purchaseHistoryRequest);
    }

    public void getSkuDetails(@BillingClient.SkuType String skuType, final String... skuId) {
        Runnable skuDetailsRequest = () -> {
            SkuDetailsParams skuParams;
            skuParams = SkuDetailsParams.newBuilder().setType(skuType).setSkusList(Arrays.asList(skuId)).build();

            mBillingClient.querySkuDetailsAsync(skuParams, (responseCode, skuDetailsList) -> {
                Log.d(TAG, "getSkuDetails: " + responseCode);
                if (responseCode.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                    if (billingListener != null) {
                        billingListener.onSkuQueryResponse(skuDetailsList);
                    }
                }
            });
        };

        executeServiceRequest(skuDetailsRequest);
    }

    public void launchBillingFLow(SkuDetails skuDetails) {
        Runnable launchBillingRequest = () -> {
            BillingFlowParams mBillingFlowParams;
            mBillingFlowParams = BillingFlowParams.newBuilder()
                    .setSkuDetails(skuDetails)
                    .build();

            mBillingClient.launchBillingFlow((Activity) context, mBillingFlowParams);
        };

        executeServiceRequest(launchBillingRequest);
    }

    public void gotoManageSubscription() {
        String PACKAGE_NAME = context.getPackageName();
        Log.d(TAG, "gotoManageSubscription: " + PACKAGE_NAME);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/account/subscriptions?package=" + PACKAGE_NAME));
        context.startActivity(browserIntent);
    }

    public void acknowledgePurchase(Purchase purchase) {
        if (mBillingClient == null) return;

        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        mBillingClient.acknowledgePurchase(params, billingResult -> {
            if (billingListener != null) {
                billingListener.onPurchaseSuccess(purchase);
            }
        });
    }

    private boolean isSubscriptionSupported() {
        int responseCode = mBillingClient.isFeatureSupported(BillingClient.SkuType.SUBS).getResponseCode();
        if (responseCode != BillingClient.BillingResponseCode.OK)
            Log.d(TAG, "isSubscriptionSupported() got an error response: " + responseCode);
        return responseCode == BillingClient.BillingResponseCode.OK;
    }

    private boolean isInAppSupported() {
        int responseCode = mBillingClient.isFeatureSupported(BillingClient.SkuType.INAPP).getResponseCode();
        if (responseCode != BillingClient.BillingResponseCode.OK)
            Log.d(TAG, "isInAppSupported() got an error response: " + responseCode);
        return responseCode == BillingClient.BillingResponseCode.OK;
    }

    private void startConnection(Runnable onSuccessRequest) {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                Log.d(TAG, "onBillingSetupFinished: " + billingResult.getResponseCode());
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    mIsServiceConnected = true;
                    billingSetupResponseCode = billingResult.getResponseCode();
                    if (onSuccessRequest != null) {
                        onSuccessRequest.run();
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                mIsServiceConnected = false;
                Log.d(TAG, "onBillingServiceDisconnected: ");
            }
        });
    }

    private PurchasesUpdatedListener getPurchaseUpdatedListener() {
        return (billingResult, purchases) -> {
            if (billingListener != null) {
                billingListener.onPurchasesUpdated(billingResult.getResponseCode(), purchases);
            }
        };
    }

    private Runnable getServiceConnectionRequest() {
        return () -> {
            if (billingListener != null) {
                billingListener.onServiceConnected(billingSetupResponseCode);
            }
        };
    }

    private void executeServiceRequest(Runnable runnable) {
        if (mIsServiceConnected) {
            runnable.run();
        } else {
            startConnection(runnable);
        }
    }


    public interface BillingListener {
        void onServiceConnected(@BillingClient.BillingResponseCode int resultCode);

        void onSkuQueryResponse(List<SkuDetails> skuDetails);

        void onPurchaseHistoryResponse(List<Purchase> purchasedItems);

        void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases);

        void onPurchaseSuccess(Purchase purchase);
    }
}
