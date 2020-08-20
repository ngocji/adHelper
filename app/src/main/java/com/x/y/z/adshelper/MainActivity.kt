package com.x.y.z.adshelper

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdSize.BANNER
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAdCallback
import com.ji.adshelper.AdsHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AdsHelper.loadBanner(bannerGroup, BANNER)
        AdsHelper.loadNative(adsGroup, title_template)
//
        AdsHelper.loadInterstitialAd(this)
        AdsHelper.loadRewardAd(this)
//
//
        buttonInteres.setOnClickListener { AdsHelper.showInterstitialAd(this, null) }
        buttonReward.setOnClickListener {
            AdsHelper.showRewardAd(this, object : RewardedAdCallback() {
                override fun onUserEarnedReward(p0: RewardItem) {
                    Log.e("OnReward", "RUn")
                }
            })
        }
    }
}