package ru.rut.democamera

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import ru.rut.democamera.databinding.ActivityFullScreenBinding
import java.io.File

class FullScreenActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFullScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val filePath = intent.getStringExtra("filePath")
        val file = if (filePath != null) File(filePath) else null

        if (file != null && file.exists()) {
            Glide.with(this)
                .load(file)
                .into(binding.fullScreenImage)
        } else {
            Toast.makeText(this, "File not found or invalid path", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Кнопка удаления фотографии
        binding.deleteBtn.setOnClickListener {
            if (file != null && file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show()

                    // Возвращаем результат в GalleryActivity
                    val resultIntent = Intent()
                    resultIntent.putExtra("deletedFilePath", file.absolutePath)
                    setResult(RESULT_OK, resultIntent)

                    finish() // Закрываем полноэкранное окно
                } else {
                    Toast.makeText(this, "Failed to delete file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Кнопка возврата к галерее
        binding.galleryBtn.setOnClickListener {
            finish() // Закрываем текущую активность
        }
    }
}
