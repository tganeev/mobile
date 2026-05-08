package org.readium.r2.testapp.alarm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.testapp.data.model.AlarmLog
import org.readium.r2.testapp.databinding.ItemAlarmLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.readium.r2.testapp.data.model.AlarmLogType

class AlarmLogsAdapter : RecyclerView.Adapter<AlarmLogsAdapter.ViewHolder>() {

    private var logs: List<AlarmLog> = emptyList()

    fun submitList(list: List<AlarmLog>) {
        logs = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlarmLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(logs[position])
    }

    override fun getItemCount(): Int = logs.size

    class ViewHolder(private val binding: ItemAlarmLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())

        fun bind(log: AlarmLog) {
            binding.timeText.text = dateFormat.format(Date(log.timestamp))
            binding.typeText.text = log.type.name
            binding.typeText.setTextColor(getColorForType(log.type))
            binding.messageText.text = log.message

            if (!log.exception.isNullOrBlank()) {
                binding.exceptionText.text = log.exception.take(200)
                binding.exceptionText.visibility = View.VISIBLE
            } else {
                binding.exceptionText.visibility = View.GONE
            }
        }

        private fun getColorForType(type: AlarmLogType): Int {
            return when (type) {
                AlarmLogType.SCHEDULED -> android.graphics.Color.parseColor("#2196F3")
                AlarmLogType.TRIGGERED -> android.graphics.Color.parseColor("#4CAF50")
                AlarmLogType.SNOOZED -> android.graphics.Color.parseColor("#FF9800")
                AlarmLogType.CANCELLED -> android.graphics.Color.parseColor("#9E9E9E")
                AlarmLogType.ERROR -> android.graphics.Color.parseColor("#F44336")
                AlarmLogType.PERMISSION_MISSING -> android.graphics.Color.parseColor("#FF5722")
                AlarmLogType.WAKE_LOCK_ACQUIRED, AlarmLogType.WAKE_LOCK_RELEASED ->
                    android.graphics.Color.parseColor("#00BCD4")
                else -> android.graphics.Color.parseColor("#757575")
            }
        }
    }
}