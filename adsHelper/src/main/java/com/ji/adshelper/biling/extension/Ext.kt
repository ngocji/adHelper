package com.ji.adshelper.biling.extension

import com.android.billingclient.api.*
import com.ji.adshelper.biling.Security
import com.ji.adshelper.biling.entities.DataWrappers

fun Purchase.getPurchaseInfo(): DataWrappers.PurchaseInfo {
    return DataWrappers.PurchaseInfo(
        purchaseState,
        developerPayload,
        isAcknowledged,
        isAutoRenewing,
        orderId,
        originalJson,
        packageName,
        purchaseTime,
        purchaseToken,
        signature,
        products.find { it.isNotEmpty() } ?: "",
        accountIdentifiers
    )
}

fun ProductDetails.toMapSUBS() = this.run {
    val pricing = findPricingSubs(subscriptionOfferDetails)
    DataWrappers.ProductDetails(
        title = title,
        description = description,
        priceCurrencyCode = pricing?.priceCurrencyCode,
        price = pricing?.formattedPrice,
        priceAmount = pricing?.priceAmountMicros?.div(
            1000000.0
        )
    )
}

fun findPricingSubs(subscriptionOfferDetails: List<ProductDetails.SubscriptionOfferDetails>?): ProductDetails.PricingPhase? {
    subscriptionOfferDetails?.forEach { product ->
        val item = product.pricingPhases.pricingPhaseList.find { it.priceAmountMicros > 0 }
        if (item != null) return item
    }

    return null
}

fun ProductDetails.toMap() = this.run {
    DataWrappers.ProductDetails(
        title = title,
        description = description,
        priceCurrencyCode = oneTimePurchaseOfferDetails?.priceCurrencyCode,
        price = oneTimePurchaseOfferDetails?.formattedPrice,
        priceAmount = oneTimePurchaseOfferDetails?.priceAmountMicros?.div(
            1000000.0
        )
    )
}

fun BillingResult.isOk(): Boolean {
    return this.responseCode == BillingClient.BillingResponseCode.OK
}

fun String?.isSignatureValid(purchase: Purchase): Boolean {
    val key = this ?: return true
    return Security.verifyPurchase(purchase.originalJson, purchase.signature, key)
}

fun String.getQueryPurchasesParams(): QueryPurchasesParams {
    return QueryPurchasesParams.newBuilder().setProductType(this)
        .build()
}
