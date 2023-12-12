package com.ji.adshelper.biling.entities

import com.android.billingclient.api.BillingClient

enum class ProductType(internal val productType: String) {
    INAPP(BillingClient.ProductType.INAPP), SUBSCRIPTION(BillingClient.ProductType.SUBS), CONSUMER(BillingClient.ProductType.INAPP);

    companion object {
        fun safe(type: String?, isConsumer: Boolean): ProductType {
            return when {
                type == BillingClient.ProductType.SUBS -> SUBSCRIPTION
                isConsumer -> CONSUMER
                else -> INAPP
            }
        }
    }
}