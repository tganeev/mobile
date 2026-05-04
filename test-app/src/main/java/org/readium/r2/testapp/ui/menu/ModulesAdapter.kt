package org.readium.r2.testapp.ui.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.readium.r2.testapp.data.model.Module
import org.readium.r2.testapp.databinding.ItemModuleCardBinding

class ModulesAdapter(
    private val onModuleClick: (Module) -> Unit
) : RecyclerView.Adapter<ModulesAdapter.ModuleViewHolder>() {

    private var modules: List<Module> = emptyList()
    var libraryStats: LibraryStats = LibraryStats()
        set(value) {
            field = value
            val pos = findLibraryPosition()
            if (pos != -1) notifyItemChanged(pos)
        }

    fun submitList(list: List<Module>) {
        modules = list
        notifyDataSetChanged()
    }

    private fun findLibraryPosition(): Int {
        return modules.indexOfFirst { it.id == 1 }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ModuleViewHolder {
        val binding = ItemModuleCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ModuleViewHolder(binding, onModuleClick)
    }

    override fun onBindViewHolder(holder: ModuleViewHolder, position: Int) {
        holder.bind(modules[position], libraryStats)
    }

    override fun getItemCount(): Int = modules.size

    class ModuleViewHolder(
        private val binding: ItemModuleCardBinding,
        private val onModuleClick: (Module) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(module: Module, libraryStats: LibraryStats) {
            if (module.id == 1 && module.isAvailable) {
                binding.libraryContent.visibility = android.view.View.VISIBLE
                binding.regularContent.visibility = android.view.View.GONE

                binding.libraryContent.layoutParams.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT

                binding.moduleIcon.setImageResource(module.iconRes)
                binding.moduleTitle.text = module.title

                // Устанавливаем дату
                val calendar = java.util.Calendar.getInstance()
                val monthNames = arrayOf("Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек")
                val month = monthNames[calendar.get(java.util.Calendar.MONTH)]
                val day = calendar.get(java.util.Calendar.DAY_OF_MONTH).toString()

                binding.dateMonth.text = month
                binding.dateDay.text = day

                // Используем статистику за сегодня
                binding.statsPages.text = libraryStats.todayPagesRead.toString()
                binding.statsMinutes.text = libraryStats.todayMinutesRead.toString()

                binding.statsPlanned.text = libraryStats.plannedCount.toString()
                binding.statsInProgress.text = libraryStats.inProgressCount.toString()
                binding.statsCompleted.text = libraryStats.completedCount.toString()
                binding.statsTotalBooks.text = libraryStats.totalBooksInHistory.toString()
            } else {
                binding.libraryContent.visibility = android.view.View.GONE
                binding.regularContent.visibility = android.view.View.VISIBLE

                binding.regularContent.layoutParams.height = android.view.ViewGroup.LayoutParams.MATCH_PARENT

                binding.regularIcon.setImageResource(module.iconRes)
                binding.regularTitle.text = module.title
                binding.regularStatus.text = if (module.isAvailable) "Активен" else "В разработке"
                binding.regularStatus.setTextColor(
                    if (module.isAvailable) {
                        binding.root.context.getColor(android.R.color.holo_green_dark)
                    } else {
                        binding.root.context.getColor(android.R.color.darker_gray)
                    }
                )
            }

            binding.root.setOnClickListener {
                onModuleClick(module)
            }
        }
    }
}