package com.example.hairstylerecommendation

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceLandmark

class OverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paintContour = Paint().apply {
        color = Color.GREEN
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private var faces: List<Face> = emptyList()
    private var previewWidth: Int = 0
    private var previewHeight: Int = 0
    private var textView: TextView? = null  // TextView reference to show face info

    // Set the faces and preview size (to scale the bounding box)
    fun setFaces(faces: List<Face>, previewWidth: Int, previewHeight: Int) {
        this.faces = faces
        this.previewWidth = previewWidth
        this.previewHeight = previewHeight
        invalidate() // Redraw the overlay
    }

    // Set the TextView reference from Activity/Fragment
    fun setTextView(textView: TextView) {
        this.textView = textView
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (face in faces) {
            // Get the bounding box of the face
            val boundingBox = face.boundingBox

            // Apply scaling to the bounding box coordinates based on the preview dimensions
            val left = scaleX(boundingBox.left.toFloat())
            val top = scaleY(boundingBox.top.toFloat())
            val right = scaleX(boundingBox.right.toFloat() +150)
            val bottom = scaleY(boundingBox.bottom.toFloat()-10)

            // Draw the bounding box
            canvas.drawRect(left, top, right, bottom, paintContour)

            // Display face-related information in the TextView
            updateFaceText(face)
        }
    }

//    private fun scaleX(x: Float): Float {
//        return ((x / previewWidth) * width *1f)
//    }
    private fun scaleX(x: Float): Float {
        // Membalik koordinat X untuk menciptakan efek cermin
        val mirroredX = previewWidth - x
        return ((mirroredX / previewWidth) * width)
    }


    private fun scaleY(y: Float): Float {
        return (y / previewHeight) * height /1.3f
    }

    // Update the TextView with face-related information
    private fun updateFaceText(face: Face) {
        textView?.let {
            val smileText = face.smilingProbability?.let { "Smile: ${(it * 100).toInt()}%" } ?: "Smile: N/A"
            val leftEyeText = face.leftEyeOpenProbability?.let { "Left Eye Open: ${(it * 100).toInt()}%" } ?: "Left Eye Open: N/A"
            val rightEyeText = face.rightEyeOpenProbability?.let { "Right Eye Open: ${(it * 100).toInt()}%" } ?: "Right Eye Open: N/A"
            val rotationText = "RotX: ${face.headEulerAngleX}\nRotY: ${face.headEulerAngleY}\nRotZ: ${face.headEulerAngleZ}"

            // Combine all the text info
            val text = "$smileText\n$leftEyeText\n$rightEyeText\n$rotationText"
            it.text = text
        }
    }
}
