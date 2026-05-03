package com.yosyoo.shopperassistant.model

import java.time.Instant

data class ScanResult(
    val rawValue: String,
    val format: String,
    val scannedAt: Instant,
)
