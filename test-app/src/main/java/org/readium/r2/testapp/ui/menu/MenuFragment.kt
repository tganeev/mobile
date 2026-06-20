// файл: src/main/java/org/readium/r2/testapp/ui/menu/MenuFragment.kt

package org.readium.r2.testapp.ui.menu

import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.testapp.R
import org.readium.r2.testapp.backup.BackupContract
import org.readium.r2.testapp.backup.BackupManager
import org.readium.r2.testapp.backup.BackupSaveContract
import org.readium.r2.testapp.data.model.Module
import org.readium.r2.testapp.databinding.FragmentMenuBinding

class MenuFragment : Fragment() {

    private var _binding: FragmentMenuBinding? = null
    private val binding get() = _binding!!

    private lateinit var modulesAdapter: ModulesAdapter
    private val libraryViewModel: LibraryStatsViewModel by viewModels()

    // ===== НОВЫЕ ПОЛЯ ДЛЯ БЭКАПА =====
    private lateinit var backupManager: BackupManager
    private val exportBackupLauncher = registerForActivityResult(
        BackupSaveContract()
    ) { uri ->
        uri?.let { performExport(it) }
    }
    private val importBackupLauncher = registerForActivityResult(
        BackupContract()
    ) { uri ->
        uri?.let { performImport(it) }
    }
    // ===== КОНЕЦ НОВЫХ ПОЛЕЙ =====

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

        // Инициализируем BackupManager
        backupManager = BackupManager(requireContext())

        setupModulesGrid()
        setupMenu()
        loadModules()
        observeLibraryStats()
    }

    override fun onResume() {
        super.onResume()
        libraryViewModel.loadStats()
    }

    private fun setupModulesGrid() {
        modulesAdapter = ModulesAdapter { module ->
            handleModuleClick(module)
        }
        binding.modulesGrid.adapter = modulesAdapter
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menuInflater.inflate(R.menu.menu_main_screen, menu)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.action_export_db -> {
                            exportDatabase()
                            true
                        }
                        R.id.action_import_db -> {
                            importDatabase()
                            true
                        }
                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun loadModules() {
        val modules = listOf(
            Module(1, "Библиотека", R.drawable.ic_module_reader, isAvailable = true),
            Module(2, "Математика", R.drawable.ic_module_math, isAvailable = false),
            Module(3, "Будильник", R.drawable.ic_module_alarm, isAvailable = true),
            Module(4, "Состояния", R.drawable.ic_module_emotions, isAvailable = false),
            Module(5, "Календарь", R.drawable.ic_module_calendar, isAvailable = false),
            Module(6, "Банк слов", R.drawable.ic_module_vocabulary, isAvailable = true),
            Module(7, "База знаний", R.drawable.ic_module_notes, isAvailable = false),
            Module(8, "Вокал", R.drawable.ic_module_vocal, isAvailable = false)
        )
        modulesAdapter.submitList(modules)
    }

    private fun observeLibraryStats() {
        lifecycleScope.launch {
            libraryViewModel.stats.collect { stats ->
                modulesAdapter.libraryStats = stats
            }
        }
    }

    // ===== НОВЫЕ МЕТОДЫ ДЛЯ БЭКАПА =====

    private fun exportDatabase() {
        AlertDialog.Builder(requireContext())
            .setTitle("Экспорт базы данных")
            .setMessage("Будет создан архив со всеми вашими данными (книги, заметки, статистика, слова и т.д.).\n\nВыберите место для сохранения.")
            .setPositiveButton("Экспортировать") { _, _ ->
                exportBackupLauncher.launch(Unit)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun importDatabase() {
        AlertDialog.Builder(requireContext())
            .setTitle("Импорт базы данных")
            .setMessage("⚠️ ВНИМАНИЕ!\n\nИмпорт полностью ЗАМЕНИТ все текущие данные приложения (книги, заметки, статистику).\n\nВыберите файл бэкапа для восстановления.")
            .setPositiveButton("Импортировать") { _, _ ->
                importBackupLauncher.launch(Unit)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun performExport(uri: android.net.Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            // Запускаем экспорт в фоне
            val result = runCatching {
                backupManager.exportData(uri) { progress ->
                    // Обновляем UI в главном потоке
                    lifecycleScope.launch(Dispatchers.Main) {
                        // Здесь обновляем прогресс
                    }
                }
            }

            // Переключаемся на главный поток для показа результата
            withContext(Dispatchers.Main) {
                result.onSuccess {
                    Snackbar.make(
                        requireView(),
                        "✅ Данные успешно экспортированы!",
                        Snackbar.LENGTH_LONG
                    ).show()
                }.onFailure { e ->
                    Snackbar.make(
                        requireView(),
                        "❌ Ошибка экспорта: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun performImport(uri: android.net.Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching {
                backupManager.importData(uri) { progress ->
                    lifecycleScope.launch(Dispatchers.Main) {
                        // Обновляем прогресс
                    }
                }
            }

            withContext(Dispatchers.Main) {
                result.onSuccess {
                    Snackbar.make(
                        requireView(),
                        "✅ Данные успешно импортированы!",
                        Snackbar.LENGTH_LONG
                    ).show()
                }.onFailure { e ->
                    Snackbar.make(
                        requireView(),
                        "❌ Ошибка импорта: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                    e.printStackTrace()
                }
            }
        }
    }

    // ===== КОНЕЦ НОВЫХ МЕТОДОВ =====

    private fun handleModuleClick(module: Module) {
        when (module.id) {
            1 -> navigateToReader()
            3 -> navigateToAlarm()
            6 -> navigateToVocabulary()
            else -> showUnderDevelopmentMessage(module)
        }
    }

    private fun navigateToVocabulary() {
        val navController = requireActivity().findNavController(R.id.nav_host_fragment)
        navController.navigate(R.id.action_menu_to_vocabulary)
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