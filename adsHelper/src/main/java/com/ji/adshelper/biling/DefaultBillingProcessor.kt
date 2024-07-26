package com.ji.adshelper.biling

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.widget.Toast
import com.android.billingclient.api.PendingPurchasesParams
import com.ji.adshelper.biling.entities.DataWrappers
import com.ji.adshelper.biling.entities.ProductType
import com.ji.adshelper.biling.extension.toMap
import com.ji.adshelper.biling.listener.BillingServiceListener
import com.ji.adshelper.biling.listener.IPurchaseLiveEvent
import com.ji.adshelper.biling.listener.OnRestoreListener
import com.ji.adshelper.biling.utils.BillingUtils
import java.util.Calendar

@Suppress("MemberVisibilityCanBePrivate")
object DefaultBillingProcessor : BillingServiceListener {
    private lateinit var iapHelper: IapHelper
    private var onRestoreListener: OnRestoreListener? = null
    val eventPurchaseSuccess = IPurchaseLiveEvent<DataWrappers.PurchaseInfo>()

    fun init(
        app: Application,
        nonConsumableSkus: List<String> = emptyList(),
        consumableSkus: List<String> = emptyList(),
        subscriptionSkus: List<String> = emptyList(),
        enabledPendingPurchase: Boolean = true,
        decodedKey: String?
    ) {
        iapHelper = IapHelperImpl(app.applicationContext)

        BillingService.init(
            app,
            nonConsumableSkus = nonConsumableSkus,
            consumableSkus = consumableSkus,
            subscriptionSkus = subscriptionSkus,
            decodedKey = decodedKey,
            pendingPurchasesParams = createPendingPurchasesParams(enabledPendingPurchase)
        )

        BillingService.setListener(this)
    }

    fun purchase(activity: Activity, productId: String) {
        if (!iapHelper.isItemPurchased(productId)) {
            val launchSuccessfully =
                BillingService.launchBillingFlow(activity, productId)
            if (!launchSuccessfully) {
                iapHelper.showPurchaseErrorMessage()
            }
        }
    }

    fun getPurchased(): DataWrappers.ProductDetails? {
        val id =
            BillingService.nonConsumableSkus.find { iapHelper.isItemPurchased(it) }.let { sku ->
                sku ?: BillingService.subscriptionSkus.find { iapHelper.isItemPurchased(it) }
            } ?: return null

        return BillingService.getProductDetail(id)?.toMap()
    }

    fun queryPurchasedInfo(
        type: String,
        productId: String,
        action: (DataWrappers.PurchaseInfo) -> Unit
    ) {
        val prType = if (type == "subs") ProductType.SUBSCRIPTION else ProductType.INAPP
        BillingService.queryPurchasedInfo(prType, productId, action)
    }

    fun isPurchased(): Boolean {
        return BillingService.nonConsumableSkus.any { iapHelper.isItemPurchased(it) } || BillingService.subscriptionSkus.any {
            iapHelper.isItemPurchased(
                it
            )
        }
    }

    fun openManager(context: Context) {
        val sku: String = getPurchasedProductId()
        if (TextUtils.isEmpty(sku)) return
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/account/subscriptions?sku=" + sku + "&package=" + context.getPackageName())
                )
            )
        } catch (e: Exception) {
            Toast.makeText(context, "Error open manager purchased!", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    fun getProductDetails() = BillingService.getProductDetails().map { entry ->
        entry.key to entry.value?.toMap()
    }.toMap()


    fun getPurchasedProductId(): String {
        val subId = BillingService.subscriptionSkus.find { iapHelper.isItemPurchased(it) }
        if (subId != null) return subId

        return BillingService.nonConsumableSkus.find { iapHelper.isItemPurchased(it) } ?: ""
    }

    fun unSubscription(activity: Activity) {
        BillingService.unsubscribe(activity, "");
    }

    fun setOnRestorePurchaseListener(listener: OnRestoreListener?) {
        this.onRestoreListener = listener
    }

    fun restorePurchase() {
        BillingService.restorePurchase { isSuccess ->
            onRestoreListener?.onRestored(isSuccess)
        }
    }

    fun createPendingPurchasesParams(enabledPendingPurchase: Boolean): PendingPurchasesParams {
        return PendingPurchasesParams.newBuilder()
            .apply {
                if (enabledPendingPurchase) {
                    enablePrepaidPlans()
                    enableOneTimeProducts()
                }
            }
            .build()
    }

    fun savePrefIfNeed(purchaseInfo: DataWrappers.PurchaseInfo) {
        // save pref for non consume and subs
        if (BillingService.nonConsumableSkus.any { it == purchaseInfo.productId } || BillingService.subscriptionSkus.any { it == purchaseInfo.productId }) {
            iapHelper.onProductPurchased(purchaseInfo.productId)
        }
    }

    fun getExpiredTime(info: DataWrappers.PurchaseInfo?): Long {
        if (info == null || info.productId.isEmpty()) return 0
        val productDetails = getProductDetails()[info.productId] ?: return 0

        val purchaseTime = info.purchaseTime
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = purchaseTime
        calendar.add(Calendar.DATE, productDetails.period)
        return calendar.timeInMillis
    }

    fun getExpiredTimeFormatted(info: DataWrappers.PurchaseInfo?): String? {
        return getExpiredTime(info).takeIf { it > 0 }?.let { BillingUtils.formatPurchaseDate(it) }
    }

    override fun onPurchaseError(isUserCancelled: Boolean) {
        if (!isUserCancelled) {
            iapHelper.showPurchaseErrorMessage()
        }
    }

    override fun onProductPurchased(purchaseInfo: DataWrappers.PurchaseInfo) {
        savePrefIfNeed(purchaseInfo)
        iapHelper.showPurchaseSuccessMessage()
        eventPurchaseSuccess.postValue(purchaseInfo)
    }

    override fun onProductRestored(purchaseInfo: DataWrappers.PurchaseInfo) {
        val revokedNonConsumePrdIds =
            BillingService.nonConsumableSkus.filter { purchaseInfo.productId != it }
        val revokedSubPrdIds =
            BillingService.subscriptionSkus.filter { purchaseInfo.productId != it }

        revokedNonConsumePrdIds.forEach { iapHelper.onProductRevoked(it) }
        revokedSubPrdIds.forEach { iapHelper.onProductRevoked(it) }

        savePrefIfNeed(purchaseInfo)
        eventPurchaseSuccess.postValue(purchaseInfo)
    }
}