package org.readium.r2.testapp.yoga

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.YogaPractices
import org.readium.r2.testapp.databinding.FragmentYogaBinding

class YogaFragment : Fragment() {

    private var _binding: FragmentYogaBinding? = null
    private val binding get() = _binding!!
    private val viewModel: YogaViewModel by viewModels()
    private lateinit var activeTimersAdapter: ActiveTimersAdapter

    // Храним состояние каждого таймера по его ID
    private val timerStates = mutableMapOf<String, TimerState>()

    private var currentSelectedPracticeId: String? = null
    private var isCenterTimerRunning = false
    private var centerTimerRemainingSeconds: Long = 31 * 60L

    // Job для корутины обновления
    private var timerUpdateJob: Job? = null

    data class TimerState(
        var remainingSeconds: Long,
        var isRunning: Boolean,
        var practiceId: String
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentYogaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupCircularMenu()
        observeViewModel()
        setupTimerControls()

        // Запускаем корутину для ритмичного обновления таймеров
        startTimerUpdater()

        // Выбираем Шаматху по умолчанию для центра
        val shamatha = YogaPractices.practices.find { it.name == "Шаматха" }
        currentSelectedPracticeId = shamatha?.id
        updateSelectedTimerDisplay()
    }

    private fun startTimerUpdater() {
        timerUpdateJob?.cancel()

        timerUpdateJob = lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                updateAllTimers()
            }
        }
    }

    private fun updateAllTimers() {
        var hasChanges = false

        // Обновляем центральный таймер
        if (isCenterTimerRunning && centerTimerRemainingSeconds > 0) {
            centerTimerRemainingSeconds--
            updateCenterTimerDisplay()
            hasChanges = true
        } else if (centerTimerRemainingSeconds == 0L && isCenterTimerRunning) {
            isCenterTimerRunning = false
            binding.circularMenuView.setCenterTimerRunning(false)
            hasChanges = true
        }

        // Обновляем все активные таймеры
        val updatedTimers = mutableListOf<ActiveTimer>()
        timerStates.forEach { (id, state) ->
            if (state.isRunning && state.remainingSeconds > 0) {
                state.remainingSeconds--
                hasChanges = true
            }

            val practice = YogaPractices.practices.find { it.id == state.practiceId }
            practice?.let {
                updatedTimers.add(
                    ActiveTimer(
                        id = id,
                        practice = it,
                        remainingSeconds = state.remainingSeconds,
                        isRunning = state.isRunning
                    )
                )
            }
        }

        if (hasChanges) {
            activeTimersAdapter.updateTimers(updatedTimers)
            updateSelectedTimerDisplay()

            updatedTimers.forEach { timer ->
                binding.circularMenuView.updateItemRemainingTime(
                    timer.practice.id,
                    timer.remainingSeconds
                )
            }
        }
    }

    private fun setupRecyclerView() {
        activeTimersAdapter = ActiveTimersAdapter(
            onPause = { timer ->
                timerStates[timer.id]?.isRunning = false
            },
            onResume = { timer ->
                timerStates[timer.id]?.isRunning = true
            },
            onStop = { timer ->
                timerStates.remove(timer.id)
                viewModel.stopTimer(timer.id)
                updateSelectedTimerDisplay()
                binding.circularMenuView.updateItemRemainingTime(timer.practice.id, timer.practice.defaultDurationMinutes * 60L)
            }
        )
        binding.activeTimersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.activeTimersRecyclerView.adapter = activeTimersAdapter
    }

    private fun setupCircularMenu() {
        val circularMenu = binding.circularMenuView

        val menuItems = YogaPractices.practices.map { practice ->
            CircularMenuView.CircularMenuItem(
                id = practice.id,
                name = practice.name,
                color = android.graphics.Color.parseColor(practice.color),
                durationMinutes = practice.defaultDurationMinutes,
                remainingSeconds = (practice.defaultDurationMinutes * 60L),
                isRunning = false
            )
        }

        circularMenu.setItems(menuItems)
        circularMenu.setCenterTimer("Шаматха", 31)

        // Обработчик центрального круга
        circularMenu.setOnCenterTimerClickListener {
            if (isCenterTimerRunning) {
                pauseCenterTimer()
            } else {
                startCenterTimer()
            }
        }

        // Обработчик внешних кругов - запуск таймера при нажатии
        circularMenu.setOnTimerClickListener { item, index ->
            val practice = YogaPractices.practices.find { it.id == item.id }
            practice?.let {
                // Переключаемся на выбранную практику
                currentSelectedPracticeId = practice.id
                // Запускаем таймер сразу при нажатии на внешний круг
                startTimerFromCircle(practice)
                // Обновляем отображение в панели управления
                updateSelectedTimerDisplay()
            }
        }
    }

    private fun startTimerFromCircle(practice: org.readium.r2.testapp.data.model.YogaPractice) {
        val timerId = practice.id
        val existingState = timerStates[timerId]

        val remaining = if (existingState != null && existingState.remainingSeconds > 0 && existingState.remainingSeconds < (practice.defaultDurationMinutes * 60L)) {
            existingState.remainingSeconds
        } else {
            (practice.defaultDurationMinutes * 60L).toLong()
        }

        timerStates[timerId] = TimerState(
            remainingSeconds = remaining,
            isRunning = true,
            practiceId = practice.id
        )

        // Обновляем отображение
        updateSelectedTimerDisplay()

        // Запускаем таймер в ViewModel
        viewModel.startTimer(practice, remaining)

        // Обновляем круговое меню
        binding.circularMenuView.updateItemRemainingTime(practice.id, remaining)

        // Показываем уведомление о запуске
        Toast.makeText(
            requireContext(),
            "Таймер ${practice.name} запущен!",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                state.activeTimers.forEach { activeTimer ->
                    if (!timerStates.containsKey(activeTimer.id)) {
                        timerStates[activeTimer.id] = TimerState(
                            remainingSeconds = activeTimer.remainingSeconds,
                            isRunning = activeTimer.isRunning,
                            practiceId = activeTimer.practice.id
                        )
                    }
                }

                val currentIds = state.activeTimers.map { it.id }.toSet()
                timerStates.keys.removeAll { it !in currentIds }

                val timersList = timerStates.map { (id, state) ->
                    val practice = YogaPractices.practices.find { it.id == state.practiceId }!!
                    ActiveTimer(
                        id = id,
                        practice = practice,
                        remainingSeconds = state.remainingSeconds,
                        isRunning = state.isRunning
                    )
                }
                activeTimersAdapter.updateTimers(timersList)
            }
        }
    }

    private fun setupTimerControls() {
        binding.startTimerButton.setOnClickListener {
            startTimer()
        }

        binding.pauseTimerButton.setOnClickListener {
            pauseTimer()
        }

        binding.stopTimerButton.setOnClickListener {
            stopTimer()
        }
    }

    private fun startCenterTimer() {
        isCenterTimerRunning = true
        centerTimerRemainingSeconds = 31 * 60L
        binding.circularMenuView.setCenterTimerRunning(true)

        val shamatha = YogaPractices.practices.find { it.name == "Шаматха" }
        shamatha?.let {
            val timerId = "center_${shamatha.id}"
            if (!timerStates.containsKey(timerId)) {
                timerStates[timerId] = TimerState(
                    remainingSeconds = centerTimerRemainingSeconds,
                    isRunning = true,
                    practiceId = shamatha.id
                )
            } else {
                timerStates[timerId]?.remainingSeconds = centerTimerRemainingSeconds
                timerStates[timerId]?.isRunning = true
            }
            viewModel.startTimer(shamatha, centerTimerRemainingSeconds)
        }
    }

    private fun pauseCenterTimer() {
        isCenterTimerRunning = false
        binding.circularMenuView.setCenterTimerRunning(false)
    }

    private fun startTimer() {
        val practice = YogaPractices.practices.find { it.id == currentSelectedPracticeId }
        practice ?: return

        val timerId = practice.id
        val existingState = timerStates[timerId]

        val remaining = if (existingState != null && existingState.remainingSeconds > 0 && existingState.remainingSeconds < (practice.defaultDurationMinutes * 60L)) {
            existingState.remainingSeconds
        } else {
            (practice.defaultDurationMinutes * 60L).toLong()
        }

        timerStates[timerId] = TimerState(
            remainingSeconds = remaining,
            isRunning = true,
            practiceId = practice.id
        )

        binding.startTimerButton.visibility = View.GONE
        binding.timerControlsLayout.visibility = View.VISIBLE

        viewModel.startTimer(practice, remaining)
        binding.circularMenuView.updateItemRemainingTime(practice.id, remaining)
    }

    private fun pauseTimer() {
        val practice = YogaPractices.practices.find { it.id == currentSelectedPracticeId }
        practice ?: return

        timerStates[practice.id]?.isRunning = false

        binding.startTimerButton.visibility = View.VISIBLE
        binding.timerControlsLayout.visibility = View.GONE
    }

    private fun stopTimer() {
        val practice = YogaPractices.practices.find { it.id == currentSelectedPracticeId }
        practice ?: return

        timerStates.remove(practice.id)
        viewModel.stopTimer(practice.id)

        binding.startTimerButton.visibility = View.VISIBLE
        binding.timerControlsLayout.visibility = View.GONE
        binding.circularMenuView.updateItemRemainingTime(practice.id, practice.defaultDurationMinutes * 60L)
    }

    private fun updateSelectedTimerDisplay() {
        val practice = YogaPractices.practices.find { it.id == currentSelectedPracticeId }
        if (practice != null) {
            // Обновляем подсветку чакр в зависимости от выбранной практики
            highlightChakra(practice.name)

            val state = timerStates[practice.id]
            val remaining = state?.remainingSeconds ?: (practice.defaultDurationMinutes * 60L).toLong()
            val minutes = remaining / 60
            val seconds = remaining % 60
            binding.timerDisplayText.text = String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun highlightChakra(practiceName: String) {
        // Здесь можно реализовать подсветку определённой чакры
        // в зависимости от выбранной практики
        when (practiceName) {
            "Випашьяна" -> {
                // Подсветка коронной чакры (фиолетовая)
            }
            "Кумбхака" -> {
                // Подсветка горловой чакры (голубая)
            }
            "Визуализация" -> {
                // Подсветка чакры третьего глаза (индиго)
            }
            "Шаматха" -> {
                // Подсветка сердечной чакры (зелёная)
            }
            "Концентрация" -> {
                // Подсветка чакры солнечного сплетения (жёлтая)
            }
            "Пранаяма" -> {
                // Подсветка сакральной чакры (оранжевая)
            }
            "Мантра" -> {
                // Подсветка горловой чакры (голубая)
            }
            "Экадаш" -> {
                // Подсветка корневой чакры (красная)
            }
        }
    }

    private fun updateCenterTimerDisplay() {
        val minutes = centerTimerRemainingSeconds / 60
        val seconds = centerTimerRemainingSeconds % 60
        binding.circularMenuView.updateCenterTimer(centerTimerRemainingSeconds)

        val shamatha = YogaPractices.practices.find { it.name == "Шаматха" }
        shamatha?.let {
            binding.circularMenuView.updateItemRemainingTime(it.id, centerTimerRemainingSeconds)
        }
    }

    fun startPulsingAnimation(view: View) {
        // Анимация увеличения по X
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.05f)
        // Анимация увеличения по Y
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.05f)

        // Длительность одной фазы (туда-обратно будет 1600мс)
        scaleX.duration = 800
        scaleY.duration = 800

        // Бесконечный повтор
        scaleX.repeatCount = ObjectAnimator.INFINITE
        scaleY.repeatCount = ObjectAnimator.INFINITE

        // Режим "туда-обратно" (пульсация)
        scaleX.repeatMode = ObjectAnimator.REVERSE
        scaleY.repeatMode = ObjectAnimator.REVERSE

        // Запуск
        scaleX.start()
        scaleY.start()
    }

    override fun onResume() {
        super.onResume()
        startTimerUpdater()
    }

    override fun onPause() {
        super.onPause()
        timerUpdateJob?.cancel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerUpdateJob?.cancel()
        _binding = null
    }
}