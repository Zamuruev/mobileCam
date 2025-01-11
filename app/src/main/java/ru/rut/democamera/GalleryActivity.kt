package ru.rut.democamera

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import ru.rut.democamera.databinding.ActivityGalleryBinding
import java.io.File

class GalleryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityGalleryBinding
    private lateinit var adapter: GalleryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGalleryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Устанавливаем отступ для Toolbar, чтобы он отображался под статус-баром
        binding.toolbar.apply {
            post {
                val statusBarHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    window.decorView.rootWindowInsets?.getInsets(WindowInsets.Type.statusBars())?.top ?: 0
                } else {
                    resources.getIdentifier("status_bar_height", "dimen", "android").let {
                        if (it > 0) resources.getDimensionPixelSize(it) else 0
                    }
                }
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    topMargin = statusBarHeight
                }
            }
        }

        // Настройка Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)  // Показываем кнопку "Назад"

        // Получаем файлы из галереи
        val directory = File(externalMediaDirs[0].absolutePath)
        val files = directory.listFiles()?.reversedArray() ?: emptyArray()

        // Передаем логику обновления кнопки удаления в адаптер
        adapter = GalleryAdapter(files, {
            val updatedFiles = directory.listFiles()?.reversedArray() ?: emptyArray()
            adapter.updateFiles(updatedFiles)
        }, { isSelectionActive ->
            updateDeleteButtonVisibility(isSelectionActive)
        })

        binding.recyclerView.layoutManager = GridLayoutManager(this, 3)  // 3 столбца
        binding.recyclerView.adapter = adapter

        // Кнопка удаления выбранных файлов
        binding.deleteButton.setOnClickListener {
            val selectedFiles = adapter.getSelectedFiles()
            for (file in selectedFiles) {
                file.delete()
            }
            adapter.clearSelection()
            val updatedFiles = directory.listFiles()?.reversedArray() ?: emptyArray()
            adapter.updateFiles(updatedFiles)
        }

        // Изначально кнопка "Удалить выбранное" должна быть скрыта
        updateDeleteButtonVisibility(false) // Начальное состояние кнопки
    }

    private fun updateDeleteButtonVisibility(isVisible: Boolean) {
        // Если в выбранных файлах нет элементов, кнопка должна быть невидимой
        binding.deleteButton.visibility = if (isVisible) View.VISIBLE else View.INVISIBLE
    }

    // Обновляем состояние кнопки при возвращении в активность
    override fun onResume() {
        super.onResume()
        updateDeleteButtonVisibility(adapter.getSelectedFiles().isNotEmpty()) // Обновляем видимость кнопки удаления при возвращении
    }

    // Обработка нажатия кнопки "Назад" (на ActionBar)
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (adapter.isSelectionMode) {
                    adapter.clearSelection()  // Выход из режима выбора
                    updateDeleteButtonVisibility(false) // Скрыть кнопку
                } else {
                    val intent = Intent(this, PhotoStartActivity::class.java)
                    startActivity(intent)  // Запуск MainActivity
                    finish()  // Закрытие текущей активности
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Обработка результата удаления файла из FullScreenActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE) {
            val deletedFilePath = data?.getStringExtra("deletedFilePath")
            if (deletedFilePath != null) {
                // Обновляем список файлов после удаления
                val directory = File(externalMediaDirs[0].absolutePath)
                val updatedFiles = directory.listFiles()?.reversedArray() ?: emptyArray()
                adapter.updateFiles(updatedFiles)
            }
        }
    }

    companion object {
        const val REQUEST_CODE = 1
    }
}
