package org.readium.r2.testapp.alarm

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.readium.r2.testapp.R
import org.readium.r2.testapp.databinding.FragmentAlarmBinding

class AlarmFragment : Fragment() {

    private var _binding: FragmentAlarmBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlarmViewModel by viewModels()

    private var isUpdatingMorningFromCode = false
    private var isUpdatingEveningFromCode = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlarmBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTimePickers()
        setupListeners()
        observeViewModel()
        checkAlarmPermissions()
    }

    private fun setupTimePickers() {
        binding.morningTimePicker.setIs24HourView(true)
        binding.eveningTimePicker.setIs24HourView(true)
    }

    private fun setupListeners() {
        binding.morningAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingMorningFromCode) {
                viewModel.updateMorningAlarmEnabled(isChecked)
            }
        }

        binding.eveningAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingEveningFromCode) {
                viewModel.updateEveningAlarmEnabled(isChecked)
            }
        }

        binding.morningTimePicker.setOnTimeChangedListener { _, hour, minute ->
            if (!isUpdatingMorningFromCode) {
                viewModel.updateMorningTime(hour, minute)
            }
        }

        binding.eveningTimePicker.setOnTimeChangedListener { _, hour, minute ->
            if (!isUpdatingEveningFromCode) {
                viewModel.updateEveningTime(hour, minute)
            }
        }

        binding.historyButton.setOnClickListener {
            navigateToHistory()
        }


        binding.fixPermissionButton.setOnClickListener {
            openAlarmSettings()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.alarmPreferences.collect { prefs ->
                isUpdatingMorningFromCode = true
                binding.morningAlarmSwitch.isChecked = prefs.isMorningEnabled
                binding.morningTimePicker.hour = prefs.morningTime.hour
                binding.morningTimePicker.minute = prefs.morningTime.minute
                isUpdatingMorningFromCode = false

                isUpdatingEveningFromCode = true
                binding.eveningAlarmSwitch.isChecked = prefs.isEveningEnabled
                binding.eveningTimePicker.hour = prefs.eveningTime.hour
                binding.eveningTimePicker.minute = prefs.eveningTime.minute
                isUpdatingEveningFromCode = false
            }
        }

        lifecycleScope.launch {
            viewModel.toastMessage.collect { message ->
                if (message.isNotEmpty()) {
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                    viewModel.toastCleared()
                }
            }
        }
    }

    private fun checkAlarmPermissions() {
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                binding.permissionWarningCard.visibility = View.VISIBLE
                binding.permissionWarningText.text = """
                    Для точной работы будильника необходимо разрешение на планирование точных будильников.
                    Разрешите показ поверх других приложений для работы будильника на заблокированном экране.
                """.trimIndent()
                return
            }
        }

        // Проверка разрешения на игнорирование оптимизации батареи
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)) {
                binding.permissionWarningCard.visibility = View.VISIBLE
                binding.permissionWarningText.text = """
                    Для работы будильника в фоновом режиме необходимо отключить оптимизацию батареи для этого приложения.
                    Нажмите "Настроить" и выберите "Не оптимизировать".
                """.trimIndent()
                return
            }
        }

        binding.permissionWarningCard.visibility = View.GONE
    }

    private fun openAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            startActivity(intent)
        }
    }

    private fun navigateToHistory() {
        val navController = requireView().findNavController()
        navController.navigate(R.id.action_alarm_to_stats)
    }

    private fun showManualEntryDialog() {
        navigateToHistory()
        Snackbar.make(binding.root, "Нажмите на кнопку + в статистике", Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}