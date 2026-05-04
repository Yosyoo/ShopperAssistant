package com.yosyoo.shopperassistant.barcode

import android.content.Context
import com.yosyoo.shopperassistant.model.BarcodeHistoryItem
import java.time.Instant
import org.json.JSONArray
import org.json.JSONObject

internal interface BarcodeHistoryRepository {
    fun load(): List<BarcodeHistoryItem>
    fun save(items: List<BarcodeHistoryItem>)
}

internal class BarcodeHistoryStore(context: Context) : BarcodeHistoryRepository {
    private val preferences = context.getSharedPreferences("barcode_history", Context.MODE_PRIVATE)

    override fun load(): List<BarcodeHistoryItem> {
        return BarcodeHistoryJson.decode(preferences.getString(KeyItems, null))
    }

    override fun save(items: List<BarcodeHistoryItem>) {
        preferences.edit()
            .putString(KeyItems, BarcodeHistoryJson.encode(items))
            .apply()
    }

    private companion object {
        const val KeyItems = "items"
    }
}

internal object BarcodeHistoryJson {
    fun decode(rawJson: String?): List<BarcodeHistoryItem> {
        if (rawJson == null) return emptyList()
        return runCatching {
            val items = JSONArray(rawJson)
            buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val id = item.optString("id")
                    val text = item.optString("text")
                    if (id.isBlank() || text.isBlank()) continue
                    add(
                        BarcodeHistoryItem(
                            id = id,
                            text = text,
                            format = item.optString("format", "手动输入"),
                            savedAt = Instant.ofEpochMilli(item.optLong("savedAt", 0L)),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encode(items: List<BarcodeHistoryItem>): String {
        val json = JSONArray()
        items.forEach { item ->
            json.put(
                JSONObject()
                    .put("id", item.id)
                    .put("text", item.text)
                    .put("format", item.format)
                    .put("savedAt", item.savedAt.toEpochMilli()),
            )
        }
        return json.toString()
    }
}
