package com.ji.adshelper.biling.listener

import com.ji.adshelper.biling.entities.DataWrappers


interface BillingServiceListener {

    fun onPricesUpdated(iapKeyPrices: Map<String, DataWrappers.ProductDetails>)

}