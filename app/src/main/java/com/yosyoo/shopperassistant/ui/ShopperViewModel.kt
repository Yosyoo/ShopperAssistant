package com.yosyoo.shopperassistant.ui

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import com.yosyoo.shopperassistant.barcode.BarcodeHistoryRepository
import com.yosyoo.shopperassistant.barcode.BarcodeHistoryStore
import com.yosyoo.shopperassistant.barcode.Code39Generator
import com.yosyoo.shopperassistant.barcode.InvalidCode39ContentException
import com.yosyoo.shopperassistant.expiry.ExpiryCalculator
import com.yosyoo.shopperassistant.expiry.ExpiryResult
import com.yosyoo.shopperassistant.model.BarcodeHistoryItem
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class ShopperDestination {
    Scan,
    Expiry,
}

data class ShopperUiState(
    val selectedDestination: ShopperDestination = ShopperDestination.Scan,
    val manualBarcodeInput: String = "",
    val code39Bitmap: Bitmap? = null,
    val code39Error: String? = null,
    val normalizedCode39Text: String? = null,
    val barcodeHistory: List<BarcodeHistoryItem> = emptyList(),
    val productionDate: LocalDate = LocalDate.now(),
    val shelfLifeDaysText: String = "30",
    val expiryResult: ExpiryResult = ExpiryCalculator.check(
        productionDate = LocalDate.now(),
        shelfLifeDays = 30,
        today = LocalDate.now(),
    ),
    val expiryError: String? = null,
)

internal data class GeneratedCode39(
    val normalizedText: String,
    val bitmap: Bitmap?,
)

internal fun interface Code39BarcodeGenerator {
    fun generate(value: String): GeneratedCode39
}

private object ProductionCode39BarcodeGenerator : Code39BarcodeGenerator {
    override fun generate(value: String): GeneratedCode39 {
        val normalizedText = Code39Generator.normalizeOrThrow(value)
        return GeneratedCode39(
            normalizedText = normalizedText,
            bitmap = Code39Generator.generate(normalizedText),
        )
    }
}

class ShopperViewModel internal constructor(
    application: Application,
    private val historyStore: BarcodeHistoryRepository,
    private val code39Generator: Code39BarcodeGenerator,
    private val todayProvider: () -> LocalDate,
    private val idProvider: () -> String,
    private val nowProvider: () -> Instant,
) : AndroidViewModel(application) {
    constructor(application: Application) : this(
        application = application,
        historyStore = BarcodeHistoryStore(application),
        code39Generator = ProductionCode39BarcodeGenerator,
        todayProvider = { LocalDate.now() },
        idProvider = { UUID.randomUUID().toString() },
        nowProvider = { Instant.now() },
    )

    private val _uiState = MutableStateFlow(
        todayProvider().let { today ->
            ShopperUiState(
                productionDate = today,
                expiryResult = ExpiryCalculator.check(
                    productionDate = today,
                    shelfLifeDays = 30,
                    today = today,
                ),
                barcodeHistory = historyStore.load(),
            )
        },
    )
    val uiState: StateFlow<ShopperUiState> = _uiState.asStateFlow()

    fun selectDestination(destination: ShopperDestination) {
        _uiState.update { it.copy(selectedDestination = destination) }
    }

    fun onManualBarcodeInputChanged(value: String) {
        _uiState.update {
            it.copy(
                manualBarcodeInput = value,
                code39Error = null,
                code39Bitmap = null,
                normalizedCode39Text = null,
            )
        }
    }

    fun onBarcodeScanned(rawValue: String) {
        val cleanValue = rawValue.trim()
        if (cleanValue.isEmpty()) return
        if (cleanValue == _uiState.value.manualBarcodeInput.trim()) return
        _uiState.update {
            it.copy(
                manualBarcodeInput = cleanValue,
                code39Bitmap = null,
                code39Error = null,
                normalizedCode39Text = null,
            )
        }
    }

    fun generateCode39FromManualInput() {
        generateCode39(value = _uiState.value.manualBarcodeInput)
    }

    fun saveCurrentBarcodeToHistory(): Boolean {
        val current = _uiState.value
        val normalizedText = current.normalizedCode39Text ?: return false
        val historyItem = BarcodeHistoryItem(
            id = idProvider(),
            text = normalizedText,
            format = "条形码",
            savedAt = nowProvider(),
        )
        val nextHistory = listOf(historyItem)
            .plus(current.barcodeHistory.filterNot { it.text == normalizedText })
            .take(MaxHistoryItems)
        historyStore.save(nextHistory)
        _uiState.update { it.copy(barcodeHistory = nextHistory) }
        return true
    }

    fun restoreBarcodeHistory(itemId: String) {
        val item = _uiState.value.barcodeHistory.firstOrNull { it.id == itemId } ?: return
        _uiState.update {
            it.copy(
                manualBarcodeInput = item.text,
                code39Bitmap = null,
                code39Error = null,
                normalizedCode39Text = null,
            )
        }
    }

    fun deleteBarcodeHistory(itemId: String): Boolean {
        val current = _uiState.value
        val nextHistory = current.barcodeHistory.filterNot { it.id == itemId }
        if (nextHistory.size == current.barcodeHistory.size) return false
        historyStore.save(nextHistory)
        _uiState.update { it.copy(barcodeHistory = nextHistory) }
        return true
    }

    fun onProductionDateSelected(date: LocalDate) {
        _uiState.update { current ->
            current.copy(productionDate = date).withRecalculatedExpiry()
        }
    }

    fun onShelfLifeDaysChanged(value: String) {
        val digitsOnly = value.filter { it.isDigit() }.take(5)
        _uiState.update { current ->
            current.copy(shelfLifeDaysText = digitsOnly).withRecalculatedExpiry()
        }
    }

    private fun generateCode39(value: String) {
        try {
            val generated = code39Generator.generate(value)
            _uiState.update { current ->
                current.copy(
                    manualBarcodeInput = value,
                    code39Bitmap = generated.bitmap,
                    code39Error = null,
                    normalizedCode39Text = generated.normalizedText,
                )
            }
        } catch (exception: InvalidCode39ContentException) {
            _uiState.update { current ->
                current.copy(
                    manualBarcodeInput = value,
                    code39Bitmap = null,
                    code39Error = exception.message ?: "该内容不能生成 Code39",
                    normalizedCode39Text = null,
                )
            }
        }
    }

    private fun ShopperUiState.withRecalculatedExpiry(): ShopperUiState {
        val shelfLifeDays = shelfLifeDaysText.toIntOrNull()
        if (shelfLifeDays == null) {
            return copy(expiryError = "请输入保质期天数")
        }

        return try {
            copy(
                expiryResult = ExpiryCalculator.check(
                    productionDate = productionDate,
                    shelfLifeDays = shelfLifeDays,
                    today = todayProvider(),
                ),
                expiryError = null,
            )
        } catch (exception: IllegalArgumentException) {
            copy(expiryError = exception.message ?: "请输入有效的保质期天数")
        }
    }

    private companion object {
        const val MaxHistoryItems = 30
    }
}
