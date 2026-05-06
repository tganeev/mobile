package org.readium.r2.testapp.vocabulary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.testapp.data.model.Vocabulary
import org.readium.r2.testapp.databinding.ItemVocabularyBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VocabularyAdapter(
    private val onItemLongClick: (Vocabulary) -> Unit
) : RecyclerView.Adapter<VocabularyAdapter.ViewHolder>() {

    private var words: List<Vocabulary> = emptyList()

    fun submitList(list: List<Vocabulary>) {
        words = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVocabularyBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemLongClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(words[position], position + 1)
    }

    override fun getItemCount(): Int = words.size

    class ViewHolder(
        private val binding: ItemVocabularyBinding,
        private val onLongClick: (Vocabulary) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(word: Vocabulary, position: Int) {
            binding.numberText.text = position.toString()
            binding.sourceWordText.text = word.sourceWord
            binding.translatedWordText.text = word.translatedWord

            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            binding.dateText.text = dateFormat.format(Date(word.createdDate))

            binding.root.setOnLongClickListener {
                onLongClick(word)
                true
            }
        }
    }
}