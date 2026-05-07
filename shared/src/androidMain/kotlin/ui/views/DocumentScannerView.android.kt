package ui.views

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import io.github.aakira.napier.Napier
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private val documentScannerExecutor = Executors.newSingleThreadExecutor()

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun DocumentScannerView(
    onScannedText: (text: String) -> Unit,
    modifier: Modifier,
) {
    val cameraPermissionState = rememberMultiplePermissionsState(listOf(android.Manifest.permission.CAMERA))
    if (cameraPermissionState.allPermissionsGranted) {
        DocumentScannerWithGrantedPermission(onScannedText, modifier)
    } else {
        LaunchedEffect(Unit) {
            Napier.d("Get Camera Permission")
            cameraPermissionState.launchMultiplePermissionRequest()
        }
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun DocumentScannerWithGrantedPermission(
    onScannedText: (text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    val textAccepted = remember { mutableStateOf(false) }
    val analysisInFlight = remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            recognizer.close()
        }
    }

    LaunchedEffect(Unit) {
        cameraProvider = suspendCoroutine<ProcessCameraProvider> { continuation ->
            ProcessCameraProvider.getInstance(context).also { provider ->
                provider.addListener({ continuation.resume(provider.get()) }, documentScannerExecutor)
            }
        }
        cameraProvider?.unbindAll()

        val imageAnalysis = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(documentScannerExecutor) { imageProxy: ImageProxy ->
            if (textAccepted.value || analysisInFlight.value) {
                imageProxy.close()
                return@setAnalyzer
            }
            val image = imageProxy.image
            if (image == null) {
                imageProxy.close()
                return@setAnalyzer
            }
            analysisInFlight.value = true
            recognizer.process(InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees))
                .addOnCompleteListener { result ->
                    imageProxy.close()
                    analysisInFlight.value = false
                    if (result.isSuccessful) {
                        val scannedText = result.result.text.trim()
                        val meaningfulLineCount = scannedText.lines().count { it.trim().length >= 3 }
                        val hasDocumentLikeData = listOf(
                            "name",
                            "birth",
                            "document",
                            "passport",
                            "expiry",
                            "issued",
                            "nationality",
                            "country",
                            "<<",
                        ).any { scannedText.contains(it, ignoreCase = true) }
                        if (
                            scannedText.length >= 35 &&
                            meaningfulLineCount >= 3 &&
                            hasDocumentLikeData &&
                            !textAccepted.value
                        ) {
                            textAccepted.value = true
                            onScannedText(scannedText)
                        }
                    } else {
                        Napier.w("DocumentScannerView: OCR failed", throwable = result.exception)
                    }
                }
        }

        cameraProvider?.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            imageAnalysis,
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(modifier = modifier) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
    }
}
