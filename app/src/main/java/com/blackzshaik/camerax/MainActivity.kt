package com.blackzshaik.camerax

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.camera.core.Camera
import androidx.camera.core.CameraProvider
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture


class MainActivity : AppCompatActivity(), ImageCapture.OnImageSavedCallback {

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var imageCapture: ImageCapture
    private lateinit var camera: Camera
    private lateinit var cameraProvider: ProcessCameraProvider

    private val backCameraSelector by lazy {
        getCameraSelector(CameraSelector.LENS_FACING_BACK)
    }

    private val frontCameraSelector by lazy {
        getCameraSelector(CameraSelector.LENS_FACING_FRONT)
    }

    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Camera permission not available closing app",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionResult.launch(Manifest.permission.CAMERA)
        } else {
            startCamera()
        }

    }


    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider, backCameraSelector)
        }, ContextCompat.getMainExecutor(this))

        findViewById<AppCompatImageView>(R.id.captureButton).setOnClickListener {
            takePicture()
        }

        findViewById<AppCompatImageView>(R.id.flipButton).setOnClickListener {
            if (!this::camera.isInitialized && !this::cameraProvider.isInitialized) return@setOnClickListener

            if (camera.cameraInfo.lensFacing == CameraSelector.LENS_FACING_FRONT) {
                cameraProvider.unbindAll()
                bindPreview(cameraProvider, backCameraSelector)
            } else {
                cameraProvider.unbindAll()
                bindPreview(cameraProvider, frontCameraSelector)
            }
        }
    }


    private fun bindPreview(cameraProvider: ProcessCameraProvider, cameraSelector: CameraSelector) {
        val preview = Preview.Builder().build()
        val previewView = findViewById<PreviewView>(R.id.previewView)

        preview.setSurfaceProvider(previewView.surfaceProvider)

        imageCapture =
            ImageCapture.Builder().setTargetRotation(previewView.display.rotation).build()

        camera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )

    }

    private fun takePicture() {
        if (this::imageCapture.isInitialized.not()) return

        imageCapture.takePicture(
            getImageOutputOptions(),
            ContextCompat.getMainExecutor(this),
            this
        )
    }

    private fun getCameraSelector(lensFacing: Int) =
        CameraSelector.Builder().requireLensFacing(lensFacing).build()

    private fun getImageOutputOptions(): ImageCapture.OutputFileOptions {
        val contentResolver = applicationContext.contentResolver
        val contentValues: ContentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}")
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        return ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()
    }

    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
        Snackbar.make(
            findViewById(R.id.rootView),
            "Image saved successfully",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onError(exception: ImageCaptureException) {
        exception.printStackTrace()
    }
}