package com.shirsh.flutter_doc_scanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path // Added import for Path
import android.graphics.PointF // Added import for PointF (or ensure it's there)
import android.util.AttributeSet // Added import for AttributeSet
import android.util.Size
import android.view.View
import androidx.camera.view.PreviewView
import kotlin.math.min

class DocumentOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = android.graphics.Color.RED // Default border color
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    private var documentCorners: List<PointF>? = null
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0

    /**
     * Sets the detected document corners for drawing the overlay.
     * @param corners List of 4 PointF objects representing the document corners in image coordinates.
     * @param previewView The PreviewView displaying the camera feed, used for scaling.
     * @param imageAnalysisSize The resolution of the image frames being analyzed (e.g., from ImageAnalysis use case).
     */
    fun setDocumentCorners(corners: List<PointF>?, previewView: PreviewView, imageAnalysisSize: Size) {
        this.documentCorners = corners
        this.previewWidth = previewView.width
        this.previewHeight = previewView.height
        this.imageWidth = imageAnalysisSize.width
        this.imageHeight = imageAnalysisSize.height
        invalidate() // Request a redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        documentCorners?.let { corners ->
            if (corners.size == 4 && previewWidth > 0 && previewHeight > 0 && imageWidth > 0 && imageHeight > 0) {
                // Map the corners from image coordinates to screen coordinates
                // The camera preview might be scaled or fit differently than the raw image analysis frames.
                // This mapping assumes a 'fitCenter' or similar scaling where the image is centered
                // and potentially letterboxed/pillarboxed within the preview view.

                // Calculate the scaling factors for width and height
                val scaleX = previewWidth.toFloat() / imageWidth.toFloat() // Ensure float division
                val scaleY = previewHeight.toFloat() / imageHeight.toFloat() // Ensure float division

                // Determine the actual scale to maintain aspect ratio (fitCenter)
                val actualScale = min(scaleX, scaleY)

                // Calculate offsets if the image is letterboxed/pillarboxed
                val offsetX = (previewWidth.toFloat() - (imageWidth.toFloat() * actualScale)) / 2f
                val offsetY = (previewHeight.toFloat() - (imageHeight.toFloat() * actualScale)) / 2f

                val mappedCorners = corners.map { point ->
                    PointF(
                        point.x * actualScale + offsetX,
                        point.y * actualScale + offsetY
                    )
                }

                val path = Path()
                path.moveTo(mappedCorners[0].x, mappedCorners[0].y)
                path.lineTo(mappedCorners[1].x, mappedCorners[1].y)
                path.lineTo(mappedCorners[2].x, mappedCorners[2].y)
                path.lineTo(mappedCorners[3].x, mappedCorners[3].y)
                path.close()
                canvas.drawPath(path, paint)
            }
        }
    }
}