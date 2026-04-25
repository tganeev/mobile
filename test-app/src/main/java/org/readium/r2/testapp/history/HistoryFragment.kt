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

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var tableAdapter: HistoryTableAdapter

    private var showStats = true

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

        setupPeriodControls()
        setupSearch()
        setupStatsToggle()
        setupFocusHandling()
        setupObservers()

        viewModel.loadData()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_history, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync_history -> {
                syncWithServer()
                true
            }
            else -> super.onOptionsItemSelected(item)
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

        binding.calendarButton.setOnClickListener {
            Snackbar.make(binding.root, "Выбор периода будет добавлен позже", Snackbar.LENGTH_LONG).show()
        }
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

    private fun setupStatsToggle() {
        binding.statsToggleButton.setOnClickListener {
            showStats = !showStats
            updateStatsVisibility()

            if (showStats) {
                binding.statsToggleButton.text = "📊 Статистика"
                binding.statsToggleButton.icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_show_chart_24)
            } else {
                binding.statsToggleButton.text = "📊 Показать статистику"
                binding.statsToggleButton.icon = null
            }
        }
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
            showStats = showStats
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}