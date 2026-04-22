package org.readium.r2.testapp.alarm

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
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

        wakeTimeInput.hint = "ЧЧ:ММ (например 07:30)"
        bedTimeInput.hint = "ЧЧ:ММ (например 23:00)"

        dateInput.setText(record.date.toString())
        dateInput.isEnabled = false

        wakeTimeInput.setText(record.wakeTime?.let { String.format("%02d:%02d", it.hour, it.minute) } ?: "")
        bedTimeInput.setText(record.bedTime?.let { String.format("%02d:%02d", it.hour, it.minute) } ?: "")

        AlertDialog.Builder(requireContext())
            .setTitle("Редактировать запись за ${record.date}")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val wakeTimeStr = wakeTimeInput.text.toString().trim()
                val bedTimeStr = bedTimeInput.text.toString().trim()

                lifecycleScope.launch {
                    try {
                        val wakeTime = if (wakeTimeStr.isNotEmpty() && wakeTimeStr.matches(Regex("\\d{2}:\\d{2}"))) {
                            val parts = wakeTimeStr.split(":")
                            LocalTime.of(parts[0].toInt(), parts[1].toInt())
                        } else null

                        val bedTime = if (bedTimeStr.isNotEmpty() && bedTimeStr.matches(Regex("\\d{2}:\\d{2}"))) {
                            val parts = bedTimeStr.split(":")
                            LocalTime.of(parts[0].toInt(), parts[1].toInt())
                        } else null

                        viewModel.updateSleepRecord(record.id, record.date, wakeTime, bedTime)
                        Snackbar.make(binding.root, "Запись обновлена", Snackbar.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Snackbar.make(binding.root, "Ошибка: неверный формат времени. Используйте ЧЧ:ММ", Snackbar.LENGTH_LONG).show()
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

                // Прокручиваем к началу списка (к самой свежей записи)
                binding.sleepRecordsRecycler.scrollToPosition(0)
            }
        }
    }

    private fun updateStatistics(records: List<SleepRecord>) {
        var totalDuration = 0L
        var countWithBoth = 0

        val sortedOldToNew = records.sortedBy { it.date }

        for (i in 0 until sortedOldToNew.size - 1) {
            val currentRecord = sortedOldToNew[i]
            val nextRecord = sortedOldToNew[i + 1]

            val bedTime = currentRecord.bedTime
            val wakeTime = nextRecord.wakeTime

            if (bedTime != null && wakeTime != null) {
                val bedMinutes = bedTime.hour * 60 + bedTime.minute
                val wakeMinutes = wakeTime.hour * 60 + wakeTime.minute

                val durationMinutes = if (wakeMinutes >= bedMinutes) {
                    wakeMinutes - bedMinutes
                } else {
                    (24 * 60 - bedMinutes) + wakeMinutes
                }

                if (durationMinutes > 0 && durationMinutes < 24 * 60) {
                    totalDuration += durationMinutes
                    countWithBoth++
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

        val today = LocalDate.now()
        dateInput.setText(String.format("%04d-%02d-%02d", today.year, today.monthValue, today.dayOfMonth))

        // Очищаем hint и устанавливаем пустой текст
        wakeTimeInput.setText("")
        bedTimeInput.setText("")

        // Убираем hint, когда получаем фокус
        wakeTimeInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && wakeTimeInput.text.toString() == "--:--") {
                wakeTimeInput.setText("")
            }
        }

        bedTimeInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && bedTimeInput.text.toString() == "--:--") {
                bedTimeInput.setText("")
            }
        }

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
                    val date = LocalDate.parse(dateStr)

                    val wakeTime = if (wakeTimeStr.isNotEmpty() && wakeTimeStr.matches(Regex("\\d{2}:\\d{2}"))) {
                        val parts = wakeTimeStr.split(":")
                        LocalTime.of(parts[0].toInt(), parts[1].toInt())
                    } else null

                    val bedTime = if (bedTimeStr.isNotEmpty() && bedTimeStr.matches(Regex("\\d{2}:\\d{2}"))) {
                        val parts = bedTimeStr.split(":")
                        LocalTime.of(parts[0].toInt(), parts[1].toInt())
                    } else null

                    if (wakeTime == null && bedTime == null) {
                        Snackbar.make(binding.root, "Укажите хотя бы одно время в формате ЧЧ:ММ", Snackbar.LENGTH_LONG).show()
                        return@setPositiveButton
                    }

                    lifecycleScope.launch {
                        if (wakeTime != null) {
                            viewModel.saveWakeTimeManual(date, wakeTime)
                        }
                        if (bedTime != null) {
                            viewModel.saveBedTimeManual(date, bedTime)
                        }
                        Snackbar.make(binding.root, "Запись сохранена", Snackbar.LENGTH_SHORT).show()
                    }

                } catch (e: Exception) {
                    Snackbar.make(binding.root, "Ошибка: неверный формат", Snackbar.LENGTH_LONG).show()
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