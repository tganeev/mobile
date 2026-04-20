package org.readium.r2.testapp.alarm

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
        // Устанавливаем режим спиннера для TimePicker (более удобный)
        binding.morningTimePicker.setIs24HourView(true)
        binding.eveningTimePicker.setIs24HourView(true)
    }

    private fun setupListeners() {
        binding.morningAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateMorningAlarmEnabled(isChecked)
        }

        binding.eveningAlarmSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateEveningAlarmEnabled(isChecked)
        }

        binding.morningTimePicker.setOnTimeChangedListener { _, hour, minute ->
            viewModel.updateMorningTime(hour, minute)
        }

        binding.eveningTimePicker.setOnTimeChangedListener { _, hour, minute ->
            viewModel.updateEveningTime(hour, minute)
        }

        binding.historyButton.setOnClickListener {
            navigateToHistory()
        }

        binding.manualEntryButton.setOnClickListener {
            showManualEntryDialog()
        }

        binding.fixPermissionButton.setOnClickListener {
            openAlarmSettings()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.alarmPreferences.collect { prefs ->
                binding.morningAlarmSwitch.isChecked = prefs.isMorningEnabled
                binding.eveningAlarmSwitch.isChecked = prefs.isEveningEnabled

                binding.morningTimePicker.hour = prefs.morningTime.hour
                binding.morningTimePicker.minute = prefs.morningTime.minute

                binding.eveningTimePicker.hour = prefs.eveningTime.hour
                binding.eveningTimePicker.minute = prefs.eveningTime.minute
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
                    Это позволит будильнику срабатывать даже в режиме энергосбережения.
                """.trimIndent()
            } else {
                binding.permissionWarningCard.visibility = View.GONE
            }
        } else {
            binding.permissionWarningCard.visibility = View.GONE
        }
    }

    private fun openAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            startActivity(intent)
        }
    }

    private fun navigateToHistory() {
        // TODO: Реализовать в Спринте 5
        Snackbar.make(binding.root, "История пока в разработке", Snackbar.LENGTH_SHORT).show()
    }

    private fun showManualEntryDialog() {
        // TODO: Реализовать в Спринте 5
        Snackbar.make(binding.root, "Ручной ввод пока в разработке", Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}