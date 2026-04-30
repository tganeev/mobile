package org.readium.r2.testapp.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentHistoryBinding
import org.readium.r2.testapp.reader.ReaderActivityContract
import java.time.format.DateTimeFormatter
import java.util.Calendar
import android.app.DatePickerDialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.ViewTreeObserver
import androidx.core.graphics.drawable.DrawableCompat
import java.time.LocalDate
import java.time.YearMonth

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var tableAdapter: HistoryTableAdapter
    private var showStats = false
    private var isSearchVisible = false

    // Слушатель для удаления в onDestroyView
    private var scrollChangeListener: ViewTreeObserver.OnScrollChangedListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        binding.searchLayout.visibility = View.GONE

        setupPeriodControls()
        setupSearch()

        setupFocusHandling()
        setupObservers()
        setupScrollViewSync()
        viewModel.loadData()
    }

    private fun setupScrollViewSync() {
        // Создаём слушатель с проверкой _binding
        scrollChangeListener = ViewTreeObserver.OnScrollChangedListener {
            // Безопасное обращение: если binding уже null, просто выходим
            _binding?.let { b ->
                b.headerHorizontalScrollView.scrollTo(b.tableScrollView.scrollX, 0)
            }
        }
        binding.tableScrollView.viewTreeObserver.addOnScrollChangedListener(scrollChangeListener!!)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_history, menu)

        // Окрашиваем все иконки меню в @color/purple_501
        val targetColor = ContextCompat.getColor(requireContext(), R.color.white)
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            val originalIcon: Drawable? = item.icon
            originalIcon?.let { icon ->
                val wrappedIcon = DrawableCompat.wrap(icon.mutate())
                DrawableCompat.setTint(wrappedIcon, targetColor)
                item.icon = wrappedIcon
            }
        }

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_select_date -> {
                showDateRangePicker()
                true
            }

            R.id.action_toggle_stats -> {
                showStats = !showStats
                updateStatsVisibility()
                // Опционально: можно менять иконку или показывать подсказку
                // item.title = if (showStats) "Скрыть статистику" else "Показать статистику"
                true
            }

            R.id.action_search -> {
                toggleSearchVisibility()
                true
            }
            R.id.action_sync_history -> {
                syncWithServer()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun toggleSearchVisibility() {
        isSearchVisible = !isSearchVisible
        binding.searchLayout.visibility = if (isSearchVisible) View.VISIBLE else View.GONE

        if (isSearchVisible) {
            // Показываем клавиатуру и фокусируемся на поле поиска
            binding.searchInput.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.searchInput, InputMethodManager.SHOW_IMPLICIT)
        } else {
            // Скрываем клавиатуру и очищаем поиск
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
            binding.searchInput.text?.clear()
            viewModel.updateSearchQuery("")
        }
    }

    private fun syncWithServer() {
        lifecycleScope.launch {
            val snackbar = Snackbar.make(binding.root, "Синхронизация с сервером...", Snackbar.LENGTH_INDEFINITE)
            snackbar.show()
            try {
                val app = requireContext().applicationContext as org.readium.r2.testapp.Application
                val result = app.syncManager.syncHistoryFromServer()
                snackbar.dismiss()
                result.onSuccess { data ->
                    val message = "Синхронизация завершена: загружено ${data.books.size} книг, ${data.readingStats.size} записей"
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    viewModel.loadData()
                }.onFailure { error ->
                    Snackbar.make(binding.root, "Ошибка: ${error.message}", Snackbar.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                snackbar.dismiss()
                Snackbar.make(binding.root, "Ошибка: ${e.message}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setupPeriodControls() {
        binding.prevMonthButton.setOnClickListener {
            viewModel.previousMonth()
        }
        binding.nextMonthButton.setOnClickListener {
            viewModel.nextMonth()
        }

    }

    private fun showDateRangePicker() {
        val currentStart = viewModel.currentStartDate
        val currentEnd = viewModel.currentEndDate
        val calendar = Calendar.getInstance()
        calendar.set(currentStart.year, currentStart.monthValue - 1, currentStart.dayOfMonth)
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val startDate = LocalDate.of(year, month + 1, day)
                showEndDatePicker(startDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Выберите начальную дату")
        }.show()
    }

    private fun showEndDatePicker(startDate: LocalDate) {
        val currentEnd = viewModel.currentEndDate
        val calendar = Calendar.getInstance()
        calendar.set(currentEnd.year, currentEnd.monthValue - 1, currentEnd.dayOfMonth)
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val endDate = LocalDate.of(year, month + 1, day)
                if (endDate.isBefore(startDate)) {
                    Snackbar.make(binding.root, "Конечная дата не может быть раньше начальной", Snackbar.LENGTH_LONG).show()
                } else {
                    viewModel.loadDataForRange(startDate, endDate)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle("Выберите конечную дату")
        }.show()
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateSearchQuery(s?.toString() ?: "")
            }
        })
        binding.searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                hideKeyboardAndClearFocus()
                true
            } else false
        }
    }

    private fun setupFocusHandling() {
        binding.root.setOnTouchListener { _, _ ->
            hideKeyboardAndClearFocus()
            false
        }
        binding.searchInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                hideKeyboardAndClearFocus()
            }
        }
        binding.searchInput.setOnClickListener {
            binding.searchInput.isCursorVisible = true
            binding.searchInput.requestFocus()
        }
    }

    private fun hideKeyboardAndClearFocus() {
        val imm = requireContext().getSystemService(android.app.Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.searchInput.windowToken, 0)
        binding.searchInput.clearFocus()
        binding.searchInput.isCursorVisible = false
    }


    private fun updateStatsVisibility() {
        viewModel.filteredTableData.value?.let { data ->
            renderTable(data, showStats)
        }
    }



    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.periodRange.collect { range ->
                val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                val periodText = "${range.first.format(dateFormatter)} - ${range.second.format(dateFormatter)}"
                binding.periodTitle.text = periodText
            }
        }
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }
        lifecycleScope.launch {
            viewModel.filteredTableData.collect { data ->
                if (data != null) {
                    if (data.books.isEmpty()) {
                        binding.emptyText.visibility = View.VISIBLE
                        binding.emptyText.text = if (viewModel.searchQuery.value.isBlank()) {
                            "Нет данных за выбранный период"
                        } else {
                            "Книги не найдены"
                        }
                        binding.tableScrollView.visibility = View.GONE
                    } else {
                        binding.emptyText.visibility = View.GONE
                        binding.tableScrollView.visibility = View.VISIBLE
                        renderTable(data, showStats)
                    }
                }
            }
        }
        lifecycleScope.launch {
            viewModel.errorMessage.collect { error ->
                if (error != null) {
                    Snackbar.make(binding.root, error, Snackbar.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun renderTable(data: HistoryTableData, showStats: Boolean) {
        if (!::tableAdapter.isInitialized) {
            tableAdapter = HistoryTableAdapter { bookId ->
                val intent = ReaderActivityContract().createIntent(
                    requireContext(),
                    ReaderActivityContract.Arguments(bookId)
                )
                startActivity(intent)
            }
        }
        tableAdapter.setData(
            data = data,
            fixedContainer = binding.fixedColumnContainer,
            dynamicContainer = binding.dynamicColumnsContainer,
            headerFixedContainer = binding.headerFixedColumnContainer,
            headerDynamicContainer = binding.headerDynamicColumnsContainer,
            showStats = showStats
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 🔧 ВАЖНО: Удаляем слушатель скролла перед обнулением binding
        scrollChangeListener?.let { listener ->
            _binding?.tableScrollView?.viewTreeObserver?.removeOnScrollChangedListener(listener)
        }
        scrollChangeListener = null
        _binding = null
    }
}