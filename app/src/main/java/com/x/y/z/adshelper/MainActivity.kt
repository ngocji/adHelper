package com.x.y.z.adshelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.ads.AdSize.BANNER
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAdCallback
import com.ji.adshelper.ads.AdsHelper
import com.ji.adshelper.ads.OpenAdsHelper
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

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(object :BroadcastReceiver(){
                override fun onReceive(context: Context?, intent: Intent?) {
                    Log.e("RUn: ", "Action: ${intent?.action}")
                }
            }, IntentFilter().apply {
                addAction(OpenAdsHelper.ACTION_CLOSE)
                addAction(OpenAdsHelper.ACTION_ERROR)
            })
    }
}