package org.readium.r2.testapp.alarm

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.testapp.data.model.SleepRecord
import org.readium.r2.testapp.databinding.ItemSleepRecordBinding
import java.time.format.DateTimeFormatter

class SleepRecordsAdapter(
    private val onItemClick: (SleepRecord) -> Unit,
    private val onItemLongClick: (SleepRecord) -> Unit
) : RecyclerView.Adapter<SleepRecordsAdapter.ViewHolder>() {

    private var records: List<SleepRecord> = emptyList()

    fun submitList(list: List<SleepRecord>) {
        records = list.sortedByDescending { it.date }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSleepRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    class ViewHolder(
        private val binding: ItemSleepRecordBinding,
        private val onItemClick: (SleepRecord) -> Unit,
        private val onItemLongClick: (SleepRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale("ru"))
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun bind(record: SleepRecord) {
            binding.recordDate.text = record.date.format(dateFormatter)

            binding.wakeTimeText.text = record.wakeTime?.format(timeFormatter) ?: "--:--"
            binding.bedTimeText.text = record.bedTime?.format(timeFormatter) ?: "--:--"

            val duration = calculateDuration(record)
            binding.durationText.text = duration

            binding.manualBadge.visibility = if (record.isManual) android.view.View.VISIBLE else android.view.View.GONE

            binding.root.setOnClickListener {
                onItemClick(record)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(record)
                true
            }
        }

        private fun calculateDuration(record: SleepRecord): String {
            val bedTime = record.bedTime ?: return "-- ч -- мин"
            val wakeTime = record.wakeTime ?: return "-- ч -- мин"

            val bedMinutes = bedTime.hour * 60 + bedTime.minute
            val wakeMinutes = wakeTime.hour * 60 + wakeTime.minute

            val durationMinutes = if (wakeMinutes >= bedMinutes) {
                wakeMinutes - bedMinutes
            } else {
                (24 * 60 - bedMinutes) + wakeMinutes
            }

            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60

            return "${hours}ч ${minutes}мин"
        }
    }
}