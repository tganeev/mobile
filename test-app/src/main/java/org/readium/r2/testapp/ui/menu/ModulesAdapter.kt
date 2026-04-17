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

    fun submitList(list: List<Module>) {
        modules = list
        notifyDataSetChanged()
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
        holder.bind(modules[position])
    }

    override fun getItemCount(): Int = modules.size

    class ModuleViewHolder(
        private val binding: ItemModuleCardBinding,
        private val onModuleClick: (Module) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(module: Module) {
            binding.moduleIcon.setImageResource(module.iconRes)
            binding.moduleTitle.text = module.title
            binding.moduleStatus.text = if (module.isAvailable) "Активен" else "В разработке"
            binding.moduleStatus.setTextColor(
                if (module.isAvailable) {
                    binding.root.context.getColor(android.R.color.holo_green_dark)
                } else {
                    binding.root.context.getColor(android.R.color.darker_gray)
                }
            )

            binding.root.setOnClickListener {
                onModuleClick(module)
            }
        }
    }
}