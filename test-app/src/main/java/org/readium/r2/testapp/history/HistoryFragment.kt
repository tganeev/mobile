package org.readium.r2.testapp.history

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
            // TODO: Добавить выбор произвольного периода
            Snackbar.make(binding.root, "Выбор периода будет добавлен позже", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.currentMonth.collect { month ->
                binding.periodTitle.text = viewModel.getFormattedMonth()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                binding.tableScrollView.visibility = if (isLoading) View.GONE else View.VISIBLE
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
    }

    private fun renderTable(data: HistoryTableData) {
        if (!::tableAdapter.isInitialized) {
            tableAdapter = HistoryTableAdapter { bookId ->
                // TODO: Открыть книгу
                Snackbar.make(binding.root, "Книга ID: $bookId", Snackbar.LENGTH_SHORT).show()
            }
        }

        tableAdapter.setData(
            data = data,
            fixedContainer = binding.fixedColumnsContainer,
            dynamicContainer = binding.dynamicColumnsContainer
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}