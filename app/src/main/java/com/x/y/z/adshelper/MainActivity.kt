package com.x.y.z.adshelper

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ji.adshelper.ads.OnAdCloseListener
import com.ji.adshelper.ads.OnAdListener
import com.ji.adshelper.ads.OpenAdsHelper

class MainActivity : AppCompatActivity() {
    private var isCheck = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (!isCheck) {
            isCheck = true
            OpenAdsHelper.getInstance()
                ?.setAdListener(object : OnAdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        Log.e("Ads", "onAdLoaded")
                        OpenAdsHelper.getInstance()?.show(object : OnAdCloseListener {
                            override fun onClose() {
                                OpenAdsHelper.getInstance()?.removeAdListener()
                            }
                        })
                    }

                    override fun onAdLoadFailed(code: Int) {
                        super.onAdLoadFailed(code)
                        Log.e("Ads", "onAdLoadFailed: $code")
                    }
                })
                ?.fetch()
        }
    }
}