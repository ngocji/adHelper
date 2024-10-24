@file:Suppress("unused")

package com.ji.adshelper.ads

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner

@SuppressLint("StaticFieldLeak")
object ConnectivityHelper : DefaultLifecycleObserver {
    private var context: Context? = null
    private val listeners = mutableSetOf<OnConnectivityChangeListener>()

    private val networkChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isAvailable = isNetworkAvailable(context ?: return)
            listeners.forEach { it.onChanged(isAvailable) }
        }
    }

    fun init(application: Application) {
        this.context = application.applicationContext
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    fun addListener(listener: OnConnectivityChangeListener) {
        this.listeners.add(listener)
    }

    fun removeListener(listener: OnConnectivityChangeListener?) {
        this.listeners.remove(listener)
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        registerNetworkChangeReceiver()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        destroy()
    }

    private fun destroy() {
        unregisterNetworkChangeReceiver()
        context = null
        listeners.clear()
    }

    private fun registerNetworkChangeReceiver() {
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        context?.registerReceiver(networkChangeReceiver, intentFilter)
    }

    private fun unregisterNetworkChangeReceiver() {
        try {
            context?.unregisterReceiver(networkChangeReceiver)
        } catch (_: Exception) {
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networkCapabilities = connectivityManager.activeNetwork?.let {
                connectivityManager.getNetworkCapabilities(it)
            }
            networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            activeNetworkInfo?.isConnected == true
        }
    }

    fun interface OnConnectivityChangeListener {
        fun onChanged(isNetworkAvailable: Boolean)
    }
}