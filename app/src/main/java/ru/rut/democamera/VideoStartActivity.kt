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
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recording
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.googlematerial.GoogleMaterial
import com.mikepenz.iconics.utils.icon
import ru.rut.democamera.databinding.ActivityVideoBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoStartActivity : ComponentActivity() {

    private lateinit var binding: ActivityVideoBinding
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var cameraExecutor: ExecutorService
    private var isRecording = false
    private var startTime = 0L
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimerRunnable = object : Runnable {
        override fun run() {
            if (isRecording) {
                val elapsedTime = System.currentTimeMillis() - startTime
                val seconds = (elapsedTime / 1000).toInt()
                val minutes = seconds / 60
                val displaySeconds = seconds % 60
                val time = String.format("%02d:%02d", minutes, displaySeconds)

                // Обновление UI должно происходить в основном потоке
                runOnUiThread {
                    binding.timerText.text = time
                }

                Log.d("CameraX", "Timer updated: $time")  // Логирование таймера
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

        // Проверка разрешений
        checkPermissions()

        // Кнопка для записи видео
        binding.recordBtn.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        // Кнопка для перехода на экран фото
        binding.photoBtn.setOnClickListener {
            val intent = Intent(this, PhotoStartActivity::class.java)
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
            startCamera()  // Если все разрешения есть, запускаем камеру
        } else {
            ActivityCompat.requestPermissions(this, permissions, 1)  // Запрашиваем разрешения
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(binding.preview.surfaceProvider)
            }

            // Создаем Recorder через Builder
            val qualitySelector = QualitySelector.from(Quality.HD)
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()

            // Создаем VideoCapture с этим Recorder
            videoCapture = VideoCapture.withOutput(recorder)

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            try {
                cameraProvider.unbindAll()  // Освобождаем все ресурсы камеры перед её подключением
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, videoCapture)
                Log.d("CameraX", "Camera successfully initialized")
            } catch (e: Exception) {
                Log.e("CameraX", "Error starting camera: ${e.message}")
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun startRecording() {
        if (!this::videoCapture.isInitialized) {
            Toast.makeText(this, "Camera is not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        val name = "CameraX-recording-" +
                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
        }

        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        // Проверка перед подготовкой записи
        if (!this::videoCapture.isInitialized) {
            Log.e("CameraX", "videoCapture is not initialized")
            return
        }

        // Подготовка записи
        val pendingRecording = videoCapture.output.prepareRecording(this, mediaStoreOutput)

        // Начало записи
        recording = pendingRecording.start(ContextCompat.getMainExecutor(this)) { recordEvent: VideoRecordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    Log.d("CameraX", "Recording started!")
                    isRecording = true
                    startTime = System.currentTimeMillis()
                    handler.post(updateTimerRunnable)  // Начинаем обновление таймера
                    binding.recordBtn.icon = IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_stop)
                }
                is VideoRecordEvent.Finalize -> {
                    Log.d("CameraX", "Recording finalized.")
                    if (recordEvent.hasError()) {
                        Log.e("CameraX", "Recording failed: ${recordEvent.error}")
                        Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Recording saved!", Toast.LENGTH_SHORT).show()
                    }
                    isRecording = false
                    handler.removeCallbacks(updateTimerRunnable)  // Останавливаем таймер
                    binding.recordBtn.icon = IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_videocam)
                }
            }
        }
    }

    private fun stopRecording() {
        // Остановка записи
        recording?.stop()  // Просто останавливаем запись
        isRecording = false
        handler.removeCallbacks(updateTimerRunnable)  // Останавливаем таймер
        binding.recordBtn.icon = IconicsDrawable(this).icon(GoogleMaterial.Icon.gmd_videocam)
    }



    override fun onResume() {
        super.onResume()
        if (!this::videoCapture.isInitialized) {
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recording?.stop()  // Останавливаем запись при уничтожении активности
        cameraExecutor.shutdown()
        Log.d("CameraX", "Camera resources cleaned up.")
    }
}
