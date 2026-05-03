package com.yosyoo.shopperassistant.ui.components

import android.os.Handler
import android.os.Looper
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeCameraPreview(
    torchEnabled: Boolean,
    onTorchAvailabilityChanged: (Boolean) -> Unit,
    onBarcodeScanned: (rawValue: String, format: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val barcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_CODE_128,
                )
                .build(),
        )
    }
    val processingFrame = remember { AtomicBoolean(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(camera, torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    DisposableEffect(lifecycleOwner, previewView) {
        var cameraProvider: ProcessCameraProvider? = null
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener(
            {
                val provider = cameraProviderFuture.get()
                cameraProvider = provider

                val preview = Preview.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .build()
                    .also { it.surfaceProvider = previewView.surfaceProvider }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            analyzeImageProxy(
                                imageProxy = imageProxy,
                                processingFrame = processingFrame,
                                scanner = barcodeScanner,
                                mainHandler = mainHandler,
                                onBarcodeScanned = onBarcodeScanned,
                            )
                        }
                    }

                provider.unbindAll()
                val boundCamera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                )
                camera = boundCamera
                onTorchAvailabilityChanged(boundCamera.cameraInfo.hasFlashUnit())
            },
            mainExecutor,
        )

        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize(),
        )
        ScanFrame(
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@OptIn(ExperimentalGetImage::class)
private fun analyzeImageProxy(
    imageProxy: ImageProxy,
    processingFrame: AtomicBoolean,
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    mainHandler: Handler,
    onBarcodeScanned: (rawValue: String, format: String) -> Unit,
) {
    if (!processingFrame.compareAndSet(false, true)) {
        imageProxy.close()
        return
    }

    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        processingFrame.set(false)
        imageProxy.close()
        return
    }

    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            val barcode = barcodes.firstOrNull { it.rawValue?.isNotBlank() == true }
            val rawValue = barcode?.rawValue
            if (barcode != null && rawValue != null) {
                mainHandler.post {
                    onBarcodeScanned(rawValue, barcode.format.displayName())
                }
            }
        }
        .addOnCompleteListener {
            processingFrame.set(false)
            imageProxy.close()
        }
}

private fun Int.displayName(): String {
    return when (this) {
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        Barcode.FORMAT_CODE_39 -> "Code39"
        Barcode.FORMAT_CODE_128 -> "Code128"
        else -> "条码"
    }
}

@Composable
private fun ScanFrame(
    modifier: Modifier = Modifier,
) {
    val outlineColor = MaterialTheme.colorScheme.primary
    val dimColor = Color.Black.copy(alpha = 0.24f)

    Box(
        modifier = modifier
            .width(280.dp)
            .height(180.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                color = dimColor,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
            )
            drawRoundRect(
                color = outlineColor,
                topLeft = Offset.Zero,
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                style = Stroke(
                    width = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(32f, 20f), 0f),
                ),
            )
            drawLine(
                color = outlineColor.copy(alpha = 0.8f),
                start = Offset(size.width * 0.14f, size.height / 2),
                end = Offset(size.width * 0.86f, size.height / 2),
                strokeWidth = 2.dp.toPx(),
            )
        }
    }
}
