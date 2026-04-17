package org.readium.r2.testapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.navigateUp
import org.readium.r2.testapp.databinding.ActivityMainBinding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import org.readium.r2.testapp.Application

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.container) { v, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(statusBars.left, statusBars.top, statusBars.right, statusBars.bottom)
            insets
        }

        // Получаем NavController
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Настройка ActionBar - только главное меню является верхним уровнем
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.menu_fragment
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        // Обновляем меню при смене destination
        navController.addOnDestinationChangedListener { _, _, _ ->
            supportInvalidateOptionsMenu()
        }

        // Блокировка экрана
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Определяем текущий фрагмент
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()

        // Если текущий фрагмент - BookshelfFragment, показываем меню синхронизации
        return when (currentFragment) {
            is org.readium.r2.testapp.bookshelf.BookshelfFragment -> {
                menuInflater.inflate(R.menu.menu_main, menu)
                true
            }
            else -> {
                // Для остальных фрагментов показываем пустое меню
                false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                performSync()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun performSync() {
        lifecycleScope.launch {
            val snackbar = Snackbar.make(
                binding.root,
                "Синхронизация...",
                Snackbar.LENGTH_INDEFINITE
            )
            snackbar.show()

            try {
                val app = application as Application
                val result = app.syncManager.syncAllBooks()

                snackbar.dismiss()

                result.onSuccess { response ->
                    val message = "Синхронизация завершена:\n" +
                        "📚 Создано книг: ${response.booksCreated}\n" +
                        "🔄 Обновлено книг: ${response.booksUpdated}\n" +
                        "📊 Создано записей: ${response.statsCreated}\n" +
                        "🔄 Обновлено записей: ${response.statsUpdated}"

                    Snackbar.make(
                        binding.root,
                        message,
                        Snackbar.LENGTH_LONG
                    ).show()
                }.onFailure { error ->
                    Snackbar.make(
                        binding.root,
                        "Ошибка: ${error.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                snackbar.dismiss()
                Snackbar.make(
                    binding.root,
                    "Ошибка: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}