package com.yosyoo.shopperassistant

import com.yosyoo.shopperassistant.expiry.ExpiryCalculator
import org.junit.Test
import java.time.LocalDate

class ExpiryCalculatorTest {
    private val today: LocalDate = LocalDate.of(2026, 5, 3)

    @Test
    fun check_marksExpiredWhenTodayEqualsExpiryDate() {
        val result = ExpiryCalculator.check(
            productionDate = LocalDate.of(2026, 4, 27),
            shelfLifeDays = 7,
            today = today,
        )

        expectTrue(result.isExpired)
        expectEquals(today, result.expiryDate)
        expectEquals(0L, result.daysUntilExpiry)
    }

    @Test
    fun check_marksExpiredWhenExpiryDateWasYesterday() {
        val result = ExpiryCalculator.check(
            productionDate = LocalDate.of(2026, 4, 26),
            shelfLifeDays = 7,
            today = today,
        )

        expectTrue(result.isExpired)
        expectEquals(-1L, result.daysUntilExpiry)
    }

    @Test
    fun check_marksValidWhenExpiryDateIsTomorrow() {
        val result = ExpiryCalculator.check(
            productionDate = LocalDate.of(2026, 4, 28),
            shelfLifeDays = 7,
            today = today,
        )

        expectFalse(result.isExpired)
        expectEquals(1L, result.daysUntilExpiry)
    }

    @Test
    fun check_countsProductionDateAsFirstShelfLifeDay() {
        val result = ExpiryCalculator.check(
            productionDate = LocalDate.of(2026, 5, 2),
            shelfLifeDays = 3,
            today = LocalDate.of(2026, 5, 4),
        )

        expectTrue(result.isExpired)
        expectEquals(LocalDate.of(2026, 5, 4), result.expiryDate)
        expectEquals(0L, result.daysUntilExpiry)
    }

    @Test
    fun check_allowsFutureProductionDateButFlagsIt() {
        val result = ExpiryCalculator.check(
            productionDate = LocalDate.of(2026, 5, 4),
            shelfLifeDays = 10,
            today = today,
        )

        expectFalse(result.isExpired)
        expectTrue(result.isProductionDateInFuture)
        expectEquals(LocalDate.of(2026, 5, 13), result.expiryDate)
    }

    @Test
    fun check_rejectsInvalidShelfLifeDays() {
        expectThrows<IllegalArgumentException> {
            ExpiryCalculator.check(
                productionDate = today,
                shelfLifeDays = 0,
                today = today,
            )
        }
    }
}
