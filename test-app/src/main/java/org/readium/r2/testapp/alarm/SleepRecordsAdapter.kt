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

    private var records: List<SleepRecord> = emptyList()      // Для отображения (новые сверху)
    private var recordsAsc: List<SleepRecord> = emptyList()   // Для расчёта (старые сверху)

    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale("ru"))

    fun submitList(list: List<SleepRecord>) {
        // Для отображения: новые сверху
        records = list.sortedByDescending { it.date }
        // Для расчёта сна: старые сверху
        recordsAsc = list.sortedBy { it.date }
        notifyDataSetChanged()
    }

    /**
     * Рассчитывает продолжительность сна (используя сортировку по возрастанию)
     */
    private fun calculateSleepDuration(record: SleepRecord): String {
        // Находим позицию записи в списке для расчёта (по возрастанию)
        val position = recordsAsc.indexOfFirst { it.id == record.id }
        if (position == -1) return "--:--"

        val currentRecord = recordsAsc[position]

        val bedTime = currentRecord.bedTime
        if (bedTime == null) return "--:--"

        // Ищем следующую запись с подъёмом
        for (i in (position + 1) until recordsAsc.size) {
            val nextRecord = recordsAsc[i]
            val wakeTime = nextRecord.wakeTime
            if (wakeTime != null) {
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

        return "--:--"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSleepRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick, onItemLongClick, dateFormatter)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val record = records[position]
        val sleepDuration = calculateSleepDuration(record)
        holder.bind(record, sleepDuration)
    }

    override fun getItemCount(): Int = records.size

    class ViewHolder(
        private val binding: ItemSleepRecordBinding,
        private val onItemClick: (SleepRecord) -> Unit,
        private val onItemLongClick: (SleepRecord) -> Unit,
        private val dateFormatter: DateTimeFormatter
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: SleepRecord, sleepDuration: String) {
            binding.recordDate.text = record.date.format(dateFormatter)

            val wakeTimeStr = record.wakeTime?.let {
                String.format("%02d:%02d", it.hour, it.minute)
            } ?: "--:--"

            val bedTimeStr = record.bedTime?.let {
                String.format("%02d:%02d", it.hour, it.minute)
            } ?: "--:--"

            binding.wakeTimeText.text = wakeTimeStr
            binding.bedTimeText.text = bedTimeStr

            if (record.bedTime != null) {
                binding.durationText.text = sleepDuration
                if (sleepDuration != "--:--") {
                    binding.durationText.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                } else {
                    binding.durationText.setTextColor(android.graphics.Color.parseColor("#999999"))
                }
            } else {
                binding.durationText.text = "--:--"
                binding.durationText.setTextColor(android.graphics.Color.parseColor("#999999"))
            }

            binding.manualBadge.visibility = if (record.isManual) android.view.View.VISIBLE else android.view.View.GONE

            binding.root.setOnClickListener {
                onItemClick(record)
            }

            binding.root.setOnLongClickListener {
                onItemLongClick(record)
                true
            }
        }
    }
}