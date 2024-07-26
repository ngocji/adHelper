package com.ji.adshelper.biling

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.WorkerThread
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResponseListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.ji.adshelper.biling.entities.DataWrappers
import com.ji.adshelper.biling.entities.ProductType
import com.ji.adshelper.biling.extension.getPurchaseInfo
import com.ji.adshelper.biling.extension.getQueryPurchasesParams
import com.ji.adshelper.biling.extension.isConsumable
import com.ji.adshelper.biling.extension.isOk
import com.ji.adshelper.biling.extension.isSignatureValid
import com.ji.adshelper.biling.extension.toMap
import com.ji.adshelper.biling.listener.BillingServiceListener
import java.util.Timer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors

object BillingService {
    private const val TAG = "BillingService"
    private var billingClient: BillingClient? = null

    var nonConsumableSkus: List<String> = emptyList()
    var consumableSkus: List<String> = emptyList()
    var subscriptionSkus: List<String> = emptyList()

    private var decodedKey: String? = null
    private val productDetailsMap = mutableMapOf<String, ProductDetails?>()
    private val handler = Handler(Looper.getMainLooper())
    private val backgroundExecutor by lazy { Executors.newSingleThreadExecutor() }

    private var billingServiceListener: BillingServiceListener? = null

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { billingResult, purchases ->
            if (billingResult.isOk() && purchases != null) {
                processPurchases(purchases)
            } else {
                billingServiceListener?.onPurchaseError(billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED)
            }
        }

    private val billingClientStateListener =
        object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                val responseCode = billingResult.responseCode
                val debugMessage = billingResult.debugMessage
                Timer("$TAG: $responseCode $debugMessage")
                when {
                    billingResult.isOk() -> {
                        querySkuDetailsAsync()
                        refreshPurchasesAsync()
                    }

                    else -> {}
                }
            }

            override fun onBillingServiceDisconnected() {
                Timer("$TAG: onBillingServiceDisconnected: disconnect service")
            }
        }


    private val productDetailsResponse =
        ProductDetailsResponseListener { billingResult, productDetailsList ->
            val responseCode = billingResult.responseCode
            val debugMessage = billingResult.debugMessage
            Timer("$TAG: $responseCode $debugMessage")
            when {
                billingResult.isOk() -> {
                    productDetailsList.forEach { item ->
                        productDetailsMap[item.productId] = item
                    }

                    productDetailsMap.mapNotNull { entry ->
                        entry.value?.let {
                            entry.key to it.toMap()
                        }
                    }.let {
                        updatePrices(it.toMap())
                    }
                }

                else -> {}
            }
        }

    @JvmStatic
    fun init(
        app: Application,
        nonConsumableSkus: List<String> = emptyList(),
        consumableSkus: List<String> = emptyList(),
        subscriptionSkus: List<String> = emptyList(),
        pendingPurchasesParams: PendingPurchasesParams,
        decodedKey: String?,
    ) {
        BillingService.nonConsumableSkus = nonConsumableSkus
        BillingService.consumableSkus = consumableSkus
        BillingService.subscriptionSkus = subscriptionSkus
        BillingService.decodedKey = decodedKey

        billingClient =
            BillingClient.newBuilder(app.applicationContext).setListener(purchasesUpdatedListener)
                .enablePendingPurchases(pendingPurchasesParams).build()
        billingClient?.startConnection(billingClientStateListener)
    }

    @JvmStatic
    fun setListener(listener: BillingServiceListener) {
        this.billingServiceListener = listener
    }


    @JvmStatic
    fun queryPurchasedInfo(
        type: ProductType,
        id: String,
        action: (DataWrappers.PurchaseInfo) -> Unit
    ) {
        billingClient?.queryPurchasesAsync(type.productType.getQueryPurchasesParams()) { result, purchases ->
            if (result.isOk()) {
                purchases.find { it.products.contains(id) }?.also {
                    action.invoke(it.getPurchaseInfo())
                }
            }
        }
    }

    private fun updatePrices(iapKeyPrices: Map<String, DataWrappers.ProductDetails>) {
        handler.post {
            billingServiceListener?.onPricesUpdated(iapKeyPrices)
        }
    }


    private fun querySkuDetailsAsync() {
        if (nonConsumableSkus.isNotEmpty() || consumableSkus.isNotEmpty()) {
            val productIds = mutableListOf<String>().apply {
                addAll(nonConsumableSkus)
                addAll(consumableSkus)
            }

            val products = getProduct(BillingClient.ProductType.INAPP, productIds)
            val params = QueryProductDetailsParams.newBuilder().setProductList(products)
            billingClient?.queryProductDetailsAsync(params.build(), productDetailsResponse)
        }

        if (subscriptionSkus.isNotEmpty()) {
            val products = getProduct(BillingClient.ProductType.SUBS, subscriptionSkus)
            val params = QueryProductDetailsParams.newBuilder().setProductList(products)
            billingClient?.queryProductDetailsAsync(params.build(), productDetailsResponse)
        }
    }


    private fun getProduct(
        productType: String,
        skus: List<String>
    ): List<QueryProductDetailsParams.Product> {
        val productList = mutableListOf<QueryProductDetailsParams.Product>()
        if (skus.isNotEmpty()) {
            skus.forEach {
                productList.add(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it)
                        .setProductType(productType)
                        .build()
                )
            }
        }
        return productList
    }

    @JvmStatic
    fun launchBillingFlow(
        activity: Activity,
        productId: String,
    ): Boolean {
        return launchBillingFlow(activity = activity, productId = productId, offerToken = null)
    }

    @JvmStatic
    fun launchBillingFlow(
        activity: Activity,
        productId: String,
        offerToken: String?
    ): Boolean {
        val productDetails = productDetailsMap[productId] ?: return false
        val optimizeOfferToken = offerToken
            ?: productDetails.subscriptionOfferDetails?.find { it.offerToken.isNotEmpty() }?.offerToken
        val productDetailsParamsList =
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .apply {
                        if (optimizeOfferToken != null) {
                            setOfferToken(optimizeOfferToken)
                        }

                        setProductDetails(productDetails)
                    }
                    .build()
            )
        val billingFlowParams =
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build()
        return billingClient?.launchBillingFlow(activity, billingFlowParams)?.isOk() ?: false
    }

    private fun processPurchases(purchases: List<Purchase>, isRestore: Boolean = false) {
        backgroundExecutor.execute {
            purchases.forEach { purchase ->
                if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
                    return@forEach
                }
                if (!decodedKey.isSignatureValid(purchase)) {
                    Timer("$TAG: Invalid signature on purchase. Check to make sure your public key is correct.")
                    return@forEach
                }
                // Grant entitlement to the user.
                val productDetails = productDetailsMap[purchase.products[0]]
                when (productDetails?.productType) {
                    BillingClient.ProductType.INAPP -> {
                        productOwned(purchase.getPurchaseInfo(), isRestore)
                    }

                    BillingClient.ProductType.SUBS -> {
                        subscriptionOwned(purchase.getPurchaseInfo(), isRestore)
                    }
                }

                when {
                    purchase.products.isConsumable() -> {
                        val result = consumePurchaseSync(purchase)
                        if (!result.isOk()) return@forEach
                    }

                    !purchase.isAcknowledged -> {
                        val result = acknowledgePurchaseSync(purchase)
                        if (!result.isOk()) return@forEach
                    }
                }
            }
        }
    }

    fun restorePurchase(onRestored: (isSuccess: Boolean) -> Unit) {
        refreshPurchasesAsync(onRestored)
    }

    private fun refreshPurchasesAsync(onRestored: ((isSuccess: Boolean) -> Unit)? = null) {
        billingClient?.queryPurchasesAsync(
            BillingClient.ProductType.INAPP.getQueryPurchasesParams()
        ) { inAppResult, inAppPurchases ->
            val inAppPurchasedList = when {
                inAppResult.isOk() -> inAppPurchases
                else -> emptyList()
            }

            billingClient?.queryPurchasesAsync(
                BillingClient.ProductType.SUBS.getQueryPurchasesParams()
            ) { subResult, subPurchases ->
                val subPurchasedList = when {
                    subResult.isOk() -> subPurchases
                    else -> emptyList()
                }

                val purchases = mutableListOf<Purchase>().apply {
                    addAll(inAppPurchasedList)
                    addAll(subPurchasedList)
                }

                val isSuccess =
                    purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                onRestored?.invoke(isSuccess)
                processPurchases(purchases, true)
            }
        }
    }

    fun unsubscribe(activity: Activity, productId: String) {
        try {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            val subscriptionUrl = ("http://play.google.com/store/account/subscriptions"
                    + "?package=" + activity.packageName
                    + "&sku=" + productId)
            intent.data = Uri.parse(subscriptionUrl)
            activity.startActivity(intent)
            activity.finish()
        } catch (e: Exception) {
            Timer("$TAG: Unsubscribing failed.")
        }
    }


    private fun productOwned(purchaseInfo: DataWrappers.PurchaseInfo, isRestore: Boolean) {
        handler.post {
            if (isRestore) {
                billingServiceListener?.onProductRestored(purchaseInfo)
            } else {
                billingServiceListener?.onProductPurchased(purchaseInfo)
            }
        }
    }

    private fun subscriptionOwned(purchaseInfo: DataWrappers.PurchaseInfo, isRestore: Boolean) {
        handler.post {
            if (isRestore) {
                billingServiceListener?.onProductRestored(purchaseInfo)
            } else {
                billingServiceListener?.onProductPurchased(purchaseInfo)
            }
        }
    }

    /**
     * If the state is PURCHASED, acknowledge the purchase if it hasn't been acknowledged yet.
     */
    @WorkerThread
    private fun acknowledgePurchaseSync(purchase: Purchase): BillingResult {
        val blockingQueue = ArrayBlockingQueue<BillingResult>(1)
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient?.acknowledgePurchase(params) { billingResult: BillingResult ->
            blockingQueue.add(billingResult)
        }
        return blockingQueue.take()
    }


    /**
     * If the state is PURCHASED, consume the purchase if it hasn't been acknowledged yet.
     */
    @WorkerThread
    private fun consumePurchaseSync(purchase: Purchase): BillingResult {
        val blockingQueue = ArrayBlockingQueue<BillingResult>(1)
        val params = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient?.consumeAsync(params) { billingResult: BillingResult, _: String ->
            blockingQueue.add(billingResult)
        }
        return blockingQueue.take()
    }

    fun getProductDetails() = productDetailsMap

    fun getProductDetail(productId: String) = productDetailsMap[productId]
}