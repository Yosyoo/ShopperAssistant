package com.yosyoo.shopperassistant.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.yosyoo.shopperassistant.barcode.BarcodeImageStorage
import com.yosyoo.shopperassistant.ui.ShopperUiState
import com.yosyoo.shopperassistant.ui.ShopperViewModel
import com.yosyoo.shopperassistant.ui.components.ErrorSurface
import com.yosyoo.shopperassistant.ui.components.SectionHeader
import com.yosyoo.shopperassistant.ui.components.ShopperCardShape
import com.yosyoo.shopperassistant.ui.components.ToolScreen
import com.yosyoo.shopperassistant.ui.components.BarcodeCameraPreview
import kotlinx.coroutines.launch

@Composable
internal fun ScanGenerateRoute(
    uiState: ShopperUiState,
    viewModel: ShopperViewModel,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    ScanGenerateScreen(
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
                if (!saved) {
                    snackbarHostState.showSnackbar("暂无可保存的条码")
                }
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
        modifier = modifier,
    )
}

@Composable
internal fun ScanGenerateScreen(
    uiState: ShopperUiState,
    onManualInputChanged: (String) -> Unit,
    onGenerateClicked: () -> Unit,
    onBarcodeScanned: (rawValue: String) -> Unit,
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

    ToolScreen(modifier = modifier) {
        item {
            SectionHeader(
                icon = Icons.Outlined.QrCodeScanner,
                title = "扫码",
            )
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                shape = ShopperCardShape,
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
                            onBarcodeScanned = { rawValue, _ -> onBarcodeScanned(rawValue) },
                            modifier = Modifier.fillMaxSize(),
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp),
                            shape = ShopperCardShape,
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
        shape = ShopperCardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                icon = Icons.Outlined.Keyboard,
                title = "条码内容",
            )
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
        else -> Unit
    }
}
