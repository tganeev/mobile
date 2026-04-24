package org.readium.r2.testapp.history

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    // Флаг для отображения/скрытия статистики
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

        setupPeriodControls()
        setupSearch()
        setupStatsToggle()
        setupObservers()

        viewModel.loadData()
    }

    private fun setupPeriodControls() {
        binding.prevMonthButton.setOnClickListener {
            viewModel.previousMonth()
        }

        binding.nextMonthButton.setOnClickListener {
            viewModel.nextMonth()
        }

        binding.calendarButton.setOnClickListener {
            // TODO: Выбор произвольного периода
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
    }

    private fun setupStatsToggle() {
        binding.statsToggleButton.setOnClickListener {
            showStats = !showStats
            updateStatsVisibility()

            // Меняем текст и иконку кнопки
            if (showStats) {
                binding.statsToggleButton.text = "📊 Статистика"
                binding.statsToggleButton.icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_baseline_show_chart_24)
            } else {
                binding.statsToggleButton.text = "📊 Показать статистику"
                binding.statsToggleButton.icon = null
            }
        }
    }

    private fun updateStatsVisibility() {
        // Перерисовываем таблицу с учётом флага showStats
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