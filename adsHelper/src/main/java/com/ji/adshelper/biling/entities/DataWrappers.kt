package com.ji.adshelper.biling.entities

import com.android.billingclient.api.AccountIdentifiers
import com.android.billingclient.api.BillingClient

class DataWrappers {


    data class ProductDetails(
        val productId: String,
        val productType: ProductType?,
        val title: String?,
        val description: String?,
        val price: String?,
        val priceAmount: Double?,
        val priceCurrencyCode: String?,
        val offers: List<Offer>? = null,
        val trialPeriod: Int = 0,
        val period: Int = 0
    ) {
        data class Offer(
            val offerId: String?,
            val offerToken: String?,
            val priceAmount: Double?,
            val price: String?,
            val priceCurrencyCode: String?
        )
    }


    data class PurchaseInfo(
        val purchaseState: PurchasedState,
        val developerPayload: String,
        val isAcknowledged: Boolean,
        val isAutoRenewing: Boolean,
        val orderId: String?,
        val originalJson: String,
        val packageName: String,
        val purchaseTime: Long,
        val purchaseToken: String,
        val signature: String,
        val productId: String,
        val accountIdentifiers: AccountIdentifiers?
    )
}