package com.yosyoo.shopperassistant.expiry

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class ExpiryStatus {
    Valid,
    Expired,
}

data class ExpiryResult(
    val productionDate: LocalDate,
    val shelfLifeDays: Int,
    val expiryDate: LocalDate,
    val today: LocalDate,
    val daysUntilExpiry: Long,
    val status: ExpiryStatus,
    val isProductionDateInFuture: Boolean,
) {
    val isExpired: Boolean = status == ExpiryStatus.Expired
}

object ExpiryCalculator {
    fun check(
        productionDate: LocalDate,
        shelfLifeDays: Int,
        today: LocalDate = LocalDate.now(),
    ): ExpiryResult {
        require(shelfLifeDays > 0) { "保质期天数必须大于 0" }

        val expiryDate = productionDate.plusDays((shelfLifeDays - 1).toLong())
        val daysUntilExpiry = ChronoUnit.DAYS.between(today, expiryDate)
        val isExpired = !today.isBefore(expiryDate)

        return ExpiryResult(
            productionDate = productionDate,
            shelfLifeDays = shelfLifeDays,
            expiryDate = expiryDate,
            today = today,
            daysUntilExpiry = daysUntilExpiry,
            status = if (isExpired) ExpiryStatus.Expired else ExpiryStatus.Valid,
            isProductionDateInFuture = productionDate.isAfter(today),
        )
    }
}
