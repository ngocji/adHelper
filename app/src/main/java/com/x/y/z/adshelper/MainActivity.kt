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
import com.ji.adshelper.ads.AdsHelper
import com.ji.adshelper.ads.OpenAdsHelper
import kotlinx.android.synthetic.main.activity_main.*
import java.util.regex.Pattern

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
        buttonInteres.setOnClickListener { AdsHelper.showInterstitialAd(this, false, null) }
        buttonReward.setOnClickListener {
            AdsHelper.showRewardAd(this, false, object : AdsHelper.AdListener() {
                override fun onAdLoadFailed() {
                }

                override fun onAdRewarded() {
                    Log.e("OnReward", "RUn")
                }
            })
        }

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    Log.e("RUn: ", "Action: ${intent?.action}")
                }
            }, IntentFilter().apply {
                addAction(OpenAdsHelper.ACTION_CLOSE)
                addAction(OpenAdsHelper.ACTION_ERROR)
            })

        lalal()
    }

    private fun lalal() {
        val s = ""

        val mPattern = Pattern.compile("#[a-zA-Z0-9]{6}")

        val matcher = mPattern.matcher(s)
        var from = 0
        while (matcher.find(from)){
            Log.e("OnMatch: ", matcher.group()+"/"+matcher.group(0)+"/"+matcher.start())
            from++
        }
    }
}