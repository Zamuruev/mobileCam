package ru.rut.democamera

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import ru.rut.democamera.databinding.ListItemImageBinding
import java.io.File

class GalleryAdapter(
    private var fileArray: Array<File>,
    private val onFileDeleted: () -> Unit,
    private val onSelectionModeChanged: (Boolean) -> Unit // Добавляем коллбэк для отслеживания изменения режима
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    private val selectedFiles = mutableSetOf<File>() // Хранение выбранных файлов
    var isSelectionMode = false // Режим выбора файлов

    inner class ViewHolder(private val binding: ListItemImageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File, onDelete: (File) -> Unit) {
            // Загрузка изображения
            if (file.extension == "mp4") {
                binding.fileType.text = "Video"
                Glide.with(binding.root).load(file).thumbnail(0.1f).into(binding.localImg)
            } else {
                binding.fileType.text = "Photo"
                Glide.with(binding.root).load(file).into(binding.localImg)
            }

            // Обновление UI для режима выбора
            binding.selectionIndicator.visibility =
                if (isSelectionMode) View.VISIBLE else View.GONE

            binding.selectionIndicator.isChecked = selectedFiles.contains(file)

            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(file)
                } else {
                    val intent = Intent(binding.root.context, FullScreenActivity::class.java).apply {
                        putExtra("filePath", file.absolutePath)
                    }
                    (binding.root.context as Activity).startActivityForResult(intent, GalleryActivity.REQUEST_CODE)
                }
            }

            // Долгое нажатие для активации/деактивации режима выбора
            binding.root.setOnLongClickListener {
                if (isSelectionMode) {
                    // Закрыть режим выбора при повторном долгом нажатии
                    isSelectionMode = false
                    selectedFiles.clear()
                    onSelectionModeChanged(false) // Уведомить об изменении состояния
                    notifyDataSetChanged()
                } else {
                    // Включить режим выбора
                    isSelectionMode = true
                    toggleSelection(file)
                    onSelectionModeChanged(true) // Уведомить об изменении состояния
                    notifyDataSetChanged()
                }
                true
            }
        }

        private fun toggleSelection(file: File) {
            if (selectedFiles.contains(file)) {
                selectedFiles.remove(file)
            } else {
                selectedFiles.add(file)
            }
            notifyItemChanged(adapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ViewHolder(ListItemImageBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(fileArray[position]) {
            fileArray = fileArray.filterIndexed { index, _ -> index != position }.toTypedArray()
            notifyItemRemoved(position)
            onFileDeleted()
        }
    }

    override fun getItemCount(): Int = fileArray.size

    // Обновление файлов в адаптере
    fun updateFiles(updatedFiles: Array<File>) {
        fileArray = updatedFiles
        notifyDataSetChanged()
    }

    // Возвращает список выбранных файлов
    fun getSelectedFiles(): List<File> = selectedFiles.toList()

    // Очистить выбор
    fun clearSelection() {
        selectedFiles.clear()
        isSelectionMode = false
        onSelectionModeChanged(false)
        notifyDataSetChanged()
    }
}
