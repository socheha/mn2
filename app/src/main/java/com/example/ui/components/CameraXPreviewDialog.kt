package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.entity.ItemEntity
import com.example.util.Formatters
import com.example.util.OfflineReceiptScanner
import com.example.util.ScannedReceiptItem
import com.example.util.ScannedReceiptResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraXPreviewDialog(
    databaseItems: List<ItemEntity> = emptyList(),
    onDismiss: () -> Unit,
    onImageCaptured: (Bitmap) -> Unit,
    onRealTimePopulateDraft: ((ScannedReceiptResult) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (!hasCameraPermission) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Izin Kamera Diperlukan",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aplikasi membutuhkan akses kamera untuk mengambil foto struk / nota secara real-time.",
                            fontSize = 13.sp,
                            color = Color.LightGray,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Berikan Izin Kamera")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("Batal")
                        }
                    }
                }
            } else {
                var isCapturing by remember { mutableStateOf(false) }
                var isFlashOn by remember { mutableStateOf(false) }
                var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }

                var cameraInstance by remember { mutableStateOf<Camera?>(null) }
                val imageCapture = remember { ImageCapture.Builder().build() }
                val executor = remember { Executors.newSingleThreadExecutor() }

                // ML Kit Real-time Text Recognition States & ImageAnalysis
                var detectedTextBlocks by remember { mutableStateOf<List<Rect>>(emptyList()) }
                var realTimeRawText by remember { mutableStateOf("") }
                var realTimeItems by remember { mutableStateOf<List<ScannedReceiptItem>>(emptyList()) }
                var frameWidth by remember { mutableStateOf(1080) }
                var frameHeight by remember { mutableStateOf(1920) }

                val textRecognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
                val imageAnalysis = remember {
                    ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                }

                DisposableEffect(Unit) {
                    onDispose {
                        textRecognizer.close()
                        executor.shutdown()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // CameraX Realtime Preview View
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                val cameraSelector = CameraSelector.Builder()
                                    .requireLensFacing(lensFacing)
                                    .build()

                                // ML Kit Real-time Frame Analyzer
                                imageAnalysis.setAnalyzer(executor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                                        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
                                        val w = if (isRotated) imageProxy.height else imageProxy.width
                                        val h = if (isRotated) imageProxy.width else imageProxy.height

                                        textRecognizer.process(inputImage)
                                            .addOnSuccessListener { visionText ->
                                                val boxes = visionText.textBlocks.flatMap { block ->
                                                    block.lines.mapNotNull { it.boundingBox }
                                                }
                                                val rawText = visionText.text
                                                val parsedItems = if (rawText.isNotBlank()) {
                                                    OfflineReceiptScanner.parseTextLinesToItems(rawText, databaseItems)
                                                } else emptyList()

                                                ContextCompat.getMainExecutor(ctx).execute {
                                                    frameWidth = w
                                                    frameHeight = h
                                                    detectedTextBlocks = boxes
                                                    realTimeRawText = rawText
                                                    realTimeItems = parsedItems
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraInstance = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture,
                                        imageAnalysis
                                    )
                                } catch (exc: Exception) {
                                    exc.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Bounding Box Canvas Overlay for ML Kit Text Recognition
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        if (frameWidth > 0 && frameHeight > 0) {
                            val scaleX = size.width / frameWidth.toFloat()
                            val scaleY = size.height / frameHeight.toFloat()

                            detectedTextBlocks.forEach { rect ->
                                val left = rect.left * scaleX
                                val top = rect.top * scaleY
                                val right = rect.right * scaleX
                                val bottom = rect.bottom * scaleY

                                // Draw translucent green fill
                                drawRect(
                                    color = Color(0x2800FF66),
                                    topLeft = Offset(left, top),
                                    size = Size(right - left, bottom - top)
                                )
                                // Draw bright green stroke
                                drawRect(
                                    color = Color(0xFF00FF66),
                                    topLeft = Offset(left, top),
                                    size = Size(right - left, bottom - top),
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }

                    // Visual Bounding Box Target Frame Guide for Scanning Receipts / Barcodes
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .height(420.dp)
                            .align(Alignment.Center)
                            .border(2.dp, Color(0xFF00E676), RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.08f))
                    ) {
                        // Corner brackets for alignment
                        Text(
                            text = "┌                                  ┐",
                            color = Color(0xFF00FF66),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 4.dp)
                        )
                        Text(
                            text = "└                                  ┘",
                            color = Color(0xFF00FF66),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 4.dp)
                        )
                    }

                    // Header Control Bar & Live Status
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 36.dp, start = 16.dp, end = 16.dp)
                            .align(Alignment.TopCenter),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                            }

                            Surface(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF00FF66),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "ML Kit Real-Time AI Camera",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    isFlashOn = !isFlashOn
                                    cameraInstance?.cameraControl?.enableTorch(isFlashOn)
                                },
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                            ) {
                                Icon(
                                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Senter",
                                    tint = if (isFlashOn) Color.Yellow else Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real-time Text Recognition Detection Badge
                        Surface(
                            color = if (realTimeItems.isNotEmpty()) Color(0xEE1B5E20) else Color(0xCC212121),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (realTimeItems.isNotEmpty()) {
                                    "✨ Real-time: ${realTimeItems.size} Item Terdeteksi (${detectedTextBlocks.size} Blok Teks)"
                                } else if (detectedTextBlocks.isNotEmpty()) {
                                    "🔍 Mendeteksi Teks Nota / Barcode (${detectedTextBlocks.size} Frasa)..."
                                } else {
                                    "📷 Arahkan kamera ke Nota / Struk Pembelian"
                                },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Bottom Action Controls (Shutter + Bridge to Draft List Button)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
                            .align(Alignment.BottomCenter),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Bridge Button: Populate Real-time ML Kit Result Directly into Draft List
                        if (realTimeItems.isNotEmpty() && onRealTimePopulateDraft != null) {
                            Button(
                                onClick = {
                                    val supplier = OfflineReceiptScanner.extractSupplierOffline(realTimeRawText)
                                    val faktur = OfflineReceiptScanner.extractFakturOffline(realTimeRawText)
                                    val scannedResult = ScannedReceiptResult(
                                        namaSupplier = supplier,
                                        nomorFaktur = faktur,
                                        tanggal = Formatters.getCurrentDateFormatted(),
                                        totalNota = realTimeItems.sumOf { it.totalHarga },
                                        items = realTimeItems,
                                        rawTextResponse = realTimeRawText
                                    )
                                    onRealTimePopulateDraft.invoke(scannedResult)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .padding(bottom = 12.dp)
                                    .testTag("btn_populate_realtime_draft")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "⚡ Masukkan ${realTimeItems.size} Item Terdeteksi ke Draf",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Shutter & Camera Toggle Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(32.dp)
                        ) {
                            // Flip Camera
                            IconButton(
                                onClick = {
                                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                        CameraSelector.LENS_FACING_FRONT
                                    } else {
                                        CameraSelector.LENS_FACING_BACK
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.65f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FlipCameraAndroid,
                                    contentDescription = "Ganti Kamera",
                                    tint = Color.White
                                )
                            }

                            // Capture Shutter Button
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    .testTag("btn_capture_camerax"),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isCapturing) return@IconButton
                                        isCapturing = true

                                        imageCapture.takePicture(
                                            executor,
                                            object : ImageCapture.OnImageCapturedCallback() {
                                                override fun onCaptureSuccess(image: ImageProxy) {
                                                    val bitmap = imageProxyToBitmap(image)
                                                    image.close()
                                                    ContextCompat.getMainExecutor(context).execute {
                                                        isCapturing = false
                                                        if (bitmap != null) {
                                                            onImageCaptured(bitmap)
                                                        }
                                                    }
                                                }

                                                override fun onError(exception: ImageCaptureException) {
                                                    exception.printStackTrace()
                                                    ContextCompat.getMainExecutor(context).execute {
                                                        isCapturing = false
                                                    }
                                                }
                                            }
                                        )
                                    },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (isCapturing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(36.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(48.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
    return try {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees != 0 && bitmap != null) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
