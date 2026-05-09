package org.readium.r2.testapp.yoga

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.testapp.databinding.ItemActiveTimerBinding

class ActiveTimersAdapter(
    private val onPause: (ActiveTimer) -> Unit,
    private val onResume: (ActiveTimer) -> Unit,
    private val onStop: (ActiveTimer) -> Unit
) : RecyclerView.Adapter<ActiveTimersAdapter.ViewHolder>() {

    private var timers: List<ActiveTimer> = emptyList()

    fun submitList(list: List<ActiveTimer>) {
        timers = list
        notifyDataSetChanged()
    }

    fun updateRemainingTimes(updatedTimers: List<ActiveTimer>) {
        updatedTimers.forEachIndexed { index, timer ->
            val oldTimer = timers.getOrNull(index)
            if (oldTimer != null && oldTimer.remainingSeconds != timer.remainingSeconds) {
                notifyItemChanged(index)
            }
        }
        timers = updatedTimers
    }

    fun updateTimers(newTimers: List<ActiveTimer>) {
        timers = newTimers
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemActiveTimerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(timers[position])
    }

    override fun getItemCount(): Int = timers.size

    inner class ViewHolder(
        private val binding: ItemActiveTimerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(timer: ActiveTimer) {
            binding.timerNameText.text = timer.practice.name

            val minutes = timer.remainingSeconds / 60
            val seconds = timer.remainingSeconds % 60
            binding.timerTimeText.text = String.format("%02d:%02d", minutes, seconds)

            if (timer.isRunning) {
                binding.pauseButton.visibility = android.view.View.VISIBLE
                binding.resumeButton.visibility = android.view.View.GONE
            } else {
                binding.pauseButton.visibility = android.view.View.GONE
                binding.resumeButton.visibility = android.view.View.VISIBLE
            }

            binding.pauseButton.setOnClickListener {
                onPause(timer)
            }

            binding.resumeButton.setOnClickListener {
                onResume(timer)
            }

            binding.stopButton.setOnClickListener {
                onStop(timer)
            }
        }
    }
}