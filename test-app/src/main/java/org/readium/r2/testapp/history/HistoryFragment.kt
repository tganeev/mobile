package org.readium.r2.testapp.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentHistoryBinding
import org.readium.r2.testapp.ui.MenuVisibilityViewModel

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var adapter: HistoryAdapter
    private val menuViewModel: MenuVisibilityViewModel by activityViewModels()

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

        // Подключаем верхнее меню через MenuProvider
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_history, menu)
                // Блокируем кнопку, если уже идёт загрузка
                menu.findItem(R.id.action_restore_history)?.isEnabled = !viewModel.isLoading.value
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_restore_history -> {
                        // ✅ Запускаем suspend-функцию внутри корутины
                        lifecycleScope.launch {
                            viewModel.restoreFromServer()
                        }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)

        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        adapter = HistoryAdapter { record ->
            Snackbar.make(binding.root, "Книга: ${record.bookTitle}", Snackbar.LENGTH_SHORT).show()
        }
        binding.historyRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecycler.adapter = adapter
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.historyRecords.collect { records ->
                adapter.submitList(records)
                if (records.isEmpty()) {
                    binding.emptyText.visibility = View.VISIBLE
                    binding.historyRecycler.visibility = View.GONE
                } else {
                    binding.emptyText.visibility = View.GONE
                    binding.historyRecycler.visibility = View.VISIBLE
                }
            }
        }
        lifecycleScope.launch {
            viewModel.isLoading.collect { isLoading ->
                binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                // Обновляем состояние кнопки в меню
                requireActivity().invalidateOptionsMenu()
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

    override fun onResume() {
        super.onResume()
        menuViewModel.setMenuVisible(false)
    }

    override fun onPause() {
        super.onPause()
        menuViewModel.setMenuVisible(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}