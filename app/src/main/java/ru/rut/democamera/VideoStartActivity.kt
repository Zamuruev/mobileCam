package ru.rut.democamera

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.Recording
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.color
import com.mikepenz.iconics.utils.icon
import ru.rut.democamera.databinding.ActivityVideoBinding
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoStartActivity : ComponentActivity() {

    private lateinit var binding: ActivityVideoBinding
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraExecutor: ExecutorService
    private var isRecording = false
    private var startTime = 0L
    private var currentCameraSelector = CameraSelector.LENS_FACING_BACK
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val elapsedTime = System.currentTimeMillis() - startTime
                val seconds = (elapsedTime / 1000).toInt()
                val minutes = seconds / 60
                val displaySeconds = seconds % 60
                val time = String.format("%02d:%02d", minutes, displaySeconds)

                Log.d("CameraX", "Timer updated: $time")

                if (isRecording) {
                    binding.timerText.text = time
                }

                handler.postDelayed(this, 1000)
            }
        }
    }


    private var recording: Recording? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        checkPermissions()

        binding.recordBtn.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        binding.photoBtn.setOnClickListener {
            val intent = Intent(this, PhotoStartActivity::class.java)
            startActivity(intent)
        }

        binding.switchBtn.setOnClickListener {
            toggleCamera()
        }

        binding.galleryBtn.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // Настроим preview для камеры
                val preview = androidx.camera.core.Preview.Builder().build().also {
                    it.setSurfaceProvider(binding.preview.surfaceProvider)  // Используем SurfaceProvider для отображения
                }

                // Используем стандартное качество
                val recorder = Recorder.Builder().build()

                // Настроим VideoCapture с выводом для записи
                videoCapture = VideoCapture.withOutput(recorder)

                // Камера с выбором линзы (фронтальная или задняя)
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(currentCameraSelector)
                    .build()

                cameraProvider.unbindAll()  // Очищаем все привязанные жизненные циклы
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)

                Log.d("CameraX", "Camera successfully initialized")
            } catch (e: Exception) {
                Log.e("CameraX", "Error starting camera: ${e.message}")
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun toggleCamera() {
        currentCameraSelector = if (currentCameraSelector == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        startCamera()
    }

    private fun startRecording() {
        if (!this::videoCapture.isInitialized) {
            Toast.makeText(this, "Camera is not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        // Показываем таймер
        binding.timerText.visibility = View.VISIBLE
        val fileName = "Video_${System.currentTimeMillis()}.mp4"
        val videoFile = File(externalMediaDirs[0], fileName)

        // Проверка, существует ли родительская директория
        if (!videoFile.parentFile.exists()) {
            val dirCreated = videoFile.parentFile.mkdirs()
            if (!dirCreated) {
                Log.e("CameraX", "Failed to create directory")
                return
            }
        }

        // Здесь создаём поток для записи
        val recorder = Recorder.Builder().build()

        // Настроим объект для записи видео на указанный файл
        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        val pendingRecording = videoCapture.output.prepareRecording(this, outputOptions)

        recording = pendingRecording.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    Log.d("CameraX", "Recording started!")
                    isRecording = true
                    startTime = System.currentTimeMillis()
                    handler.post(updateTimerRunnable)  // Таймер запускается

                    // Изменяем цвет на красный для кнопки остановки
                    binding.recordBtn.icon = IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_stop)
                        .color({ IconicsColor(ContextCompat.getColor(this, R.color.red)) })

                }
                is VideoRecordEvent.Finalize -> {
                    Log.d("CameraX", "Recording finalized.")
                    if (recordEvent.hasError()) {
                        Log.e("CameraX", "Recording failed: ${recordEvent.error}")
                        Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Recording saved!", Toast.LENGTH_SHORT).show()
                        // Если нужно добавить в галерею, используйте addVideoToGallery
                        addVideoToGallery(videoFile)
                    }
                    isRecording = false
                    handler.removeCallbacks(updateTimerRunnable)

                    // Возвращаем иконку камеры в белый цвет
                    binding.recordBtn.icon = IconicsDrawable(this)
                        .icon(GoogleMaterial.Icon.gmd_videocam)
                        .color({ IconicsColor(ContextCompat.getColor(this, android.R.color.white)) })
                }
            }
        }

    }

    private fun IconicsColor(color: Int): IconicsColor {
        return IconicsColor.colorInt(color)
    }



    private fun stopRecording() {
        // Останавливаем запись и корректно обновляем UI
        recording?.stop()
        isRecording = false
        handler.removeCallbacks(updateTimerRunnable)
        binding.recordBtn.icon = IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_videocam)
        binding.timerText.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()
        if (!this::videoCapture.isInitialized) {
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recording?.stop()
        cameraExecutor.shutdown()
        Log.d("CameraX", "Camera resources cleaned up.")
    }

    private fun addVideoToGallery(file: File) {
        // Проверяем, существует ли файл перед его добавлением
        if (!file.exists()) {
            Log.e("CameraX", "File does not exist: ${file.path}")
            return
        }

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/DemoCamera")  // Путь к MediaStore
        }

        // Вставляем в MediaStore
        contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
            Log.d("CameraX", "Video URI: $uri")  // Логируем URI
            try {
                contentResolver.openOutputStream(uri)?.use { outputStream ->
                    file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                Log.d("CameraX", "Video added to gallery successfully")
            } catch (e: Exception) {
                Log.e("CameraX", "Error copying video to gallery: ${e.message}")
            }
        } ?: run {
            Log.e("CameraX", "Failed to add video to gallery")
        }
    }
}
