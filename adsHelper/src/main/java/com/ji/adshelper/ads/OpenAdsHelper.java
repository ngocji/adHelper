package com.ji.adshelper.ads;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import static androidx.lifecycle.Lifecycle.Event.ON_START;

public class OpenAdsHelper implements Application.ActivityLifecycleCallbacks, LifecycleObserver {
    public static final String ACTION_CLOSE = "openAdsHelper_ActionClose";
    public static final String ACTION_ERROR = "openAdsHelper_ActionError";

    private static OpenAdsHelper instance;

    private Context context;
    private AppOpenAd appOpenAd = null;
    private AppOpenAd.AppOpenAdLoadCallback loadCallback;
    private boolean isLoadingAd = false;
    private boolean isShowingAd = false;
    private Activity currentActivity;

    public static OpenAdsHelper getInstance() {
        if (instance == null) throw new NullPointerException("Open ad need init in application");
        return instance;
    }

    public static void init(Application application) {
        new OpenAdsHelper(application);
    }

    public OpenAdsHelper(Application application) {
        instance = this;
        context = application;
        loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
            /** Called when an app open ad has loaded */
            @Override
            public void onAppOpenAdLoaded(AppOpenAd ad) {
                appOpenAd = ad;
                isLoadingAd = false;
            }

            /** Called when an app open ad has failed to load */
            @Override
            public void onAppOpenAdFailedToLoad(LoadAdError loadAdError) {
                isLoadingAd = false;
            }
        };
        application.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
    }


    public void fetchAd() {
        // Fetch a new ad if we are not fetching them and there is no loaded ad available.
        if (isAdAvailable() || isLoadingAd) {
            return;
        }

        isLoadingAd = true;
        AdRequest request = new AdRequest.Builder().build();
        AppOpenAd.load(context, AdsSDK.openAdId, request, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
    }

    public void showAdIfAvailable() {
        // Show ad if an ad is available and no open ad is showing currently
        if (!isShowingAd && isAdAvailable()) {
            FullScreenContentCallback fullScreenContentCallback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            appOpenAd = null;
                            isShowingAd = false;
                            fetchAd();
                            sendEvent(ACTION_CLOSE);
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {
                            sendEvent(ACTION_ERROR);
                        }

                        @Override
                        public void onAdShowedFullScreenContent() {
                            isShowingAd = true;
                        }
                    };
            appOpenAd.show(currentActivity, fullScreenContentCallback);
        } else {
            //fetch a new ad if needed
            fetchAd();
            sendEvent(ACTION_ERROR);
        }
    }

    public boolean isAdAvailable() {
        return appOpenAd != null;
    }

    // region override lifecycler
    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {

    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        currentActivity = null;
    }


    /**
     * LifecycleObserver methods
     */
    @OnLifecycleEvent(ON_START)
    public void onStart() {
        // automatically show an app open ad when the application starts or reopens from background
        showAdIfAvailable();
    }

    // endregion

    private void sendEvent(String action) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(action));
    }
}
