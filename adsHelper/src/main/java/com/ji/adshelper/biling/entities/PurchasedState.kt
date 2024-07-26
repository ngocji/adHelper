package com.ji.adshelper.biling.entities

import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState

enum class PurchasedState {
    UNSPECIFIED_STATE,
    PURCHASED,
    PENDING;

    companion object {
        fun from(@PurchaseState state: Int): PurchasedState {
            return when (state) {
                Purchase.PurchaseState.PURCHASED -> PURCHASED
                Purchase.PurchaseState.PENDING -> PENDING
                else -> UNSPECIFIED_STATE
            }
        }
    }
}