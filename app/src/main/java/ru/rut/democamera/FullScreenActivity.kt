package ru.rut.democamera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import ru.rut.democamera.databinding.ActivityFullScreenBinding
import java.io.File

class FullScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullScreenBinding
    private var videoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Получаем путь файла, переданный через Intent
        val filePath = intent.getStringExtra("filePath")
        videoFile = if (filePath != null) File(filePath) else null

        // Проверяем, что файл существует
        if (videoFile != null && videoFile!!.exists()) {
            if (videoFile!!.extension == "mp4") {
                // Если файл — видео, используем VideoView
                binding.fullScreenImage.visibility = android.view.View.GONE
                binding.fullScreenVideo.visibility = android.view.View.VISIBLE
                binding.restartBtn.visibility = android.view.View.VISIBLE // Показываем кнопку "Проиграть"
                val videoUri = Uri.fromFile(videoFile)
                binding.fullScreenVideo.setVideoURI(videoUri)
                binding.fullScreenVideo.start()

                // Устанавливаем слушатель для перезапуска видео
                binding.fullScreenVideo.setOnCompletionListener {
                    // После завершения видео кнопка "Проиграть" доступна
                    binding.restartBtn.visibility = android.view.View.VISIBLE
                }

                // Обработчик нажатия кнопки перезапуска
                binding.restartBtn.setOnClickListener {
                    binding.fullScreenVideo.seekTo(0) // Перемещаем видео в начало
                    binding.fullScreenVideo.start() // Перезапускаем воспроизведение
                }
            } else {
                // Если файл — изображение, используем ImageView
                binding.fullScreenImage.visibility = android.view.View.VISIBLE
                binding.fullScreenVideo.visibility = android.view.View.GONE
                binding.restartBtn.visibility = android.view.View.GONE // Скрываем кнопку "Проиграть"
                Glide.with(this)
                    .load(videoFile)
                    .into(binding.fullScreenImage)
            }
        } else {
            Toast.makeText(this, "File not found or invalid path", Toast.LENGTH_SHORT).show()
            finish() // Закрываем активность, если файл не найден
            return
        }

        // Кнопка удаления файла
        binding.deleteBtn.setOnClickListener {
            if (videoFile != null && videoFile!!.exists()) {
                val deleted = videoFile!!.delete()
                if (deleted) {
                    Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show()

                    // Возвращаем результат в GalleryActivity
                    val resultIntent = Intent()
                    resultIntent.putExtra("deletedFilePath", videoFile!!.absolutePath)
                    setResult(RESULT_OK, resultIntent)

                    finish() // Закрываем полноэкранное окно
                } else {
                    Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Кнопка возврата в галерею
        binding.galleryBtn.setOnClickListener {
            finish() // Закрываем текущую активность
        }
    }
}
