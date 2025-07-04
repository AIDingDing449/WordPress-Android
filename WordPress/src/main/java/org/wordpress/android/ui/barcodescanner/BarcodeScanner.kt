package org.wordpress.android.ui.barcodescanner

import android.content.res.Configuration
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.wordpress.android.ui.compose.theme.AppThemeM3
import androidx.camera.core.Preview as CameraPreview

private const val TIMEOUT_MS = 15_000L

@Composable
fun BarcodeScanner(
    codeScanner: CodeScanner,
    onScannedResult: CodeScannerCallback
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }
    val startTime = System.nanoTime()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { context ->
                val previewView = PreviewView(context)
                val preview = CameraPreview.Builder().build()
                preview.surfaceProvider = previewView.surfaceProvider
                val selector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
                val imageAnalysis = ImageAnalysis.Builder()
                    .setResolutionSelector(ResolutionSelector.Builder()
                        .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                        .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(
                                previewView.width,
                                previewView.height
                            ),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    ).build())
                    .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    // This is called repeatedly as the scanner looks for a QR code, so we can use it
                    // to detect when the scan has taken too long and time out. This is important
                    // because some Samsung devices fail to detect the QR code, so we can timeout
                    // and let them know they can use their camera app to scan it instead.
                    val endTime = System.nanoTime()
                    val elapsedTimeMs = (endTime - startTime) / 1_000_000
                    if (elapsedTimeMs > TIMEOUT_MS) {
                        onScannedResult.run(
                            CodeScannerStatus.Failure(
                                error = "ScanTimeout",
                                type = CodeScanningErrorType.ScanTimeout
                            )
                        )
                        return@setAnalyzer
                    }
                    val callback = object : CodeScannerCallback {
                        override fun run(status: CodeScannerStatus?) {
                            status?.let { onScannedResult.run(it) }
                        }
                    }
                    codeScanner.startScan(imageProxy, callback)
                }
                try {
                    cameraProviderFuture.get().bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                } catch (e: IllegalStateException) {
                    onScannedResult.run(CodeScannerStatus.Failure(
                        e.message
                            ?: "Illegal state exception while binding camera provider to lifecycle",
                        CodeScanningErrorType.Other(e)
                    ))
                } catch (e: IllegalArgumentException) {
                    onScannedResult.run(CodeScannerStatus.Failure(
                        e.message
                            ?: "Illegal argument exception while binding camera provider to lifecycle",
                        CodeScanningErrorType.Other(e)
                    ))
                }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

class DummyCodeScanner : CodeScanner {
    override fun startScan(imageProxy: ImageProxy, callback: CodeScannerCallback) {
        callback.run(CodeScannerStatus.Success("", GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCA))
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BarcodeScannerScreenPreview() {
    AppThemeM3 {
        BarcodeScanner(codeScanner = DummyCodeScanner(), onScannedResult = object : CodeScannerCallback {
            override fun run(status: CodeScannerStatus?) {
                // no-ops
            }
        })
    }
}
