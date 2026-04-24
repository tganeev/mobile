package org.readium.r2.testapp.history

import android.app.DatePickerDialog
import android.os.Bundle
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var tableAdapter: HistoryTableAdapter

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
            showDateRangePicker()
        }
    }

    private fun showDateRangePicker() {
        val currentStart = viewModel.currentStartDate
        val currentEnd = viewModel.currentEndDate

        // Создаём диалог выбора начальной даты
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
            viewModel.tableData.collect { data ->
                if (data != null) {
                    if (data.books.isEmpty()) {
                        binding.emptyText.visibility = View.VISIBLE
                        binding.tableScrollView.visibility = View.GONE
                    } else {
                        binding.emptyText.visibility = View.GONE
                        binding.tableScrollView.visibility = View.VISIBLE
                        renderTable(data)
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
                        renderTable(data)
                    }
                }
            }
        }
    }

    private fun setupSearch() {
        binding.searchInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                viewModel.updateSearchQuery(s?.toString() ?: "")
            }
        })
    }


    private fun renderTable(data: HistoryTableData) {
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
            dynamicContainer = binding.dynamicColumnsContainer
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}