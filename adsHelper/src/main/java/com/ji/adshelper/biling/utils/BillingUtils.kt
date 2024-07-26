package com.ji.adshelper.biling.utils

import com.ji.adshelper.biling.entities.DataWrappers
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Locale

object BillingUtils {
    @JvmStatic
    fun getPerMonthPrice(yearDetails: DataWrappers.ProductDetails?): String {
        yearDetails ?: return ""
        val pricePerMonth = yearDetails.priceAmount?.let { if (it != 0.0) it / 12 else 0 }
        val format = NumberFormat.getCurrencyInstance()
        format.maximumFractionDigits = 0
        format.currency = Currency.getInstance(yearDetails.priceCurrencyCode)
        return format.format(pricePerMonth)
    }

    @JvmStatic
    fun getYearPriceByMonth(monthDetails: DataWrappers.ProductDetails?): String {
        monthDetails ?: return ""
        val price = monthDetails.priceAmount?.let { if (it != 0.0) it * 12 else 0 }
        val format = NumberFormat.getCurrencyInstance()
        format.maximumFractionDigits = 0
        format.currency = Currency.getInstance(monthDetails.priceCurrencyCode)
        return format.format(price)
    }

    @JvmStatic
    fun calculateDiscount(
        details1: DataWrappers.ProductDetails?,
        details2: DataWrappers.ProductDetails?
    ): Float {
        if (details1 == null || details2 == null) return 0f
        val price1 = details1.priceAmount ?: 0.0
        val price2 = details2.priceAmount ?: 0.0
        return if (price2 != 0.0) ({
            (price1 / price2) * 100f
        }).toFloat() else {
            0f
        }
    }

    fun formatPurchaseDate(purchasedTime: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = purchasedTime
        try {
            return SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
        } catch (e: Exception) {
            e.printStackTrace()
            return ""
        }
    }
}