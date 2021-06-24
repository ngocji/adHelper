package com.ji.adshelper.biling

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast

interface IapHelper {
    fun showPurchaseErrorMessage()
    fun showPurchaseSuccessMessage()
    fun isItemPurchased(productId: String): Boolean
    fun onProductPurchased(productId: String)
    fun onProductRevoked(productId: String)
}

open class IapHelperImpl(private val context: Context) : IapHelper {

    companion object {
        const val PREF_NAME = "iap.pref"
    }

    private val pref: SharedPreferences by lazy { context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) }

    override fun showPurchaseErrorMessage() {
        Toast.makeText(context.applicationContext,
                "Something wrong, can not purchase this item now, please try again",
                Toast.LENGTH_LONG).show()
    }

    override fun showPurchaseSuccessMessage() {
        Toast.makeText(context.applicationContext,
                "Purchase successfully, thank you for your purchase",
                Toast.LENGTH_LONG).show()
    }

    override fun isItemPurchased(productId: String): Boolean = pref.getBoolean(productId, false)

    override fun onProductPurchased(productId: String) {
        pref.edit().putBoolean(productId, true).apply()
    }

    override fun onProductRevoked(productId: String) {
        pref.edit().putBoolean(productId, false).apply()
    }
}