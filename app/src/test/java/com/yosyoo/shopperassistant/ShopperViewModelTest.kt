package com.yosyoo.shopperassistant

import android.app.Application
import com.yosyoo.shopperassistant.barcode.BarcodeHistoryRepository
import com.yosyoo.shopperassistant.barcode.InvalidCode39ContentException
import com.yosyoo.shopperassistant.model.BarcodeHistoryItem
import com.yosyoo.shopperassistant.ui.Code39BarcodeGenerator
import com.yosyoo.shopperassistant.ui.GeneratedCode39
import com.yosyoo.shopperassistant.ui.ShopperViewModel
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import org.junit.Test

class ShopperViewModelTest {
    private val today = LocalDate.of(2026, 5, 4)
    private val now = Instant.parse("2026-05-04T10:15:30Z")

    @Test
    fun generateCode39FromManualInput_updatesGeneratedState() {
        val viewModel = viewModel()

        viewModel.onManualBarcodeInputChanged(" ab-12 ")
        viewModel.generateCode39FromManualInput()

        val state = viewModel.uiState.value
        expectEquals(" ab-12 ", state.manualBarcodeInput)
        expectEquals("AB-12", state.normalizedCode39Text)
        expectEquals(null, state.code39Error)
    }

    @Test
    fun generateCode39FromManualInput_storesValidationError() {
        val generator = FakeCode39BarcodeGenerator().apply {
            generateBlock = { throw InvalidCode39ContentException("该内容不能生成 Code39") }
        }
        val viewModel = viewModel(generator = generator)

        viewModel.onManualBarcodeInputChanged("苹果-123")
        viewModel.generateCode39FromManualInput()

        val state = viewModel.uiState.value
        expectEquals("苹果-123", state.manualBarcodeInput)
        expectEquals(null, state.normalizedCode39Text)
        expectEquals("该内容不能生成 Code39", state.code39Error)
    }

    @Test
    fun onBarcodeScanned_updatesManualInput() {
        val viewModel = viewModel()

        viewModel.onBarcodeScanned(" 6901234567890 ")

        val state = viewModel.uiState.value
        expectEquals("6901234567890", state.manualBarcodeInput)
        expectEquals(null, state.normalizedCode39Text)
        expectEquals(null, state.code39Error)
    }

    @Test
    fun onBarcodeScanned_ignoresDuplicateValue() {
        val viewModel = viewModel()

        viewModel.onManualBarcodeInputChanged("ABC-123")
        viewModel.generateCode39FromManualInput()
        viewModel.onBarcodeScanned("ABC-123")

        val state = viewModel.uiState.value
        expectEquals("ABC-123", state.manualBarcodeInput)
        expectEquals("ABC-123", state.normalizedCode39Text)
        expectEquals(null, state.code39Error)
    }

    @Test
    fun history_canSaveRestoreAndDeleteCurrentBarcode() {
        val historyStore = FakeBarcodeHistoryRepository()
        val viewModel = viewModel(historyStore = historyStore)

        viewModel.onManualBarcodeInputChanged("item-01")
        viewModel.generateCode39FromManualInput()

        expectTrue(viewModel.saveCurrentBarcodeToHistory())
        val savedItem = viewModel.uiState.value.barcodeHistory.single()
        expectEquals("history-id", savedItem.id)
        expectEquals("ITEM-01", savedItem.text)
        expectEquals(now, savedItem.savedAt)
        expectEquals(listOf(savedItem), historyStore.savedItems)

        viewModel.onManualBarcodeInputChanged("other")
        viewModel.restoreBarcodeHistory(savedItem.id)
        expectEquals("ITEM-01", viewModel.uiState.value.manualBarcodeInput)

        expectTrue(viewModel.deleteBarcodeHistory(savedItem.id))
        expectEquals(emptyList<BarcodeHistoryItem>(), viewModel.uiState.value.barcodeHistory)
        expectEquals(emptyList<BarcodeHistoryItem>(), historyStore.savedItems)
    }

    @Test
    fun expiry_updatesWhenProductionDateAndShelfLifeChange() {
        val viewModel = viewModel()

        viewModel.onProductionDateSelected(LocalDate.of(2026, 5, 1))
        viewModel.onShelfLifeDaysChanged("7天")

        val state = viewModel.uiState.value
        expectEquals("7", state.shelfLifeDaysText)
        expectEquals(null, state.expiryError)
        expectEquals(LocalDate.of(2026, 5, 1), state.expiryResult.productionDate)
        expectEquals(LocalDate.of(2026, 5, 7), state.expiryResult.expiryDate)
        expectEquals(3L, state.expiryResult.daysUntilExpiry)
    }

    private fun viewModel(
        historyStore: FakeBarcodeHistoryRepository = FakeBarcodeHistoryRepository(),
        generator: FakeCode39BarcodeGenerator = FakeCode39BarcodeGenerator(),
    ): ShopperViewModel {
        return ShopperViewModel(
            application = Application(),
            historyStore = historyStore,
            code39Generator = generator,
            todayProvider = { today },
            idProvider = { "history-id" },
            nowProvider = { now },
        )
    }

    private class FakeBarcodeHistoryRepository(
        private val initialItems: List<BarcodeHistoryItem> = emptyList(),
    ) : BarcodeHistoryRepository {
        var savedItems: List<BarcodeHistoryItem> = emptyList()
            private set

        override fun load(): List<BarcodeHistoryItem> = initialItems

        override fun save(items: List<BarcodeHistoryItem>) {
            savedItems = items
        }
    }

    private class FakeCode39BarcodeGenerator : Code39BarcodeGenerator {
        var generateBlock: (String) -> GeneratedCode39 = { value ->
            GeneratedCode39(
                normalizedText = value.trim().uppercase(Locale.US),
                bitmap = null,
            )
        }

        override fun generate(value: String): GeneratedCode39 = generateBlock(value)
    }
}
