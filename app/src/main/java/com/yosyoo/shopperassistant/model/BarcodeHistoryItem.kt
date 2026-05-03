package com.yosyoo.shopperassistant.model

import java.time.Instant

data class BarcodeHistoryItem(
    val id: String,
    val text: String,
    val format: String,
    val savedAt: Instant,
)
