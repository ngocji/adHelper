package com.ji.adshelper.consent

abstract class OnConsentListener {
    open fun onFailed() {}
    abstract fun onLoadAd()
}