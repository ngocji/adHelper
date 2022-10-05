package com.ji.adshelper.biling.listener

import com.ji.adshelper.biling.entities.DataWrappers

interface PurchaseServiceListener : BillingServiceListener {

    override fun onPricesUpdated(iapKeyPrices: Map<String, DataWrappers.ProductDetails>)

    fun onProductPurchased(purchaseInfo: DataWrappers.PurchaseInfo)


    fun onProductRestored(purchaseInfo: DataWrappers.PurchaseInfo)
}