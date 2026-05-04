package com.yosyoo.shopperassistant

import com.yosyoo.shopperassistant.barcode.BarcodeHistoryJson
import com.yosyoo.shopperassistant.model.BarcodeHistoryItem
import java.time.Instant
import org.junit.Test

class BarcodeHistoryStoreTest {
    @Test
    fun json_roundTripsEmptyList() {
        val json = BarcodeHistoryJson.encode(emptyList())

        expectEquals(emptyList<BarcodeHistoryItem>(), BarcodeHistoryJson.decode(json))
        expectEquals(emptyList<BarcodeHistoryItem>(), BarcodeHistoryJson.decode(null))
    }

    @Test
    fun json_preservesBarcodeTextWithSpecialCharacters() {
        val item = BarcodeHistoryItem(
            id = "id-1",
            text = "AB-12.$/+% \"quoted\" \\ slash",
            format = "格式/测试",
            savedAt = Instant.parse("2026-05-04T08:00:00Z"),
        )

        val decoded = BarcodeHistoryJson.decode(BarcodeHistoryJson.encode(listOf(item)))

        expectEquals(listOf(item), decoded)
    }

    @Test
    fun json_returnsEmptyListWhenDataIsCorrupted() {
        val decoded = BarcodeHistoryJson.decode("{not valid json")

        expectEquals(emptyList<BarcodeHistoryItem>(), decoded)
    }
}
