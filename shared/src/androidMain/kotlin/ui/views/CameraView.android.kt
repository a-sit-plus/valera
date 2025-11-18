package ui.views

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.github.aakira.napier.Napier
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

// Modified from https://github.com/JetBrains/compose-multiplatform/blob/master/examples/imageviewer/shared/src/androidMain/kotlin/example/imageviewer/view/CameraView.android.kt
// Modified from https://developer.android.com/training/camerax/analyze


private val executor = Executors.newSingleThreadExecutor()

@OptIn(ExperimentalPermissionsApi::class)
@Composable
actual fun CameraView(
    onFoundPayload: (text: String) -> Unit,
    modifier: Modifier,
) {
    val cameraPermissionState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.CAMERA,
        )
    )
    if (cameraPermissionState.allPermissionsGranted) {
        CameraWithGrantedPermission(
            onFoundPayload,
            modifier = modifier,
        )
    } else {
        LaunchedEffect(Unit) {
            Napier.d("Get Camera Permission")
            cameraPermissionState.launchMultiplePermissionRequest()
        }
    }

}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraWithGrantedPermission(
    onFoundPayload: (text: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val foundQrCode = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val preview = Preview.Builder().build()
    val previewView = remember { PreviewView(context) }
    val imageCapture: ImageCapture = remember { ImageCapture.Builder().build() }
    var isFrontCamera by rememberSaveable { mutableStateOf(false) }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

    val cameraSelector = remember(isFrontCamera) {
        val lensFacing =
            if (isFrontCamera) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
        CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
    }
    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
        }
    }

    LaunchedEffect(isFrontCamera) {
        cameraProvider = suspendCoroutine<ProcessCameraProvider> { continuation ->
            ProcessCameraProvider.getInstance(context).also { cameraProvider ->
                cameraProvider.addListener({
                    continuation.resume(cameraProvider.get())
                }, executor)
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
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(executor, { imageProxy: ImageProxy ->
            imageProxy.image?.let { image ->
                val inputImage =
                    InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                val scanner = BarcodeScanning.getClient()
                scanner.process(inputImage)
                    .addOnCompleteListener {
                        imageProxy.close()
                        if (it.isSuccessful && !foundQrCode.value) {
                            for (barcode in it.result as List<Barcode>) {

                                val payload = barcode.rawValue.toString()
                                foundQrCode.value = true
                                onFoundPayload(payload)
                                break
                            }
                        } else {
                            Napier.w("CameraWithGrantedPermission: error", throwable = it.exception)
                        }
                    }
            }
        })

        cameraProvider?.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageAnalysis,
            imageCapture
        )
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }

    Box(modifier = modifier.pointerInput(isFrontCamera) {
        detectHorizontalDragGestures { change, dragAmount ->
            if (dragAmount.absoluteValue > 50.0) {
                isFrontCamera = !isFrontCamera
            }
        }
    }) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
    }
}

