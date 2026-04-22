package org.readium.r2.testapp.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.testapp.databinding.ItemHistoryRecordBinding

data class HistoryRecord(
    val date: String,
    val bookTitle: String,
    val eventType: String,
    val details: String,
    val bookIdentifier: String
)

class HistoryAdapter(
    private val onItemClick: (HistoryRecord) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    private var records: List<HistoryRecord> = emptyList()

    fun submitList(list: List<HistoryRecord>) {
        records = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(records[position])
    }

    override fun getItemCount(): Int = records.size

    class ViewHolder(
        private val binding: ItemHistoryRecordBinding,
        private val onItemClick: (HistoryRecord) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(record: HistoryRecord) {
            binding.historyDate.text = record.date
            binding.historyBook.text = record.bookTitle
            binding.historyEvent.text = record.eventType
            binding.historyDetails.text = record.details

            binding.root.setOnClickListener {
                onItemClick(record)
            }
        }
    }
}