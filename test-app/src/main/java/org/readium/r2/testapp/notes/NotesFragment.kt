package org.readium.r2.testapp.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.Note
import org.readium.r2.testapp.databinding.FragmentNotesBinding
import org.readium.r2.testapp.utils.viewLifecycle
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesFragment : Fragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotesViewModel by viewModels()
    private lateinit var adapter: NotesAdapter

    // Лаунчер для экспорта (сохранение файла)
    private val saveFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            lifecycleScope.launch {
                try {
                    val contentResolver = requireContext().contentResolver
                    val outputStream = contentResolver.openOutputStream(uri)
                    outputStream?.let { stream ->
                        // Сохраняем JSON во временный файл, затем копируем
                        val tempFile = File(requireContext().cacheDir, "temp_export.json")
                        val result = viewModel.exportNotesToFile(tempFile)
                        when (result) {
                            is NotesViewModel.ExportResult.Success -> {
                                // Копируем содержимое в выбранный URI
                                tempFile.inputStream().use { input ->
                                    stream.use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                tempFile.delete()
                                Snackbar.make(
                                    binding.root,
                                    "Экспортировано заметок: ${result.count}",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                            is NotesViewModel.ExportResult.Error -> {
                                Snackbar.make(
                                    binding.root,
                                    "Ошибка: ${result.message}",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }
                    } ?: run {
                        Snackbar.make(
                            binding.root,
                            "Не удалось открыть файл для записи",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Snackbar.make(
                        binding.root,
                        "Ошибка экспорта: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    // Лаунчер для импорта (выбор файла)
    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            lifecycleScope.launch {
                try {
                    val contentResolver = requireContext().contentResolver
                    val inputStream = contentResolver.openInputStream(uri)
                    inputStream?.let { stream ->
                        // Сохраняем во временный файл
                        val tempFile = File(requireContext().cacheDir, "temp_import.json")
                        tempFile.outputStream().use { output ->
                            stream.copyTo(output)
                        }
                        stream.close()

                        val result = viewModel.importNotesFromFile(tempFile)
                        tempFile.delete()

                        when (result) {
                            is NotesViewModel.ImportResult.Success -> {
                                val message = if (result.imported == result.total) {
                                    "Импортировано ${result.imported} заметок"
                                } else {
                                    "Импортировано ${result.imported} из ${result.total} заметок (некоторые пропущены)"
                                }
                                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                            }
                            is NotesViewModel.ImportResult.Error -> {
                                Snackbar.make(
                                    binding.root,
                                    "Ошибка импорта: ${result.message}",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }
                    } ?: run {
                        Snackbar.make(
                            binding.root,
                            "Не удалось открыть файл для чтения",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Snackbar.make(
                        binding.root,
                        "Ошибка импорта: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = NotesAdapter(
            onNoteClick = { note -> showNoteDetailsDialog(note) },
            onNoteLongClick = { note -> showNoteOptionsDialog(note) }
        )
        binding.notesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.notesRecyclerView.adapter = adapter
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                viewModel.searchNotes(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty()) {
                    viewModel.loadAllNotes()
                } else {
                    viewModel.searchNotes(newText)
                }
                return true
            }
        })
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.notes.collect { notes ->
                adapter.submitList(notes)
                binding.emptyView.visibility = if (notes.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showNoteDetailsDialog(note: Note) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_note_details, null)

        val contentText = dialogView.findViewById<TextView>(R.id.noteContent)
        val myCommentInput = dialogView.findViewById<EditText>(R.id.noteMyComment)
        val bookTitleText = dialogView.findViewById<TextView>(R.id.noteBookTitle)
        val authorText = dialogView.findViewById<TextView>(R.id.noteAuthor)
        val categoryInput = dialogView.findViewById<EditText>(R.id.noteCategory)

        contentText.text = note.content
        myCommentInput.setText(note.myComment ?: "")
        bookTitleText.text = note.bookTitle
        authorText.text = note.bookAuthor ?: "—"
        categoryInput.setText(note.category)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Редактирование заметки")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val updatedNote = note.copy(
                    myComment = myCommentInput.text.toString().trim().ifEmpty { null },
                    category = categoryInput.text.toString().trim().ifEmpty { "Общее" }
                )
                lifecycleScope.launch {
                    viewModel.updateNote(updatedNote)
                    Snackbar.make(binding.root, "Заметка обновлена", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showNoteOptionsDialog(note: Note) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить заметку?")
            .setMessage("Заметка будет удалена без возможности восстановления")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteNote(note.id)
                    Snackbar.make(binding.root, "Заметка удалена", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_notes, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                performExport()
                true
            }
            R.id.action_import -> {
                performImport()
                true
            }
            R.id.action_delete_all -> {
                confirmDeleteAllNotes()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun performExport() {
        // Проверяем, есть ли заметки для экспорта
        lifecycleScope.launch {
            val notes = viewModel.notes.value
            if (notes.isEmpty()) {
                Snackbar.make(binding.root, "Нет заметок для экспорта", Snackbar.LENGTH_SHORT).show()
                return@launch
            }

            // Создаём имя файла с датой
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fileName = "pkms_notes_backup_${dateFormat.format(Date())}.json"

            // Запускаем сохранение
            saveFileLauncher.launch(fileName)
        }
    }

    private fun performImport() {
        // Запускаем выбор файла
        openFileLauncher.launch(arrayOf("application/json"))
    }

    private fun confirmDeleteAllNotes() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Удалить все заметки?")
            .setMessage("Все заметки будут удалены без возможности восстановления")
            .setPositiveButton("Удалить все") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteAllNotes()
                    Snackbar.make(binding.root, "Все заметки удалены", Snackbar.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}