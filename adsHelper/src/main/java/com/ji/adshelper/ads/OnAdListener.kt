package com.ji.adshelper.ads

abstract class OnAdListener {
    open fun onAdLoadFailed(code: Int) {}
    open fun onAdLoaded() {}
    open fun onAdClosed() {}
    open fun onAdLoading() {}
}