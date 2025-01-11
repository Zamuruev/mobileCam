package ru.rut.democamera

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.snackbar.Snackbar
import ru.rut.democamera.databinding.ActivityMainBinding
import java.io.File

class PhotoStartActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private var imageCapture: ImageCapture? = null

    @RequiresApi(Build.VERSION_CODES.M)
    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                startCamera()
            } else {
                Snackbar.make(
                    binding.root,
                    "The camera permission is necessary",
                    Snackbar.LENGTH_INDEFINITE
                ).show()
            }
        }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.preview.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            binding.preview.scaleType = PreviewView.ScaleType.FILL_CENTER
            try {
                cameraProvider.unbindAll()  // Очищаем все привязки перед повторным связыванием камеры
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Log.e("TAG", "Use case binding failed: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Запрос разрешений камеры
        cameraPermissionResult.launch(android.Manifest.permission.CAMERA)

        // Инициализация камеры с задней камерой
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        // Кнопка для переключения камеры
        binding.switchBtn.setOnClickListener {
            // Переключаем состояние камеры (передняя/задняя)
            val newLensFacing = if (cameraSelector.lensFacing == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }

            // Обновляем cameraSelector с новым состоянием
            cameraSelector = CameraSelector.Builder()
                .requireLensFacing(newLensFacing)
                .build()

            // Перезапускаем камеру с новым CameraSelector
            startCamera()
        }

        // Кнопка для перехода на экран видео
        binding.switchToVideoBtn.setOnClickListener {
            val intent = Intent(this, VideoStartActivity::class.java) // Переход на экран видео
            startActivity(intent)
        }

        // Кнопка для захвата фотографии
        binding.imgCaptureBtn.setOnClickListener {
            takePhoto() // Логика для съемки фото
        }

        // Открытие галереи
        binding.galleryBtn.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraProvider.unbindAll()

    }

    private fun takePhoto() {
        imageCapture?.let {
            val fileName = "JPEG_${System.currentTimeMillis()}.jpg"
            val file = File(externalMediaDirs[0], fileName)
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            it.takePicture(
                outputFileOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.i("TAG", "The image has been saved in ${file.toUri()}")
                        Toast.makeText(this@PhotoStartActivity, "Photo saved!", Toast.LENGTH_SHORT).show()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e("TAG", "Error taking photo: ${exception.message}")
                        Toast.makeText(this@PhotoStartActivity, "Error taking photo", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = ColorDrawable(Color.WHITE)
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

}

