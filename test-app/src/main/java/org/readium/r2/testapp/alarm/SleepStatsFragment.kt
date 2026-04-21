package org.readium.r2.testapp.alarm

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.SleepRecord
import org.readium.r2.testapp.databinding.FragmentSleepStatsBinding
import java.time.LocalDate
import java.time.LocalTime

class SleepStatsFragment : Fragment() {

    private var _binding: FragmentSleepStatsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlarmViewModel by viewModels()
    private lateinit var adapter: SleepRecordsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSleepStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireView().findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        adapter = SleepRecordsAdapter(
            onItemClick = { record ->
                showEditEntryDialog(record)  // Редактирование по клику
            },
            onItemLongClick = { record ->
                showDeleteConfirmDialog(record)  // Удаление по долгому нажатию
            }
        )
        binding.sleepRecordsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.sleepRecordsRecycler.adapter = adapter
    }

    private fun showEditEntryDialog(record: SleepRecord) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_sleep, null)
        val dateInput = dialogView.findViewById<EditText>(R.id.dateInput)
        val wakeTimeInput = dialogView.findViewById<EditText>(R.id.wakeTimeInput)
        val bedTimeInput = dialogView.findViewById<EditText>(R.id.bedTimeInput)

        // Заполняем существующими значениями
        dateInput.setText(record.date.toString())
        dateInput.isEnabled = false  // Дату менять нельзя

        wakeTimeInput.setText(record.wakeTime?.toString() ?: "")
        bedTimeInput.setText(record.bedTime?.toString() ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle("Редактировать запись за ${record.date}")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val wakeTimeStr = wakeTimeInput.text.toString().trim()
                val bedTimeStr = bedTimeInput.text.toString().trim()

                lifecycleScope.launch {
                    try {
                        val wakeTime = if (wakeTimeStr.isNotEmpty()) LocalTime.parse(wakeTimeStr) else null
                        val bedTime = if (bedTimeStr.isNotEmpty()) LocalTime.parse(bedTimeStr) else null

                        viewModel.updateSleepRecord(record.id, record.date, wakeTime, bedTime)
                        Snackbar.make(binding.root, "Запись обновлена", Snackbar.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Snackbar.make(binding.root, "Ошибка: ${e.message}", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun setupFab() {
        binding.fabAddRecord.setOnClickListener {
            showManualEntryDialog()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            viewModel.allSleepRecords.collect { records ->
                adapter.submitList(records)
                updateStatistics(records)
            }
        }
    }

    private fun updateStatistics(records: List<SleepRecord>) {
        var totalDuration = 0L
        var countWithBoth = 0

        for (i in records.indices) {
            val record = records[i]
            val bedTime = record.bedTime
            if (bedTime != null && i + 1 < records.size) {
                val nextRecord = records[i + 1]
                val wakeTime = nextRecord.wakeTime
                if (wakeTime != null) {
                    val duration = calculateDurationMinutes(bedTime, wakeTime)
                    if (duration > 0) {
                        totalDuration += duration
                        countWithBoth++
                    }
                }
            }
        }

        val avgDuration = if (countWithBoth > 0) totalDuration / countWithBoth else 0
        val avgHours = avgDuration / 60
        val avgMinutes = avgDuration % 60

        binding.avgDurationText.text = "${avgHours}ч ${avgMinutes}мин"
        binding.totalRecordsText.text = records.size.toString()
    }

    private fun calculateDurationMinutes(bedTime: LocalTime, wakeTime: LocalTime): Long {
        val bedMinutes = bedTime.hour * 60 + bedTime.minute
        val wakeMinutes = wakeTime.hour * 60 + wakeTime.minute

        return if (wakeMinutes >= bedMinutes) {
            (wakeMinutes - bedMinutes).toLong()
        } else {
            ((24 * 60 - bedMinutes) + wakeMinutes).toLong()
        }
    }

    private fun showManualEntryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_sleep, null)
        val dateInput = dialogView.findViewById<EditText>(R.id.dateInput)
        val wakeTimeInput = dialogView.findViewById<EditText>(R.id.wakeTimeInput)
        val bedTimeInput = dialogView.findViewById<EditText>(R.id.bedTimeInput)

        // Предзаполняем сегодняшней датой в формате yyyy-MM-dd
        val today = LocalDate.now()
        dateInput.setText(String.format("%04d-%02d-%02d", today.year, today.monthValue, today.dayOfMonth))

        // DatePicker по клику на поле даты
        dateInput.setOnClickListener {
            val datePicker = DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    val selectedDate = LocalDate.of(year, month + 1, day)
                    dateInput.setText(String.format("%04d-%02d-%02d", selectedDate.year, selectedDate.monthValue, selectedDate.dayOfMonth))
                },
                today.year,
                today.monthValue - 1,
                today.dayOfMonth
            )
            datePicker.show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Добавить запись")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val dateStr = dateInput.text.toString().trim()
                val wakeTimeStr = wakeTimeInput.text.toString().trim()
                val bedTimeStr = bedTimeInput.text.toString().trim()

                try {
                    // Парсим дату
                    val date = LocalDate.parse(dateStr)

                    // Парсим время подъёма (ожидаем формат HH:MM)
                    val wakeTime = if (wakeTimeStr.isNotEmpty()) {
                        val parts = wakeTimeStr.split(":")
                        if (parts.size == 2) {
                            LocalTime.of(parts[0].toInt(), parts[1].toInt())
                        } else {
                            null
                        }
                    } else {
                        null
                    }

                    // Парсим время отбоя (ожидаем формат HH:MM)
                    val bedTime = if (bedTimeStr.isNotEmpty()) {
                        val parts = bedTimeStr.split(":")
                        if (parts.size == 2) {
                            LocalTime.of(parts[0].toInt(), parts[1].toInt())
                        } else {
                            null
                        }
                    } else {
                        null
                    }

                    lifecycleScope.launch {
                        if (wakeTime != null) {
                            viewModel.saveWakeTimeManual(date, wakeTime)
                        }
                        if (bedTime != null) {
                            viewModel.saveBedTimeManual(date, bedTime)
                        }
                        Snackbar.make(binding.root, "Запись сохранена", Snackbar.LENGTH_SHORT).show()

                        // Очищаем поля
                        dateInput.setText("")
                        wakeTimeInput.setText("")
                        bedTimeInput.setText("")
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Snackbar.make(binding.root, "Ошибка: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDeleteConfirmDialog(record: SleepRecord) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удалить запись")
            .setMessage("Удалить запись за ${record.date}?")
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    viewModel.deleteSleepRecord(record.id)
                    Snackbar.make(binding.root, "Запись удалена", Snackbar.LENGTH_SHORT).show()
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