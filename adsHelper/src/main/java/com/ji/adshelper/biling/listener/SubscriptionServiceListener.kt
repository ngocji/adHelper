package com.ji.adshelper.biling.listener

import com.ji.adshelper.biling.entities.DataWrappers

interface SubscriptionServiceListener : BillingServiceListener {

    fun onSubscriptionRestored(purchaseInfo: DataWrappers.PurchaseInfo)

    fun onSubscriptionPurchased(purchaseInfo: DataWrappers.PurchaseInfo)
}