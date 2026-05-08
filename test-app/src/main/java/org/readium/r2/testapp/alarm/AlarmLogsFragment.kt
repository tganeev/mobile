package org.readium.r2.testapp.alarm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentAlarmLogsBinding

class AlarmLogsFragment : Fragment() {

    private var _binding: FragmentAlarmLogsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AlarmLogsViewModel by viewModels()
    private lateinit var adapter: AlarmLogsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmLogsBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        observeData()
    }



    private fun setupRecyclerView() {
        adapter = AlarmLogsAdapter()
        binding.logsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.logsRecyclerView.adapter = adapter
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.logs.collect { logs ->
                adapter.submitList(logs)
                binding.emptyView.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_alarm_logs, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_logs -> {
                confirmClearLogs()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun confirmClearLogs() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Очистить логи")
            .setMessage("Все логи будут удалены без возможности восстановления")
            .setPositiveButton("Очистить") { _, _ ->
                lifecycleScope.launch {
                    viewModel.clearAllLogs()
                    Snackbar.make(binding.root, "Логи очищены", Snackbar.LENGTH_SHORT).show()
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