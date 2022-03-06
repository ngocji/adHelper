package com.ji.adshelper.ads

abstract class OnAdListener {
    fun onAdLoadFailed(code: Int) {}
    fun onAdLoaded() {}
    fun onAdClosed() {}
}