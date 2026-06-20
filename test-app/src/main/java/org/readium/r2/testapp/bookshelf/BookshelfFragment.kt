// файл: src/main/java/org/readium/r2/testapp/bookshelf/BookshelfFragment.kt

package org.readium.r2.testapp.bookshelf

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.readium.r2.shared.DelicateReadiumApi
import org.readium.r2.shared.util.AbsoluteUrl
import org.readium.r2.testapp.Application
import org.readium.r2.testapp.R
import org.readium.r2.testapp.backup.BackupContract
import org.readium.r2.testapp.backup.BackupManager
import org.readium.r2.testapp.backup.BackupSaveContract
import org.readium.r2.testapp.data.model.Book
import org.readium.r2.testapp.databinding.FragmentBookshelfBinding
import org.readium.r2.testapp.opds.GridAutoFitLayoutManager
import org.readium.r2.testapp.reader.ReaderActivityContract
import org.readium.r2.testapp.utils.LinkBookDialogFragment
import org.readium.r2.testapp.utils.viewLifecycle
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import androidx.activity.result.ActivityResultLauncher

import kotlinx.coroutines.Dispatchers  // <--- ДОБАВИТЬ
import kotlinx.coroutines.withContext  // <--- ДОБАВИТЬ
import android.app.ProgressDialog

class BookshelfFragment : Fragment() {

    private inner class OnViewAttachedListener : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
            app.readium.onLcpDialogAuthenticationParentAttached(view)
        }

        override fun onViewDetachedFromWindow(view: View) {
            app.readium.onLcpDialogAuthenticationParentDetached()
        }
    }

    private val bookshelfViewModel: BookshelfViewModel by activityViewModels()
    private lateinit var bookshelfAdapter: BookshelfAdapter
    private lateinit var appStoragePickerLauncher: ActivityResultLauncher<String>
    private lateinit var sharedStoragePickerLauncher: ActivityResultLauncher<Array<String>>
    private var binding: FragmentBookshelfBinding by viewLifecycle()
    private var onViewAttachedListener: OnViewAttachedListener = OnViewAttachedListener()

    private val app: Application
        get() = requireContext().applicationContext as Application

    // ===== НОВЫЕ ПОЛЯ ДЛЯ БЭКАПА =====
    private lateinit var backupManager: BackupManager

    private val exportBackupLauncher = registerForActivityResult(
        BackupSaveContract()
    ) { uri ->
        uri?.let { performExport(it) }
    }

    private val importBackupLauncher = registerForActivityResult(
        BackupContract()
    ) { uri ->
        uri?.let { performImport(it) }
    }
    // ===== КОНЕЦ НОВЫХ ПОЛЕЙ =====

    fun showEditBookDialog(book: Book) {
        val dialog = EditBookDialogFragment.newInstance(book)
        dialog.show(childFragmentManager, "EditBookDialog")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    fun performSync() {
        lifecycleScope.launch {
            val snackbar = Snackbar.make(
                requireView(),
                "Синхронизация...",
                Snackbar.LENGTH_INDEFINITE
            )
            snackbar.show()

            try {
                val result = app.syncManager.syncAllBooks()

                snackbar.dismiss()

                result.onSuccess { response ->
                    val message = buildString {
                        append("Синхронизация завершена:\n")
                        append("📚 Создано книг: ${response.booksCreated}\n")
                        append("🔄 Обновлено книг: ${response.booksUpdated}\n")
                        append("📊 Создано записей: ${response.statsCreated}\n")
                        append("🔄 Обновлено записей: ${response.statsUpdated}")
                    }
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
                }.onFailure { error ->
                    Snackbar.make(requireView(), "Ошибка: ${error.message}", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                snackbar.dismiss()
                Snackbar.make(requireView(), "Ошибка: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    // ===== НОВЫЕ МЕТОДЫ ДЛЯ БЭКАПА =====

    fun exportDatabase() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Экспорт базы данных")
            .setMessage("Будет создан архив со всеми вашими данными (книги, заметки, статистика, слова и т.д.).\n\nВыберите место для сохранения.")
            .setPositiveButton("Экспортировать") { _, _ ->
                exportBackupLauncher.launch(Unit)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    fun importDatabase() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Импорт базы данных")
            .setMessage("⚠️ ВНИМАНИЕ!\n\nИмпорт полностью ЗАМЕНИТ все текущие данные приложения (книги, заметки, статистику).\n\nВыберите файл бэкапа для восстановления.")
            .setPositiveButton("Импортировать") { _, _ ->
                importBackupLauncher.launch(Unit)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }
    private fun performExport(uri: Uri) {
        lifecycleScope.launch {
            val progressDialog = showProgressDialog("Экспорт данных...")

            try {
                // Используем callback подход
                backupManager.exportData(uri) { progress ->
                    // Обновляем UI в главном потоке
                    lifecycleScope.launch(Dispatchers.Main) {
                        progressDialog.setProgress(progress)
                    }
                }

                // Успешное завершение
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Snackbar.make(
                        requireView(),
                        "✅ Данные успешно экспортированы!",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Snackbar.make(
                        requireView(),
                        "❌ Ошибка экспорта: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun performImport(uri: Uri) {
        lifecycleScope.launch {
            val progressDialog = showProgressDialog("Импорт данных...")

            try {
                // Используем callback подход
                backupManager.importData(uri) { progress ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        progressDialog.setProgress(progress)
                    }
                }

                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()

                    // Перезагружаем список книг
                    lifecycleScope.launch {
                        bookshelfViewModel.books.collectLatest {
                            bookshelfAdapter.submitList(it)
                        }
                    }

                    Snackbar.make(
                        requireView(),
                        "✅ Данные успешно импортированы!",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressDialog.dismiss()
                    Snackbar.make(
                        requireView(),
                        "❌ Ошибка импорта: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                e.printStackTrace()
            }
        }
    }

    private fun showProgressDialog(title: String): android.app.ProgressDialog {
        return android.app.ProgressDialog(requireContext()).apply {
            setTitle(title)
            setMessage("Подождите...")
            setProgressStyle(android.app.ProgressDialog.STYLE_HORIZONTAL)
            max = 100
            setCancelable(false)
            show()
        }
    }

    // ===== КОНЕЦ НОВЫХ МЕТОДОВ =====

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentBookshelfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.addOnAttachStateChangeListener(onViewAttachedListener)

        bookshelfViewModel.channel.receive(viewLifecycleOwner) { handleEvent(it) }

        // Инициализируем BackupManager
        backupManager = BackupManager(requireContext())

        bookshelfAdapter = BookshelfAdapter(
            onBookClick = { book ->
                book.id?.let {
                    bookshelfViewModel.openPublication(it)
                }
            },
            onBookLongClick = { book -> confirmDeleteBook(book) }
        )
        bookshelfAdapter.setOnEditBookClick { book ->
            showEditBookDialog(book)
        }

        appStoragePickerLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    bookshelfViewModel.importPublicationFromStorage(it)
                }
            }

        sharedStoragePickerLauncher =
            registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                uri?.let {
                    val takeFlags: Int = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    requireContext().contentResolver.takePersistableUriPermission(uri, takeFlags)
                    bookshelfViewModel.addPublicationFromStorage(it)
                }
            }

        binding.bookshelfBookList.apply {
            setHasFixedSize(true)
            layoutManager = GridAutoFitLayoutManager(requireContext(), 120)
            adapter = bookshelfAdapter
            addItemDecoration(VerticalSpaceItemDecoration(10))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                bookshelfViewModel.books.collectLatest {
                    bookshelfAdapter.submitList(it)
                }
            }
        }

        binding.bookshelfAddBookFab.setOnClickListener {
            var selected = 0
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.add_book))
                .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                    dialog.cancel()
                }
                .setPositiveButton(getString(R.string.ok)) { _, _ ->
                    when (selected) {
                        0 -> appStoragePickerLauncher.launch("*/*")
                        1 -> sharedStoragePickerLauncher.launch(arrayOf("*/*"))
                        else -> askForRemoteUrl()
                    }
                }
                .setSingleChoiceItems(R.array.documentSelectorArray, 0) { _, which ->
                    selected = which
                }
                .show()
        }

        lifecycleScope.launch {
            app.bookshelf.channel.receiveAsFlow().collect { event ->
                handleBookshelfEvent(event)
            }
        }
    }

    @OptIn(DelicateReadiumApi::class)
    private fun askForRemoteUrl() {
        val urlEditText = EditText(requireContext())
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_book))
            .setMessage(R.string.enter_url)
            .setView(urlEditText)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                val url = AbsoluteUrl(urlEditText.text.toString())
                if (url == null || !URLUtil.isValidUrl(urlEditText.text.toString())) {
                    urlEditText.error = getString(R.string.invalid_url)
                    return@setPositiveButton
                }
                bookshelfViewModel.addPublicationFromWeb(url)
            }
            .show()
    }

    private fun handleEvent(event: BookshelfViewModel.Event) {
        when (event) {
            is BookshelfViewModel.Event.OpenPublicationError -> {
                event.error.toUserError().show(requireActivity())
            }
            is BookshelfViewModel.Event.LaunchReader -> {
                val intent = ReaderActivityContract().createIntent(
                    requireContext(),
                    event.arguments
                )
                startActivity(intent)
            }
        }
    }

    private fun handleBookshelfEvent(event: org.readium.r2.testapp.domain.Bookshelf.Event) {
        when (event) {
            is org.readium.r2.testapp.domain.Bookshelf.Event.ShowLinkDialog -> {
                val dialog = LinkBookDialogFragment.newInstance(event.existingBook)
                dialog.setOnLinkConfirmed {
                    lifecycleScope.launch {
                        app.bookshelf.attachFileToExistingBook(
                            serverIdentifier = event.existingBook.serverIdentifier ?: "",
                            href = event.newBookData.url.toString(),
                            cover = event.newBookData.coverFile.path,
                            mediaType = event.newBookData.format?.mediaType?.toString() ?: ""
                        )
                        app.bookshelf.refreshBooks()
                        lifecycleScope.launch {
                            bookshelfViewModel.books.collect {
                                bookshelfAdapter.submitList(it)
                            }
                        }
                    }
                    dialog.dismiss()
                }
                dialog.setOnCreateNewConfirmed {
                    lifecycleScope.launch {
                        app.bookshelf.addPublicationFromStorage(event.newBookData.url)
                    }
                    dialog.dismiss()
                }
                dialog.show(childFragmentManager, "LinkBookDialog")
            }
            else -> { /* другие события не обрабатываем */ }
        }
    }

    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) :
        RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect,
            view: View,
            parent: RecyclerView,
            state: RecyclerView.State,
        ) {
            outRect.bottom = verticalSpaceHeight
        }
    }

    private fun deleteBook(book: Book) {
        bookshelfViewModel.deletePublication(book)
    }

    private fun confirmDeleteBook(book: Book) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirm_delete_book_title))
            .setMessage(getString(R.string.confirm_delete_book_text))
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.cancel()
            }
            .setPositiveButton(getString(R.string.delete)) { dialog, _ ->
                deleteBook(book)
                dialog.dismiss()
            }
            .show()
    }
}