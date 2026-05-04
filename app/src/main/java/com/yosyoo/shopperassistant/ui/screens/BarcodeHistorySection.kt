package com.yosyoo.shopperassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yosyoo.shopperassistant.model.BarcodeHistoryItem
import com.yosyoo.shopperassistant.ui.components.SectionHeader
import com.yosyoo.shopperassistant.ui.components.ShopperCardShape
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val HistoryDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")

@Composable
internal fun BarcodeHistorySection(
    history: List<BarcodeHistoryItem>,
    onHistoryItemClicked: (String) -> Unit,
    onDeleteHistoryItemClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionHeader(
            icon = Icons.Outlined.History,
            title = "历史记录",
            trailing = {
                Text(
                    text = "${history.size} 条",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        if (history.isEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ShopperCardShape,
            ) {
                Text(
                    text = "还没有保存的条码。",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            history.forEach { item ->
                BarcodeHistoryRow(
                    item = item,
                    onUseClicked = { onHistoryItemClicked(item.id) },
                    onDeleteClicked = { onDeleteHistoryItemClicked(item.id) },
                )
            }
        }
    }
}

@Composable
private fun BarcodeHistoryRow(
    item: BarcodeHistoryItem,
    onUseClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = ShopperCardShape,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${item.format} · ${item.savedAt.formatHistoryTime()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onUseClicked) {
                Text("使用")
            }
            IconButton(onClick = onDeleteClicked) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "删除历史记录",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

private fun Instant.formatHistoryTime(): String {
    return atZone(ZoneId.systemDefault()).format(HistoryDateTimeFormatter)
}
