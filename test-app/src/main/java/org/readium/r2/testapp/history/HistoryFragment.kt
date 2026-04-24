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
import org.readium.r2.testapp.reader.ReaderActivityContract

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

        setupToolbar()
        setupPeriodControls()
        setupObservers()

        viewModel.loadData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
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

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.currentMonth.collect { month ->
                binding.periodTitle.text = viewModel.getFormattedMonth()
            }
        }

        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (!isLoading) {
                    // Убираем старые ссылки, которые могли вызвать ошибку
                    // tableScrollView больше не существует
                }
            }
        }

        lifecycleScope.launch {
            viewModel.tableData.collect { data ->
                if (data != null) {
                    if (data.books.isEmpty()) {
                        binding.emptyText.visibility = View.VISIBLE
                    } else {
                        binding.emptyText.visibility = View.GONE
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
                // Открываем книгу по клику на название
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