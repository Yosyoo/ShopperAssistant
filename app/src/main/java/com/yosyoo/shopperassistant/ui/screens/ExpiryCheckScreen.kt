package com.yosyoo.shopperassistant.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yosyoo.shopperassistant.expiry.ExpiryResult
import com.yosyoo.shopperassistant.ui.ShopperUiState
import com.yosyoo.shopperassistant.ui.components.ErrorSurface
import com.yosyoo.shopperassistant.ui.components.ExpiryInfoRow
import com.yosyoo.shopperassistant.ui.components.SectionHeader
import com.yosyoo.shopperassistant.ui.components.ShopperCardShape
import com.yosyoo.shopperassistant.ui.components.ToolScreen
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

private val ChineseDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExpiryCheckScreen(
    uiState: ShopperUiState,
    onProductionDateSelected: (LocalDate) -> Unit,
    onShelfLifeDaysChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    ToolScreen(modifier = modifier) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = ShopperCardShape,
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    SectionHeader(
                        icon = Icons.Outlined.Event,
                        title = "保质期信息",
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.CalendarToday, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(uiState.productionDate.format(ChineseDateFormatter))
                    }
                    OutlinedTextField(
                        value = uiState.shelfLifeDaysText,
                        onValueChange = onShelfLifeDaysChanged,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("保质期天数") },
                        singleLine = true,
                        isError = uiState.expiryError != null,
                        supportingText = {
                            Text(uiState.expiryError ?: "到期日当天按已过期处理")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                    )
                }
            }
        }

        item {
            uiState.expiryError?.let { error ->
                ErrorSurface(
                    text = error,
                    modifier = Modifier.fillMaxWidth(),
                )
            } ?: ExpiryResultCard(
                result = uiState.expiryResult,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.productionDate.toDatePickerMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onProductionDateSelected(millis.toLocalDateFromDatePickerMillis())
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun ExpiryResultCard(
    result: ExpiryResult,
    modifier: Modifier = Modifier,
) {
    val containerColor = if (result.isExpired) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (result.isExpired) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    val title = if (result.isExpired) "已过期" else "未过期"
    val subtitle = when {
        result.daysUntilExpiry == 0L -> "今天到期，按规则已过期"
        result.daysUntilExpiry < 0L -> "已过期 ${result.daysUntilExpiry.absoluteValue} 天"
        else -> "距离到期还有 ${result.daysUntilExpiry} 天"
    }

    Surface(
        modifier = modifier,
        shape = ShopperCardShape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = if (result.isExpired) {
                        Icons.Outlined.ErrorOutline
                    } else {
                        Icons.Outlined.CheckCircle
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            HorizontalDivider(color = contentColor.copy(alpha = 0.24f))
            ExpiryInfoRow("生产日期", result.productionDate.format(ChineseDateFormatter))
            ExpiryInfoRow("保质期", "${result.shelfLifeDays} 天")
            ExpiryInfoRow("到期日", result.expiryDate.format(ChineseDateFormatter))
            ExpiryInfoRow("今天", result.today.format(ChineseDateFormatter))
            if (result.isProductionDateInFuture) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Text("生产日期在未来，请核对输入。")
                }
            }
        }
    }
}

private fun LocalDate.toDatePickerMillis(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toLocalDateFromDatePickerMillis(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}
