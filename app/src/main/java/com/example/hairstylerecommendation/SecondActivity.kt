package com.example.hairstylerecommendation

import android.os.Bundle
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.hairstylerecommendation.R.id.faceCountText
import com.example.hairstylerecommendation.R.id.overlayView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

class SecondActivity : AppCompatActivity() {

    private lateinit var overlayView: OverlayView
    private lateinit var faceCountText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_second)


        overlayView = findViewById(R.id.overlayView)
        faceCountText = findViewById(R.id.faceCountText)
        val previewView = findViewById<androidx.camera.view.PreviewView>(R.id.previewView)

        val btnBack: Button = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            // Aksi saat tombol diklik, seperti finish atau pindah ke activity lain
            finish()  // Jika ingin kembali ke activity sebelumnya
        }

        previewView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Get the size of the preview view after layout
                val previewWidth = previewView.width
                val previewHeight = previewView.height

                // Now remove the layout listener to avoid repeated calls
                previewView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                // Initialize the camera provider once the preview size is known
                val cameraProviderFuture = ProcessCameraProvider.getInstance(this@SecondActivity)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    // Preview setup
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    // Face detection analyzer setup
                    val faceAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(ContextCompat.getMainExecutor(this@SecondActivity), FaceAnalyzer())
                        }

                    val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                    try {
                        // Unbind previous use cases and bind new ones
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(this@SecondActivity, cameraSelector, preview, faceAnalyzer)

                        // Pass the size of the preview view to the overlay view
                        overlayView.setFaces(emptyList(), previewWidth, previewHeight)

                    } catch (exc: Exception) {
                        Log.e("MainActivity", "Binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(this@SecondActivity))
            }
        })
    }

    private inner class FaceAnalyzer : ImageAnalysis.Analyzer {

        @OptIn(ExperimentalGetImage::class)
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                val options = FaceDetectorOptions.Builder()
                    .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                    .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                    .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                    .build()

                val detector = FaceDetection.getClient(options)

                detector.process(image)
                    .addOnSuccessListener { faces ->
                        updateOverlay(faces)
                        updateFaceCount(faces.size, faces)
                    }
                    .addOnFailureListener { e ->
                        Log.e("FaceAnalyzer", "Face detection failed", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        }

        private fun updateOverlay(faces: List<Face>) {
            // Assuming the preview size is passed correctly
            overlayView.setFaces(faces, previewWidth = 640, previewHeight = 480) // Use the actual preview size
            overlayView.invalidate()
        }

        private fun updateFaceCount(count: Int, faces: List<Face>) {
            if (count == 0) {
                faceCountText.text = "No faces detected"
                return
            }

            // Efficient shape count and total face calculation
            val shapeCounts = faces.fold(mutableMapOf<String, Int>()) { map, face ->
                val boundingBox = face.boundingBox
                val width = boundingBox.width().toDouble()
                val height = boundingBox.height().toDouble()
                val faceLength = Math.sqrt(width * width + height * height) // Diagonal length
                val ratio = width / height

                val shape = determineShape(ratio, faceLength, width, height)

                map[shape] = map.getOrDefault(shape, 0) + 1
                map
            }

            val totalFaces = faces.size

            // Calculate shape percentages with clear formatting
            val shapePercentages = shapeCounts.map { (shape, count) ->
                "$shape"
            }.joinToString("\n")

            // Construct formatted output with indentation and line breaks
            faceCountText.text = String.format("""
        Face detected

        Shape breakdown: %s
    """, shapePercentages).trimIndent()
        }

        private fun determineShape(ratio: Double, faceLength: Double, width: Double, height: Double): String {
            val tolerance = 0.1 // Adjust tolerance for more/less strict classification

            return when {
                // Rectangle or Oblong shape - high width-to-height ratio, low height-to-length ratio
                ratio > 1.9 - tolerance && height / faceLength < 0.5 + tolerance -> "Rectangle/Oblong"

                // Oval shape - moderate width-to-height ratio, higher height-to-length ratio
                ratio in 1.5 - tolerance..1.9 + tolerance && height / faceLength > 0.52 - tolerance -> "Oval (Ratio: ${String.format("%.2f", ratio)})"

                // Square shape - near equal width and height, higher width-to-length ratio
                ratio in 0.9 - tolerance..1.2 + tolerance && width / faceLength > 0.55 - tolerance -> "Square (Ratio: ${String.format("%.2f", ratio)})"

                // Triangle shape - high height, low ratio, low height-to-length ratio
                ratio in 0.7 - tolerance..0.9 + tolerance && width / faceLength > 0.6 - tolerance -> "Triangle (Ratio: ${String.format("%.2f", ratio)})"

                // Diamond shape - moderate ratio, higher height-to-length ratio
                ratio in 1.3 - tolerance..1.4 + tolerance && height / faceLength > 0.5 - tolerance -> "Diamond (Ratio: ${String.format("%.2f", ratio)})"

                // Heart shape - higher height, low ratio, low height-to-length ratio
                width < height - tolerance && ratio < 1.0 + tolerance && height / faceLength < 0.6 + tolerance -> "Heart (Ratio: ${String.format("%.2f", ratio)})"

                else -> {
                    // Show the ratio when shape is unclassified
                    "Unclassified (Ratio: ${String.format("%.2f", ratio)})"
                }
            }
        }


    }
}


//
//package com.example.hairstylerecommendation
//
//import android.os.Bundle
//import android.widget.Toast
//import androidx.appcompat.app.AppCompatActivity
//import androidx.recyclerview.widget.LinearLayoutManager
//import androidx.recyclerview.widget.RecyclerView
//import com.example.hairstylerecommendation.adapter.UserAdapter
//import com.example.hairstylerecommendation.api.RetrofitInstance
//import com.example.hairstylerecommendation.models.User
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//
//class SecondActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
//        recyclerView.layoutManager = LinearLayoutManager(this)
//
//        RetrofitInstance.api.getUsers().enqueue(object : Callback<List<User>> {
//            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
//                if (response.isSuccessful) {
//                    val users = response.body() ?: emptyList()
//                    recyclerView.adapter = UserAdapter(users)
//                } else {
//                    Toast.makeText(this@SecondActivity, "Error: ${response.message()}", Toast.LENGTH_LONG).show()
//                }
//            }
//
//            override fun onFailure(call: Call<List<User>>, t: Throwable) {
//                Toast.makeText(this@SecondActivity, "Failed: ${t.message}", Toast.LENGTH_LONG).show()
//            }
//        })
//    }
//}
