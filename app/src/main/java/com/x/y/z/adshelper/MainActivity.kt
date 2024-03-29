package com.x.y.z.adshelper

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdSize.BANNER
import com.ji.adshelper.ads.AdsHelper
import com.ji.adshelper.consent.ConsentInfo
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<View>(R.id.buttonInteres).setOnClickListener { AdsHelper.showInterstitialAd(this, false, null) }
        findViewById<View>(R.id.buttonReward).setOnClickListener {
            AdsHelper.showRewardAd(this, false, object : AdsHelper.AdListener() {
                override fun onAdLoadFailed() {
                }

                override fun onAdRewarded() {
                    Log.e("OnReward", "RUn")
                }
            })
        }
    }
}