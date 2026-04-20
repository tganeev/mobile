package org.readium.r2.testapp.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import org.readium.r2.testapp.R
import org.readium.r2.testapp.data.model.Module
import org.readium.r2.testapp.databinding.FragmentMenuBinding

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var modulesAdapter: ModulesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupModulesGrid()
        loadModules()
    }

    private fun setupModulesGrid() {
        modulesAdapter = ModulesAdapter { module ->
            handleModuleClick(module)
        }
        binding.modulesGrid.adapter = modulesAdapter
    }

    private fun loadModules() {
        val modules = listOf(
            Module(1, "Библиотека", R.drawable.ic_module_reader, isAvailable = true),
            Module(2, "Математика", R.drawable.ic_module_math, isAvailable = false),
            Module(3, "Будильник", R.drawable.ic_module_alarm, isAvailable = true),
            Module(4, "Состояния", R.drawable.ic_module_emotions, isAvailable = false),
            Module(5, "Календарь", R.drawable.ic_module_calendar, isAvailable = false),
            Module(6, "Банк слов", R.drawable.ic_module_vocabulary, isAvailable = false),
            Module(7, "Заметки", R.drawable.ic_module_notes, isAvailable = false),
            Module(8, "Вокал", R.drawable.ic_module_vocal, isAvailable = false)
        )
        modulesAdapter.submitList(modules)
    }

    private fun handleModuleClick(module: Module) {
        when (module.id) {
            1 -> navigateToReader()
            3 -> navigateToAlarm()
            else -> showUnderDevelopmentMessage(module)
        }
    }

    private fun navigateToReader() {
        val navController = requireActivity().findNavController(R.id.nav_host_fragment)
        navController.navigate(R.id.action_menu_to_bookshelf)
    }

    private fun navigateToAlarm() {
        val navController = requireActivity().findNavController(R.id.nav_host_fragment)
        navController.navigate(R.id.action_menu_to_alarm)
    }

    private fun showUnderDevelopmentMessage(module: Module) {
        Snackbar.make(
            binding.root,
            "Модуль «${module.title}» в разработке",
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}