package com.shirsh.flutter_doc_scanner

import android.Manifest
import android.annotation.SuppressLint // Explicitly import for @SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
// import android.graphics.Bitmap // Only needed if you explicitly handle Bitmap manipulation
// import android.graphics.Matrix // Only needed if you explicitly handle Matrix transformations
import android.graphics.PointF
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size // Explicitly import Size
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector // Explicitly import CameraSelector
import androidx.camera.core.ImageAnalysis // Explicitly import ImageAnalysis
import androidx.camera.core.ImageCapture // Explicitly import ImageCapture
import androidx.camera.core.ImageCaptureException // Explicitly import ImageCaptureException
import androidx.camera.core.ImageProxy // Explicitly import ImageProxy
import androidx.camera.core.Preview // Explicitly import Preview
import androidx.camera.lifecycle.ProcessCameraProvider // Explicitly import ProcessCameraProvider
import androidx.camera.view.PreviewView // Explicitly import PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage // Explicitly import InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner // Explicitly import GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions // Explicitly import GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning // Explicitly import GmsDocumentScanning
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

// Your main Activity for scanning
class YourCurrentScannerActivity : AppCompatActivity() { // Or DocumentScannerActivity

    private lateinit var previewView: PreviewView
    private lateinit var messageTextView: TextView
    private lateinit var shutterButton: ImageButton
    private lateinit var manualButton: Button
    private lateinit var autoCaptureButton: Button
    private lateinit var documentOverlayView: DocumentOverlayView

    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private var isAutoCaptureMode = true
    private var lastDocumentDetectionTime: Long = 0
    private var lastDetectedCorners: List<PointF>? = null
    private val AUTO_CAPTURE_STABLE_DURATION_MS = 1000L // 1 second stability

    // ML Kit Document Scanner
    private lateinit var documentScanner: GmsDocumentScanner

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        const val SCANNED_IMAGE_PATHS_KEY = "scanned_image_paths"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_document_scanner) // Ensure it's all lowercase

        previewView = findViewById(R.id.camera_preview_view)
        messageTextView = findViewById(R.id.message_text_view)
        shutterButton = findViewById(R.id.shutter_button)
        manualButton = findViewById(R.id.manual_button)
        autoCaptureButton = findViewById(R.id.auto_capture_button)
        documentOverlayView = findViewById(R.id.document_overlay_view)

        // Initialize ML Kit Document Scanner
        val options = GmsDocumentScannerOptions.Builder()
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_BASE)
            .build()
        documentScanner = GmsDocumentScanning.getClient(options)

        // Set initial UI state
        updateUIMessage("Looking for document...")
        setAutoCaptureToggle(true)

        shutterButton.setOnClickListener {
            if (!isAutoCaptureMode) {
                takePhoto()
            } else {
                Toast.makeText(this, "Switch to Manual mode to take photo.", Toast.LENGTH_SHORT).show()
            }
        }

        manualButton.setOnClickListener {
            setAutoCaptureToggle(false)
            updateUIMessage("Position document in frame")
            // When switching to manual, clear the overlay. Use a placeholder size like Size(1,1)
            // if you don't have the actual image size at this point, as it's not used when corners are null.
            documentOverlayView.setDocumentCorners(null, previewView, Size(1,1))
        }

        autoCaptureButton.setOnClickListener {
            setAutoCaptureToggle(true)
            updateUIMessage("Looking for document...")
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish() // Close activity if permissions are not granted
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalyzer { corners, imageSize ->
                        // Callback from the ImageAnalyzer when document corners are detected
                        runOnUiThread {
                            if (isAutoCaptureMode) {
                                if (corners != null && corners.size == 4) {
                                    documentOverlayView.setDocumentCorners(corners, previewView, imageSize)
                                    val currentTime = System.currentTimeMillis()
                                    if (lastDetectedCorners == null || !areCornersSimilar(lastDetectedCorners!!, corners)) {
                                        // Reset stability timer if corners change significantly
                                        lastDocumentDetectionTime = currentTime
                                        lastDetectedCorners = corners
                                        updateUIMessage("Capturing... hold steady")
                                    } else if (currentTime - lastDocumentDetectionTime >= AUTO_CAPTURE_STABLE_DURATION_MS) {
                                        // Auto-capture if stable for long enough
                                        takePhoto()
                                        updateUIMessage("Processing document...") // Indicate processing
                                    }
                                } else {
                                    // No document detected or not 4 corners
                                    documentOverlayView.setDocumentCorners(null, previewView, Size(1,1)) // Clear overlay
                                    updateUIMessage("Looking for document...")
                                    lastDocumentDetectionTime = 0 // Reset timer
                                    lastDetectedCorners = null
                                }
                            }
                        }
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider?.unbindAll()

                // Bind use cases to camera
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e("ScannerActivity", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // Simple check for corner similarity (you can improve this)
    private fun areCornersSimilar(corners1: List<PointF>, corners2: List<PointF>, tolerance: Float = 10f): Boolean {
        if (corners1.size != 4 || corners2.size != 4) return false
        for (i in 0 until 4) {
            val dist = Math.sqrt(
                Math.pow((corners1[i].x - corners2[i].x).toDouble(), 2.0) +
                        Math.pow((corners1[i].y - corners2[i].y).toDouble(), 2.0)
            )
            if (dist > tolerance) return false
        }
        return true
    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: return // Get a reference to the readable ImageCapture use case

        // Disable UI during capture to prevent multiple captures
        shutterButton.isEnabled = false
        manualButton.isEnabled = false
        autoCaptureButton.isEnabled = false
        updateUIMessage("Scanning... hold steady") // Keep this message during capture

        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("ScannerActivity", "Photo capture failed: ${exc.message}", exc)
                    runOnUiThread {
                        Toast.makeText(baseContext, "Photo capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                        // Re-enable UI on error
                        shutterButton.isEnabled = true
                        manualButton.isEnabled = true
                        autoCaptureButton.isEnabled = true
                        updateUIMessage(if(isAutoCaptureMode) "Looking for document..." else "Position document in frame")
                    }
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri ?: Uri.fromFile(photoFile)
                    Log.d("ScannerActivity", "Photo capture succeeded: $savedUri")
                    // Now, process the captured image with ML Kit Document Scanner
                    processCapturedImage(savedUri)
                }
            })
    }

    private fun processCapturedImage(uri: Uri) {
        updateUIMessage("Extracting information...") // UI for extraction state

        documentScanner.scanDocument(this, uri)
            .addOnSuccessListener { result ->
                val scannedImageUris = mutableListOf<String>()
                // ML Kit Document Scanner can return multiple pages or just one
                result.pages.forEach { page ->
                    scannedImageUris.add(page.imageUri.toString())
                }
                // If you chose PDF format, you'd get result.pdf.uri
                // scannedImageUris.add(result.pdf?.uri?.toString() ?: "")

                val resultIntent = Intent()
                resultIntent.putStringArrayListExtra(SCANNED_IMAGE_PATHS_KEY, ArrayList(scannedImageUris))
                setResult(RESULT_OK, resultIntent)
                finish() // Close activity and return results
            }
            .addOnFailureListener { e ->
                Log.e("ScannerActivity", "ML Kit Document Scanning failed: ${e.message}", e)
                Toast.makeText(this, "Document scanning failed: ${e.message}", Toast.LENGTH_LONG).show()
                // Re-enable UI and go back to scanning
                runOnUiThread {
                    shutterButton.isEnabled = true
                    manualButton.isEnabled = true
                    autoCaptureButton.isEnabled = true
                    updateUIMessage("No document found. Capture manually") // Fallback message
                    documentOverlayView.setDocumentCorners(null, previewView, Size(1,1)) // Clear overlay
                }
            }
    }


    private fun updateUIMessage(message: String) {
        messageTextView.text = message
    }

    private fun setAutoCaptureToggle(isAuto: Boolean) {
        isAutoCaptureMode = isAuto
        manualButton.isSelected = !isAuto
        autoCaptureButton.isSelected = isAuto

        // Update text color based on selection
        // IMPORTANT: Use your plugin's R.color instead of android.R.color.
        // I've commented out the problematic lines, you need to define these colors.
        val selectedTextColor = ContextCompat.getColor(this, R.color.black) // Use R.color.black
        val unselectedTextColor = ContextCompat.getColor(this, R.color.white) // Use R.color.white

        manualButton.setTextColor(if (!isAuto) selectedTextColor else unselectedTextColor)
        autoCaptureButton.setTextColor(if (isAuto) selectedTextColor else unselectedTextColor)

        // Enable/disable shutter button based on mode
        shutterButton.isEnabled = !isAuto
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

// Custom ImageAnalyzer to detect document corners
class ImageAnalyzer(private val listener: (List<PointF>?, Size) -> Unit) : ImageAnalysis.Analyzer {

    // You might need a more robust way to detect document corners.
    // This is a placeholder. Consider using OpenCV or more advanced ML Kit features.
    // For simplicity, I'm simulating a document detection.
    private val random = Random()

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            // --- Replace this with actual document detection logic ---
            // Example: Using ML Kit Document Scanner's internal detection (though it's usually called via scanDocument)
            // Or use OpenCV for contour detection.

            // Simulate detection for demonstration:
            val detected = random.nextBoolean() // Simulate sometimes detecting a document
            if (detected && random.nextDouble() > 0.3) { // Make it stable sometimes
                val width = inputImage.width
                val height = inputImage.height
                // Simulate detecting a rectangle in the middle
                val p1 = PointF((width * 0.2).toFloat(), (height * 0.2).toFloat())
                val p2 = PointF((width * 0.8).toFloat(), (height * 0.2).toFloat())
                val p3 = PointF((width * 0.8).toFloat(), (height * 0.8).toFloat())
                val p4 = PointF((width * 0.2).toFloat(), (height * 0.8).toFloat())
                listener(listOf(p1, p2, p3, p4), Size(width, height))
            } else {
                listener(null, Size(inputImage.width, inputImage.height))
            }
            // --- End of simulated detection ---

        }
        imageProxy.close()
    }
}