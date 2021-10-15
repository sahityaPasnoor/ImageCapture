package com.example.imagecapture

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Surface.ROTATION_0
import android.view.Surface.ROTATION_90
import android.widget.Button
import android.widget.Toast
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.*
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var videoCapture: VideoCapture
    private lateinit var viewFinder: PreviewView
    private lateinit var outputDirectory: File
    private var lensFacing: Int = CameraSelector.LENS_FACING_FRONT

    private val executor = Executors.newSingleThreadExecutor()
    private var isRecording = false
    private var camera: Camera? = null
    private lateinit var cameraProviderFuture: ProcessCameraProvider


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)




        viewFinder = findViewById(R.id.preview_view)

        requestPermission()


        startCamera(this)



        findViewById<Button>(R.id.stop).setOnClickListener {
            stopRecording()
        }
    }

    @SuppressLint("RestrictedApi", "UnsafeExperimentalUsageError")
    private fun startCamera(context: Context) {

        cameraProviderFuture = ProcessCameraProvider.getInstance(context).get()


        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()


        videoCapture = VideoCapture.Builder.fromConfig(VideoCapture.DEFAULT_CONFIG.config).build()


        val preview: Preview = Preview.Builder().apply {
            setTargetAspectRatio(AspectRatio.RATIO_16_9)
            setTargetRotation(ROTATION_0)
        }.build()
        preview.setSurfaceProvider(viewFinder.surfaceProvider)

        cameraProviderFuture.bindToLifecycle(
            this@MainActivity,
            cameraSelector,
            preview,
            videoCapture
        )
        startRecording()


    }


    private fun requestPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        )
            requestPermissions(
                permissions, 100
            )
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startRecording()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }


    @SuppressLint("RestrictedApi", "MissingPermission")
    private fun startRecording() {


        val file = createFile(
            getOutputDirectory(this),
            FILENAME,
            VIDEO_EXTENSION
        )


        val output = VideoCapture.OutputFileOptions.Builder(file).build()

        videoCapture.startRecording(
            output,
            executor,
            object : VideoCapture.OnVideoSavedCallback {


                override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                    outputFileResults.savedUri?.toFile()
                }

                override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                    cause?.printStackTrace()
                }
            }
        )
    }

    @SuppressLint("RestrictedApi")
    private fun stopRecording() {
        videoCapture.stopRecording()
    }

    companion object {
        private const val FILENAME = "yyyy_MM_dd_HH_mm_ss"
        private const val VIDEO_EXTENSION = ".mp4"

        private val permissions = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )

        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = appContext.externalMediaDirs?.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() }
            }
            return if (mediaDir != null && mediaDir.exists()) mediaDir else appContext.filesDir
        }

        fun createFile(baseFolder: File, format: String, extension: String) =
            File(
                baseFolder, SimpleDateFormat(format, Locale.US)
                    .format(System.currentTimeMillis()) + extension
            )
    }

}