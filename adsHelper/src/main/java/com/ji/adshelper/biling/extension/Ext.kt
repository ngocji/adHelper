package com.ji.adshelper.biling.extension

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetails.PricingPhases
import com.android.billingclient.api.ProductDetails.SubscriptionOfferDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import com.ji.adshelper.biling.BillingService
import com.ji.adshelper.biling.Security
import com.ji.adshelper.biling.entities.DataWrappers
import com.ji.adshelper.biling.entities.ProductType
import com.ji.adshelper.biling.entities.PurchasedState

fun Purchase.getPurchaseInfo(): DataWrappers.PurchaseInfo {
    return DataWrappers.PurchaseInfo(
        purchaseState = PurchasedState.from(purchaseState),
        developerPayload = developerPayload,
        isAcknowledged = isAcknowledged,
        isAutoRenewing = isAutoRenewing,
        orderId = orderId,
        originalJson = originalJson,
        packageName = packageName,
        purchaseTime = purchaseTime,
        purchaseToken = purchaseToken,
        signature = signature,
        productId = products.find { it.isNotEmpty() } ?: "",
        accountIdentifiers = accountIdentifiers
    )
}

private fun findOfferPricePhase(
    offerId: String?,
    pricePhases: PricingPhases
): ProductDetails.PricingPhase? {
    return pricePhases.pricingPhaseList.find {
        it.priceAmountMicros > 0
    }
}

private fun findBasePricingSubs(subscriptionOfferDetails: List<SubscriptionOfferDetails>?): ProductDetails.PricingPhase? {
    return subscriptionOfferDetails?.find { it.offerId == null }?.let { product ->
        return findOfferPricePhase(null, product.pricingPhases)
    }
}


private fun List<SubscriptionOfferDetails>.mapToOffer(): List<DataWrappers.ProductDetails.Offer> {
    return filter { !it.offerId.isNullOrBlank() }
        .map { item ->
            val pricePhaseDetail = findOfferPricePhase(item.offerId, item.pricingPhases)
            DataWrappers.ProductDetails.Offer(
                offerId = item.offerId,
                offerToken = item.offerToken,
                priceAmount = pricePhaseDetail?.priceAmountMicros?.toPriceAmount(),
                price = pricePhaseDetail?.formattedPrice,
                priceCurrencyCode = pricePhaseDetail?.priceCurrencyCode
            )
        }
}

private fun findSubscriptionTrial(subscriptionOfferDetails: List<SubscriptionOfferDetails>?): Int {
    subscriptionOfferDetails?.forEach { detail ->
        detail.pricingPhases.pricingPhaseList.forEach { pricingPhase ->
            if (pricingPhase.priceAmountMicros <= 0) {
                return parsePeriod(pricingPhase.billingPeriod)
            }
        }
    }

    return 0
}

private fun parsePeriod(billingPeriod: String): Int {
    if (billingPeriod.isBlank()) return 0
    var day = 0
    val period = billingPeriod.substring(1)
    val type = period.substring(period.length - 1)
    val count = period.substring(0, period.length - 1).toIntOrNull() ?: 0
    when (type.uppercase()) {
        "D" -> day = count
        "W" -> day = count * 7
        "M" -> day = count * 30
        "Y" -> day = count * 365
    }
    return day
}

fun ProductDetails.toMap() = this.run {
    val type = ProductType.safe(
        type = productType,
        isConsumer = listOf(productId).isConsumable()
    )
    var price: String? = null
    var priceAmount: Double? = null
    var priceCurrencyCode: String? = null
    var offers: List<DataWrappers.ProductDetails.Offer>? = null
    var trialPeriod = 0
    var period = 0

    when (type) {
        ProductType.SUBSCRIPTION -> {
            trialPeriod = findSubscriptionTrial(subscriptionOfferDetails)
            findBasePricingSubs(subscriptionOfferDetails)?.also {
                price = it.formattedPrice
                priceAmount = it.priceAmountMicros.toPriceAmount()
                priceCurrencyCode = it.priceCurrencyCode
                period = parsePeriod(it.billingPeriod)
            }

            offers = subscriptionOfferDetails?.mapToOffer()
        }

        else -> {
            price = oneTimePurchaseOfferDetails?.formattedPrice
            priceAmount = oneTimePurchaseOfferDetails?.priceAmountMicros?.toPriceAmount()
            priceCurrencyCode = oneTimePurchaseOfferDetails?.priceCurrencyCode
        }
    }

    DataWrappers.ProductDetails(
        title = title,
        description = description,
        priceCurrencyCode = priceCurrencyCode,
        price = price,
        priceAmount = priceAmount,
        productId = productId,
        productType = type,
        offers = offers,
        trialPeriod = trialPeriod,
        period = period
    )
}

private fun Long.toPriceAmount() = this.div(1000000.0)

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

fun List<String>.isConsumable(): Boolean {
    val hasConsumableSku = any { BillingService.consumableSkus.contains(it) }
    val hasNonConsumableSku =
        any {
            BillingService.nonConsumableSkus.contains(it) || BillingService.subscriptionSkus.contains(
                it
            )
        }
    return hasConsumableSku && !hasNonConsumableSku
}