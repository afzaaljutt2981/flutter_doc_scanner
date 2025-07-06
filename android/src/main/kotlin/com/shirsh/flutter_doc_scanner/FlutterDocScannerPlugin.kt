package com.shirsh.flutter_doc_scanner


import android.app.Activity
import android.app.Application
import android.content.Intent // Make sure this import is present
import android.content.IntentSender
import android.os.Bundle
import android.util.Log
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener // Import this


class FlutterDocScannerPlugin : MethodCallHandler, ActivityResultListener,
    FlutterPlugin, ActivityAware {

    private var channel: MethodChannel? = null
    private var pluginBinding: FlutterPluginBinding? = null
    private var activityBinding: ActivityPluginBinding? = null
    private var applicationContext: Application? = null // This can be private val or var if you want to set it once
    private val CHANNEL = "flutter_doc_scanner"
    private var activity: Activity? = null
    private val TAG = FlutterDocScannerPlugin::class.java.simpleName

    private val REQUEST_CODE_SCAN = 213312
    private val REQUEST_CODE_SCAN_URI = 214412
    private val REQUEST_CODE_SCAN_IMAGES = 215512
    private val REQUEST_CODE_SCAN_PDF = 216612

    // --- NEW Request Code for your custom scanner ---
    private val REQUEST_CODE_CUSTOM_SCANNER = 217712 // Unique request code for your activity

    private lateinit var resultChannel: MethodChannel.Result // This holds the result object for any pending Flutter call

    override fun onMethodCall(call: MethodCall, result: Result) {
        // Assign the result object to resultChannel for later use
        resultChannel = result

        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "getScanDocuments" -> {
                val arguments = call.arguments as? Map<*, *>
                val page = (arguments?.get("page") as? Int)?.coerceAtLeast(1) ?: 4
                startDocumentScan(page) // Uses ML Kit's UI
            }
            "getScannedDocumentAsImages" -> {
                val arguments = call.arguments as? Map<*, *>
                val page = (arguments?.get("page") as? Int)?.coerceAtLeast(1) ?: 4
                startDocumentScanImages(page) // Uses ML Kit's UI
            }
            "getScannedDocumentAsPdf" -> {
                val arguments = call.arguments as? Map<*, *>
                val page = (arguments?.get("page") as? Int)?.coerceAtLeast(1) ?: 4
                startDocumentScanPDF(page) // Uses ML Kit's UI
            }
            "getScanDocumentsUri" -> {
                val arguments = call.arguments as? Map<*, *>
                val page = (arguments?.get("page") as? Int)?.coerceAtLeast(1) ?: 4
                startDocumentScanUri(page) // Uses ML Kit's UI
            }
            "startAdvancedDocumentScan" -> { // THIS IS THE NEW METHOD CALL FOR YOUR CUSTOM UI
                if (activity == null) {
                    resultChannel.error("NO_ACTIVITY", "Plugin not attached to an Activity.", null)
                    return
                }
                // Launch your custom DocumentScannerActivity
                // CORRECTED: Use YourCurrentScannerActivity::class.java
                val intent = Intent(activity, YourCurrentScannerActivity::class.java)
                activity?.startActivityForResult(intent, REQUEST_CODE_CUSTOM_SCANNER)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    // Existing ML Kit Document Scanner launch methods (no changes needed here)
    private fun startDocumentScan(page: Int = 4) {
        val options =
            GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(page)
                .setResultFormats(
                    GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                    GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                ).setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
        val scanner = GmsDocumentScanning.getClient(options)
        val task: Task<IntentSender>? = activity?.let { scanner.getStartScanIntent(it) }
        task?.addOnSuccessListener { intentSender ->
            val intent = IntentSenderRequest.Builder(intentSender).build().intentSender
            try {
                startIntentSenderForResult(
                    activity!!,
                    intent,
                    REQUEST_CODE_SCAN,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            } catch (e: Exception) {
                resultChannel.error("SCAN_ERROR", "Failed to launch ML Kit Scanner: ${e.message}", null)
                e.printStackTrace()
            }
        }?.addOnFailureListener { e ->
            resultChannel.error("SCAN_FAILURE", "ML Kit Scanner launch failed: ${e.message}", null)
        }
    }

    private fun startDocumentScanImages(page: Int = 4) {
        val options =
            GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(page)
                .setResultFormats(
                    GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                    GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                ).setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
        val scanner = GmsDocumentScanning.getClient(options)
        val task: Task<IntentSender>? = activity?.let { scanner.getStartScanIntent(it) }
        task?.addOnSuccessListener { intentSender ->
            val intent = IntentSenderRequest.Builder(intentSender).build().intentSender
            try {
                startIntentSenderForResult(
                    activity!!,
                    intent,
                    REQUEST_CODE_SCAN_IMAGES,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            } catch (e: Exception) {
                resultChannel.error("SCAN_ERROR", "Failed to launch ML Kit Scanner: ${e.message}", null)
                e.printStackTrace()
            }
        }?.addOnFailureListener { e ->
            resultChannel.error("SCAN_FAILURE", "ML Kit Scanner launch failed: ${e.message}", null)
        }
    }

    private fun startDocumentScanPDF(page: Int = 4) {
        val options =
            GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(page)
                .setResultFormats(
                    GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                    GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                ).setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
        val scanner = GmsDocumentScanning.getClient(options)
        val task: Task<IntentSender>? = activity?.let { scanner.getStartScanIntent(it) }
        task?.addOnSuccessListener { intentSender ->
            val intent = IntentSenderRequest.Builder(intentSender).build().intentSender
            try {
                startIntentSenderForResult(
                    activity!!,
                    intent,
                    REQUEST_CODE_SCAN_PDF,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            } catch (e: Exception) {
                resultChannel.error("SCAN_ERROR", "Failed to launch ML Kit Scanner: ${e.message}", null)
                e.printStackTrace()
            }
        }?.addOnFailureListener { e ->
            resultChannel.error("SCAN_FAILURE", "ML Kit Scanner launch failed: ${e.message}", null)
        }
    }

    private fun startDocumentScanUri(page: Int = 4) {
        val options =
            GmsDocumentScannerOptions.Builder().setGalleryImportAllowed(true).setPageLimit(page)
                .setResultFormats(
                    GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
                    GmsDocumentScannerOptions.RESULT_FORMAT_PDF
                ).setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL).build()
        val scanner = GmsDocumentScanning.getClient(options)
        val task: Task<IntentSender>? = activity?.let { scanner.getStartScanIntent(it) }
        task?.addOnSuccessListener { intentSender ->
            val intent = IntentSenderRequest.Builder(intentSender).build().intentSender
            try {
                startIntentSenderForResult(
                    activity!!,
                    intent,
                    REQUEST_CODE_SCAN_URI,
                    null,
                    0,
                    0,
                    0,
                    null
                )
            } catch (e: Exception) {
                resultChannel.error("SCAN_ERROR", "Failed to launch ML Kit Scanner: ${e.message}", null)
                e.printStackTrace()
            }
        }?.addOnFailureListener { e ->
            resultChannel.error("SCAN_FAILURE", "ML Kit Scanner launch failed: ${e.message}", null)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        // Handle results from existing ML Kit Scanner calls
        when (requestCode) {
            REQUEST_CODE_SCAN -> {
                if (resultCode == Activity.RESULT_OK) {
                    val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
                    scanningResult?.getPdf()?.let { pdf ->
                        val pdfUri = pdf.getUri()
                        val pageCount = pdf.getPageCount()
                        resultChannel.success(
                            mapOf(
                                "pdfUri" to pdfUri.toString(),
                                "pageCount" to pageCount,
                            )
                        )
                    } ?: resultChannel.error("SCAN_FAILED", "No PDF result returned from ML Kit", null)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    resultChannel.success(null) // User cancelled
                } else {
                    resultChannel.error("SCAN_FAILED", "ML Kit scanning failed with code: $resultCode", null)
                }
                return true // Handled this request code
            }
            REQUEST_CODE_SCAN_IMAGES -> {
                if (resultCode == Activity.RESULT_OK) {
                    val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
                    scanningResult?.getPages()?.let { pages ->
                        // Collect URIs as Strings
                        val uriStrings = pages.map { it.imageUri.toString() }
                        resultChannel.success(
                            mapOf(
                                "Uris" to uriStrings, // Changed key to "Uris" for consistency
                                "Count" to pages.size,
                            )
                        )
                    } ?: resultChannel.error("SCAN_FAILED", "No image results returned from ML Kit", null)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    resultChannel.success(null)
                } else {
                    resultChannel.error("SCAN_FAILED", "ML Kit image scanning failed with code: $resultCode", null)
                }
                return true
            }
            REQUEST_CODE_SCAN_PDF -> { // This case is similar to REQUEST_CODE_SCAN, might be redundant if REQUEST_CODE_SCAN handles all formats
                if (resultCode == Activity.RESULT_OK) {
                    val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
                    scanningResult?.getPdf()?.let { pdf ->
                        val pdfUri = pdf.getUri()
                        val pageCount = pdf.getPageCount()
                        resultChannel.success(
                            mapOf(
                                "pdfUri" to pdfUri.toString(),
                                "pageCount" to pageCount,
                            )
                        )
                    } ?: resultChannel.error("SCAN_FAILED", "No PDF result returned from ML Kit", null)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    resultChannel.success(null)
                } else {
                    resultChannel.error("SCAN_FAILED", "ML Kit PDF scanning failed with code: $resultCode", null)
                }
                return true
            }
            REQUEST_CODE_SCAN_URI -> { // This case is similar to REQUEST_CODE_SCAN_IMAGES, might be redundant
                if (resultCode == Activity.RESULT_OK) {
                    val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(data)
                    scanningResult?.getPages()?.let { pages ->
                        val uriStrings = pages.map { it.imageUri.toString() }
                        resultChannel.success(
                            mapOf(
                                "Uris" to uriStrings, // Changed key to "Uris" for consistency
                                "Count" to pages.size,
                            )
                        )
                    } ?: resultChannel.error("SCAN_FAILED", "No URI results returned from ML Kit", null)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    resultChannel.success(null)
                } else {
                    resultChannel.error("SCAN_FAILED", "ML Kit URI scanning failed with code: $resultCode", null)
                }
                return true
            }
            // --- NEW Case for your custom scanner results ---
            REQUEST_CODE_CUSTOM_SCANNER -> {
                if (resultCode == Activity.RESULT_OK) {
                    // Expecting a list of image paths (Strings) from your custom YourCurrentScannerActivity
                    // CORRECTED: Use YourCurrentScannerActivity.SCANNED_IMAGE_PATHS_KEY
                    val imagePaths = data?.getStringArrayListExtra(YourCurrentScannerActivity.SCANNED_IMAGE_PATHS_KEY)
                    resultChannel.success(imagePaths)
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    resultChannel.success(emptyList<String>()) // Or null, depending on your desired Flutter behavior for cancellation
                } else {
                    resultChannel.error("CUSTOM_SCAN_FAILED", "Custom scanner activity failed with code: $resultCode", null)
                }
                return true // Handled this request code
            }
        }
        return false // If the request code is not handled by this plugin
    }

    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        pluginBinding = binding
        // Initialize channel here as well, since onAttachedToActivity might be called later
        channel = MethodChannel(binding.binaryMessenger, CHANNEL)
        channel!!.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
        pluginBinding = null
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    // Updated createPluginSetup logic - simplified
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activityBinding = binding
        activity = binding.activity
        applicationContext = binding.applicationContext as Application // Get application context
        activityBinding?.addActivityResultListener(this) // Register for activity results
        // No need to call createPluginSetup if channel is already initialized in onAttachedToEngine
        // The channel's method handler is already set in onAttachedToEngine, and the activity/context
        // are now available via activityBinding and pluginBinding.applicationContext
    }

    override fun onDetachedFromActivity() {
        activityBinding?.removeActivityResultListener(this) // Unregister
        activityBinding = null
        activity = null // Clear activity reference
        applicationContext = null // Clear application context reference
    }
}