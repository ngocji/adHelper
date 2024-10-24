package com.x.y.z.adshelper

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdSize
import com.ji.adshelper.ads.AdsHelper

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AdsHelper.loadInterstitialAd(this)
        AdsHelper.loadRewardAd(this)

        findViewById<View>(R.id.buttonInteres).setOnClickListener {
            AdsHelper.showInterstitialAd(this, true, {
                Toast.makeText(this, "AdClosed", Toast.LENGTH_SHORT).show()
            })
        }

        findViewById<View>(R.id.buttonReward).setOnClickListener {
            AdsHelper.showRewardAd(this, true, {
                Toast.makeText(this, "AdClosed", Toast.LENGTH_SHORT).show()
            })
        }

        val banner = findViewById<ViewGroup>(R.id.bannerGroup)
        AdsHelper.loadBanner(viewGroup = banner, AdSize.BANNER)

        val native = findViewById<ViewGroup>(R.id.adsGroup)
        AdsHelper.loadNative(viewGroup = native, isMedium = true)
    }
}