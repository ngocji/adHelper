package com.ji.adshelper.biling;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.SkuDetails;
import com.anjlab.android.iab.v3.TransactionDetails;

public class BillingProcessHelper implements BillingProcessor.IBillingHandler {
    private Context context;
    private String iapLicenseKey;
    private String merchantId;
    private Callback callback;

    private IapSharePref iapSharePref;
    private BillingProcessor processor;
    private String restoreProductId;

    public BillingProcessHelper(Context context, @NonNull String iapLicenseKey, String merchantId, Callback callback) {
        this.context = context;
        this.iapLicenseKey = iapLicenseKey;
        this.merchantId = merchantId;
        this.callback = callback;
        iapSharePref = IapSharePref.getInstance(context);
        initProcess();
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        if (productId == null) return;

        iapSharePref.isPurchased(productId);
        if (callback != null) {
            callback.onPurchased(productId);
        }
    }

    @Override
    public void onPurchaseHistoryRestored() {
        if (TextUtils.isEmpty(restoreProductId) || processor == null || !processor.isInitialized())
            return;

        if (processor.isPurchased(restoreProductId)) {
            iapSharePref.isPurchased(restoreProductId);
            if (callback != null) {
                callback.onPurchaseHistoryRestored(restoreProductId);
            }
        }
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        if (callback != null) {
            callback.onBillingError(error);
        }
    }

    @Override
    public void onBillingInitialized() {
        if (callback != null) {
            callback.onBillingInitialized();
        }
    }

    // region purchase

    public void purchase(FragmentActivity target, String productId) {
        if (processor == null || !processor.isInitialized()) {
            initProcess();
        }
        if (BillingProcessor.isIabServiceAvailable(target.getApplicationContext())) {
            processor.purchase(target, productId);
        }
    }

    public void subscribe(FragmentActivity target, String productId) {
        if (processor == null || !processor.isInitialized()) {
            initProcess();
        }
        if (BillingProcessor.isIabServiceAvailable(target.getApplicationContext())) {
            processor.subscribe(target, productId);
        }
    }


    public void restore(String productId) {
        restoreProductId = productId;
        if (processor != null && processor.isInitialized()) {
            processor.loadOwnedPurchasesFromGoogle();
        }
    }

    public SkuDetails getPrice(String productId) {
        if (processor == null || !processor.isInitialized()) return null;
        return processor.getPurchaseListingDetails(productId);
    }

    public SkuDetails getSubscriptionPrice(String productId) {
        if (processor == null || !processor.isInitialized()) return null;
        return processor.getSubscriptionListingDetails(productId);
    }

    public boolean handleResultPurchase(int requestCode, int resultCode, Intent data) {
        return processor != null && processor.handleActivityResult(requestCode, resultCode, data);
    }

    // endregion


    // region private method
    private void initProcess() {
        processor = BillingProcessor.newBillingProcessor(context, iapLicenseKey, merchantId, this);
        processor.initialize();
    }

    // endregion


    public interface Callback {
        void onBillingInitialized();

        void onPurchaseHistoryRestored(String productId);

        void onPurchased(String productId);

        void onBillingError(Throwable throwable);
    }
}
