package com.ji.adshelper.biling

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.android.billingclient.api.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors
import kotlin.math.min


object BillingService : PurchasesUpdatedListener, BillingClientStateListener,
    SkuDetailsResponseListener {

    private const val TAG = "BillingService"
    private const val RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
    private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 mins
    private const val SKU_DETAILS_REQUERY_TIME = 1000L * 60L * 60L * 4L // 4 hours

    private var isInit = false
    private var billingClient: BillingClient? = null
    private var nonConsumableSkus: List<String> = emptyList()
    private var consumableSkus: List<String> = emptyList()
    private var subscriptionSkus: List<String> = emptyList()
    private var publicKey: String? = null
    private var enableDebug: Boolean = false

    private val handler = Handler(Looper.getMainLooper())
    private val backgroundExecutor by lazy { Executors.newSingleThreadExecutor() }

    // how long before the data source tries to reconnect to Google play
    private var reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS

    // when was the last successful SkuDetailsResponse?
    private var skuDetailsResponseTime = -SKU_DETAILS_REQUERY_TIME

    private val skuDetailsMap = mutableMapOf<String, MutableLiveData<SkuDetails>>()

    private var billingFlowListener: BillingFlowListener? = null
    var billingRestoredListener: BillingRestoredListener? = null

    fun init(
        app: Application,
        nonConsumableSkus: List<String>,
        consumableSkus: List<String>,
        subscriptionSkus: List<String>,
        publicKey: String?,
        enableDebug: Boolean
    ) {
        if (isInit) return
        isInit = true

        BillingService.nonConsumableSkus = nonConsumableSkus
        BillingService.consumableSkus = consumableSkus
        BillingService.subscriptionSkus = subscriptionSkus
        BillingService.publicKey = publicKey
        BillingService.enableDebug = enableDebug

        billingClient = BillingClient.newBuilder(app.applicationContext).setListener(this)
            .enablePendingPurchases().build()
        billingClient?.startConnection(this)

        initializeSkuDetailLiveData()
    }

    private fun initializeSkuDetailLiveData() {
        val allSkus = mutableListOf<String>().apply {
            addAll(nonConsumableSkus)
            addAll(consumableSkus)
            addAll(subscriptionSkus)
        }
        for (sku in allSkus) {
            skuDetailsMap[sku] = object : MutableLiveData<SkuDetails>() {
                override fun onActive() {
                    if (SystemClock.elapsedRealtime() - skuDetailsResponseTime > SKU_DETAILS_REQUERY_TIME) {
                        skuDetailsResponseTime = SystemClock.elapsedRealtime()
                        log("Skus not fresh, re-querying ...")
                        querySkuDetailsAsync()
                    }
                }
            }
        }
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        log("onBillingSetupFinished: $responseCode $debugMessage")

        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                // The billing client is ready. You can query purchases here.
                // This doesn't mean that your app is set up correctly in the console -- it just
                // means that you have a connection to the Billing service.
                reconnectMilliseconds = RECONNECT_TIMER_START_MILLISECONDS
                querySkuDetailsAsync()
                refreshPurchasesAsync()
            }
            else -> retryBillingServiceConnectionWithExponentialBackoff()
        }
    }

    override fun onBillingServiceDisconnected() {
        retryBillingServiceConnectionWithExponentialBackoff()
    }

    /**
     * Retries the billing service connection with exponential backoff, maxing out at the time
     * specified by RECONNECT_TIMER_MAX_TIME_MILLISECONDS.
     */
    private fun retryBillingServiceConnectionWithExponentialBackoff() {
        handler.postDelayed({ billingClient?.startConnection(this) }, reconnectMilliseconds)
        reconnectMilliseconds =
            min(reconnectMilliseconds * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS)
    }

    /**
     * Calls the billing client functions to query sku details for both the inapp and subscription
     * SKUs. SKU details are useful for displaying item names and price lists to the user, and are
     * required to make a purchase.
     */
    private fun querySkuDetailsAsync() {
        val inAppSkus = mutableListOf<String>().apply {
            addAll(nonConsumableSkus)
            addAll(consumableSkus)
        }

        if (inAppSkus.isNotEmpty()) {
            val params = SkuDetailsParams.newBuilder()
                .setType(BillingClient.SkuType.INAPP)
                .setSkusList(inAppSkus)
                .build()
            billingClient?.querySkuDetailsAsync(params, this)
        }
        if (subscriptionSkus.isNotEmpty()) {
            val params = SkuDetailsParams.newBuilder()
                .setType(BillingClient.SkuType.SUBS)
                .setSkusList(subscriptionSkus)
                .build()
            billingClient?.querySkuDetailsAsync(params, this)
        }
    }

    /**
     * Receives the result from [.querySkuDetailsAsync]}.
     *
     *
     * Store the SkuDetails and post them in the [.skuDetailsLiveDataMap]. This allows other
     * parts of the app to use the [SkuDetails] to show SKU information and make purchases.
     */
    override fun onSkuDetailsResponse(
        billingResult: BillingResult,
        skuDetailsList: List<SkuDetails>?
    ) {
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        when (responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                skuDetailsResponseTime = SystemClock.elapsedRealtime()
                log("onSkuDetailsResponse: $responseCode $debugMessage")
                skuDetailsList?.forEach { skuDetails ->
                    skuDetailsMap[skuDetails.sku]?.postValue(skuDetails)
                }
            }
            else -> skuDetailsResponseTime = -SKU_DETAILS_REQUERY_TIME
        }
    }

    fun refreshPurchasesAsync() {
        billingClient?.queryPurchasesAsync(BillingClient.SkuType.INAPP) { inAppResult, inAppPurchases ->
            val inAppPurchasedList = when {
                inAppResult.isOk() -> inAppPurchases
                else -> emptyList()
            }

            billingClient?.queryPurchasesAsync(BillingClient.SkuType.SUBS) { subResult, subPurchases ->
                val subPurchasedList = when {
                    subResult.isOk() -> subPurchases
                    else -> emptyList()
                }

                val purchases = mutableListOf<Purchase>().apply {
                    addAll(inAppPurchasedList)
                    addAll(subPurchasedList)
                }

                processPurchases(purchases, false)
            }
        }
        log("Refreshing purchases started.")
    }

    fun getPurchases(
        @BillingClient.SkuType type: String,
        vararg skuIds: String
    ): MutableLiveData<List<Purchase>> {
        val data = MutableLiveData<List<Purchase>>()

        billingClient?.queryPurchasesAsync(type) { result, purchases ->
            val resultPurchase = mutableListOf<Purchase>()
            when {
                result.isOk() -> {
                    purchases.forEach { purchase ->
                        purchase.skus.forEach sub@{
                            if (skuIds.contains(it)) {
                                resultPurchase.add(purchase)
                                return@sub
                            }
                        }
                    }
                }
            }

            data.postValue(resultPurchase)
        }

        return data
    }

    fun launchBillingFlow(
        activity: Activity,
        sku: String,
        billingFlowListener: BillingFlowListener?
    ): Boolean {
        BillingService.billingFlowListener = billingFlowListener
        val skuDetails = skuDetailsMap[sku]?.value ?: return false
        val params = BillingFlowParams.newBuilder().setSkuDetails(skuDetails).build()
        val result = billingClient?.launchBillingFlow(activity, params) ?: return false
        return result.isOk()
    }

    fun unsubscribe(activity: Activity, sku: String) {
        try {
            val intent = Intent()
            intent.action = Intent.ACTION_VIEW
            val subscriptionUrl = ("http://play.google.com/store/account/subscriptions"
                    + "?package=" + activity.packageName
                    + "&sku=" + sku)
            intent.data = Uri.parse(subscriptionUrl)
            activity.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.isOk() && purchases != null) {
            processPurchases(purchases, true)
        } else {
            val isUserCancelled =
                billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED
            handler.post {
                billingFlowListener?.onPurchaseError(isUserCancelled)
                billingFlowListener = null
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>, isFromBillingFlow: Boolean) {
        backgroundExecutor.execute {
            val purchasedSkus = mutableSetOf<String>()

            purchases.forEach { purchase ->
                if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) {
                    return@forEach
                }
                if (!isSignatureValid(purchase)) {
                    log("Invalid signature on purchase. Check to make sure your public key is correct.")
                    return@forEach
                }

                when {
                    purchase.isConsumable() -> {
                        val result = consumePurchaseSync(purchase)
                        if (!result.isOk()) return@forEach
                    }
                    !purchase.isAcknowledged -> {
                        val result = acknowledgePurchaseSync(purchase)
                        if (!result.isOk()) return@forEach
                    }
                }

                purchasedSkus.addAll(purchase.skus)
            }

            handler.post {
                if (isFromBillingFlow) {
                    billingFlowListener?.onPurchased(purchasedSkus)
                    billingFlowListener = null
                } else {
                    billingRestoredListener?.onPurchaseRestored(purchasedSkus)
                }
            }
        }
    }

    private fun Purchase.isConsumable(): Boolean {
        val hasConsumableSku = skus.any { consumableSkus.contains(it) }
        val hasNonConsumableSku =
            skus.any { nonConsumableSkus.contains(it) || subscriptionSkus.contains(it) }
        return hasConsumableSku && !hasNonConsumableSku
    }

    private fun isSignatureValid(purchase: Purchase): Boolean {
        val key = publicKey ?: return true
        return Security.verifyPurchase(purchase.originalJson, purchase.signature, key)
    }

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

    private fun log(message: String) {
        if (enableDebug) {
            Log.d(TAG, message)
        }
    }

    private fun BillingResult.isOk(): Boolean {
        return this.responseCode == BillingClient.BillingResponseCode.OK
    }

    fun getSkuTitle(sku: String): LiveData<String> {
        val skuDetailsLiveData = skuDetailsMap[sku]!!
        return Transformations.map(skuDetailsLiveData) { obj: SkuDetails -> obj.title }
    }

    fun getSkuPrice(sku: String?): LiveData<String> {
        val skuDetailsLiveData = skuDetailsMap[sku]!!
        return Transformations.map(skuDetailsLiveData) { obj: SkuDetails -> obj.price }
    }

    fun getSkuDescription(sku: String?): LiveData<String> {
        val skuDetailsLiveData = skuDetailsMap[sku]!!
        return Transformations.map(skuDetailsLiveData) { obj: SkuDetails -> obj.description }
    }

    fun getSkuDetailsMap() = skuDetailsMap

    interface BillingFlowListener {
        @MainThread
        fun onPurchased(skus: Set<String>)

        @MainThread
        fun onPurchaseError(isUserCancelled: Boolean)
    }

    interface BillingRestoredListener {
        @MainThread
        fun onPurchaseRestored(skus: Set<String>)
    }
}