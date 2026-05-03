package com.yosyoo.shopperassistant.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yosyoo.shopperassistant.barcode.BarcodeImageStorage
import com.yosyoo.shopperassistant.expiry.ExpiryResult
import com.yosyoo.shopperassistant.model.BarcodeHistoryItem
import com.yosyoo.shopperassistant.ui.ShopperDestination
import com.yosyoo.shopperassistant.ui.ShopperUiState
import com.yosyoo.shopperassistant.ui.ShopperViewModel
import com.yosyoo.shopperassistant.ui.components.BarcodeCameraPreview
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private val ChineseDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
private val HistoryDateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopperAssistantApp(
    viewModel: ShopperViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "超市助手",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = uiState.selectedDestination == ShopperDestination.Scan,
                    onClick = { viewModel.selectDestination(ShopperDestination.Scan) },
                    icon = { Icon(Icons.Outlined.QrCodeScanner, contentDescription = null) },
                    label = { Text("扫码生成") },
                )
                NavigationBarItem(
                    selected = uiState.selectedDestination == ShopperDestination.Expiry,
                    onClick = { viewModel.selectDestination(ShopperDestination.Expiry) },
                    icon = { Icon(Icons.Outlined.Event, contentDescription = null) },
                    label = { Text("保质期检查") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .consumeWindowInsets(innerPadding)

        when (uiState.selectedDestination) {
            ShopperDestination.Scan -> ScanGenerateScreen(
                uiState = uiState,
                onManualInputChanged = viewModel::onManualBarcodeInputChanged,
                onGenerateClicked = viewModel::generateCode39FromManualInput,
                onBarcodeScanned = viewModel::onBarcodeScanned,
                onSaveClicked = {
                    val bitmap = uiState.code39Bitmap
                    if (bitmap != null) {
                        val barcodeText = uiState.normalizedCode39Text ?: uiState.manualBarcodeInput
                        coroutineScope.launch {
                            runCatching {
                                BarcodeImageStorage.saveToPictures(context, bitmap, barcodeText)
                            }.onSuccess {
                                snackbarHostState.showSnackbar("已保存到图片/超市助手")
                            }.onFailure { throwable ->
                                snackbarHostState.showSnackbar(throwable.message ?: "保存失败")
                            }
                        }
                    }
                },
                onShareClicked = {
                    val bitmap = uiState.code39Bitmap
                    if (bitmap != null) {
                        val barcodeText = uiState.normalizedCode39Text ?: uiState.manualBarcodeInput
                        runCatching {
                            val shareIntent = BarcodeImageStorage.createShareIntent(
                                context = context,
                                bitmap = bitmap,
                                barcodeText = barcodeText,
                            )
                            context.startActivity(Intent.createChooser(shareIntent, "分享 Code39 条形码"))
                        }.onFailure { throwable ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(throwable.message ?: "分享失败")
                            }
                        }
                    }
                },
                onSaveHistoryClicked = {
                    coroutineScope.launch {
                        val saved = viewModel.saveCurrentBarcodeToHistory()
                        snackbarHostState.showSnackbar(
                            if (saved) "已保存到历史记录" else "暂无可保存的条码",
                        )
                    }
                },
                onHistoryItemClicked = viewModel::restoreBarcodeHistory,
                onDeleteHistoryItemClicked = { itemId ->
                    coroutineScope.launch {
                        val deleted = viewModel.deleteBarcodeHistory(itemId)
                        snackbarHostState.showSnackbar(
                            if (deleted) "已删除历史记录" else "未找到历史记录",
                        )
                    }
                },
                modifier = contentModifier,
            )
            ShopperDestination.Expiry -> ExpiryCheckScreen(
                uiState = uiState,
                onProductionDateSelected = viewModel::onProductionDateSelected,
                onShelfLifeDaysChanged = viewModel::onShelfLifeDaysChanged,
                modifier = contentModifier,
            )
        }
    }
}

@Composable
private fun ScanGenerateScreen(
    uiState: ShopperUiState,
    onManualInputChanged: (String) -> Unit,
    onGenerateClicked: () -> Unit,
    onBarcodeScanned: (rawValue: String, format: String) -> Unit,
    onSaveClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onSaveHistoryClicked: () -> Unit,
    onHistoryItemClicked: (String) -> Unit,
    onDeleteHistoryItemClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionPrompted by rememberSaveable { mutableStateOf(false) }
    var torchEnabled by rememberSaveable { mutableStateOf(false) }
    var torchAvailable by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        cameraPermissionGranted = granted
        permissionPrompted = true
    }

    LaunchedEffect(cameraPermissionGranted) {
        if (!cameraPermissionGranted && !permissionPrompted) {
            permissionPrompted = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp,
            ) {
                Box(Modifier.fillMaxSize()) {
                    if (cameraPermissionGranted) {
                        BarcodeCameraPreview(
                            torchEnabled = torchEnabled,
                            onTorchAvailabilityChanged = { available ->
                                torchAvailable = available
                                if (!available) torchEnabled = false
                            },
                            onBarcodeScanned = onBarcodeScanned,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        ) {
                            IconButton(
                                enabled = torchAvailable,
                                onClick = { torchEnabled = !torchEnabled },
                            ) {
                                Icon(
                                    imageVector = if (torchEnabled) {
                                        Icons.Outlined.FlashOff
                                    } else {
                                        Icons.Outlined.FlashOn
                                    },
                                    contentDescription = if (torchEnabled) "关闭闪光灯" else "打开闪光灯",
                                )
                            }
                        }
                    } else {
                        CameraPermissionContent(
                            onGrantClicked = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                        )
                    }
                }
            }
        }

        item {
            ManualBarcodeInput(
                value = uiState.manualBarcodeInput,
                error = uiState.code39Error,
                onValueChanged = onManualInputChanged,
                onGenerateClicked = onGenerateClicked,
            )
        }

        uiState.lastScanResult?.let { scanResult ->
            item {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AssistChip(
                                onClick = {},
                                label = { Text(scanResult.format) },
                            )
                            Text(
                                text = "刚刚扫到",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = scanResult.rawValue,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        item {
            BarcodeResultSection(
                bitmap = uiState.code39Bitmap,
                normalizedText = uiState.normalizedCode39Text,
                error = uiState.code39Error,
                onSaveClicked = onSaveClicked,
                onShareClicked = onShareClicked,
                onSaveHistoryClicked = onSaveHistoryClicked,
            )
        }

        item {
            BarcodeHistorySection(
                history = uiState.barcodeHistory,
                onHistoryItemClicked = onHistoryItemClicked,
                onDeleteHistoryItemClicked = onDeleteHistoryItemClicked,
            )
        }
    }
}

@Composable
private fun CameraPermissionContent(
    onGrantClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "需要相机权限才能扫码",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "也可以在下方手动输入条码内容生成 Code39。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onGrantClicked) {
            Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("打开相机")
        }
    }
}

@Composable
private fun ManualBarcodeInput(
    value: String,
    error: String?,
    onValueChanged: (String) -> Unit,
    onGenerateClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("条码内容") },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { message -> { Text(message) } },
                trailingIcon = {
                    Icon(Icons.Outlined.Keyboard, contentDescription = null)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onGenerateClicked() },
                ),
            )
            Button(
                onClick = onGenerateClicked,
                enabled = value.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("生成条形码")
            }
        }
    }
}

@Composable
private fun BarcodeResultSection(
    bitmap: Bitmap?,
    normalizedText: String?,
    error: String?,
    onSaveClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onSaveHistoryClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        bitmap != null && normalizedText != null -> BarcodePreviewCard(
            bitmap = bitmap,
            normalizedText = normalizedText,
            onSaveClicked = onSaveClicked,
            onShareClicked = onShareClicked,
            onSaveHistoryClicked = onSaveHistoryClicked,
            modifier = modifier,
        )
        error != null -> ErrorSurface(
            text = error,
            modifier = modifier.fillMaxWidth(),
        )
        else -> EmptyBarcodeState(modifier = modifier.fillMaxWidth())
    }
}

@Composable
private fun BarcodePreviewCard(
    bitmap: Bitmap,
    normalizedText: String,
    onSaveClicked: () -> Unit,
    onShareClicked: () -> Unit,
    onSaveHistoryClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Code39：$normalizedText",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Code39 条形码",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.White)
                        .padding(12.dp),
                    contentScale = ContentScale.Fit,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = onSaveClicked,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("保存图片")
                }
                Button(
                    onClick = onShareClicked,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("分享")
                }
            }
            OutlinedButton(
                onClick = onSaveHistoryClicked,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.History, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("保存到历史记录")
            }
        }
    }
}

@Composable
private fun BarcodeHistorySection(
    history: List<BarcodeHistoryItem>,
    onHistoryItemClicked: (String) -> Unit,
    onDeleteHistoryItemClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "历史记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${history.size} 条",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (history.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
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
        shape = RoundedCornerShape(8.dp),
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

@Composable
private fun EmptyBarcodeState(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.QrCodeScanner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "扫码或手动输入后，这里会显示可保存的 Code39 PNG。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpiryCheckScreen(
    uiState: ShopperUiState,
    onProductionDateSelected: (LocalDate) -> Unit,
    onShelfLifeDaysChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "生产日期",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
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
        shape = RoundedCornerShape(8.dp),
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
            Text("生产日期：${result.productionDate.format(ChineseDateFormatter)}")
            Text("保质期：${result.shelfLifeDays} 天")
            Text("到期日：${result.expiryDate.format(ChineseDateFormatter)}")
            Text("今天：${result.today.format(ChineseDateFormatter)}")
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

@Composable
private fun ErrorSurface(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun LocalDate.toDatePickerMillis(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toLocalDateFromDatePickerMillis(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}

private fun Instant.formatHistoryTime(): String {
    return atZone(ZoneId.systemDefault()).format(HistoryDateTimeFormatter)
}
