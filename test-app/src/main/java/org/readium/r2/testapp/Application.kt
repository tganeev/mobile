// файл: src/main/java/org/readium/r2/testapp/Application.kt
// Исправляем только проблемные места

package org.readium.r2.testapp

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.StrictMode
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.readium.r2.testapp.BuildConfig.DEBUG
import org.readium.r2.testapp.alarm.AlarmScheduler
import org.readium.r2.testapp.alarm.AlarmSoundService
import org.readium.r2.testapp.data.AlarmPreferencesDataStore
import org.readium.r2.testapp.data.BookRepository
import org.readium.r2.testapp.data.SleepRepository
import org.readium.r2.testapp.data.db.AppDatabase
import org.readium.r2.testapp.domain.Bookshelf
import org.readium.r2.testapp.domain.CoverStorage
import org.readium.r2.testapp.domain.PublicationRetriever
import org.readium.r2.testapp.reader.ReaderRepository
import org.readium.r2.testapp.sync.HistorySyncManager
import org.readium.r2.testapp.sync.SyncManager
import org.readium.r2.testapp.utils.tryOrLog
import timber.log.Timber
import java.io.File
import java.util.Properties
import java.util.concurrent.Executors

class Application : android.app.Application() {

    lateinit var readium: Readium
        private set

    lateinit var storageDir: File

    lateinit var bookRepository: BookRepository
        private set

    lateinit var bookshelf: Bookshelf
        private set

    lateinit var readerRepository: ReaderRepository
        private set

    lateinit var syncManager: SyncManager
        private set

    lateinit var alarmPreferencesDataStore: AlarmPreferencesDataStore
        private set

    lateinit var sleepRepository: SleepRepository
        private set

    lateinit var historySyncManager: HistorySyncManager
        private set

    private val coroutineScope: CoroutineScope = MainScope()

    private val Context.navigatorPreferences: DataStore<Preferences>
        by preferencesDataStore(name = "navigator-preferences")

    override fun onCreate() {
        if (DEBUG) {
            enableStrictMode()
            Timber.plant(Timber.DebugTree())
        }

        super.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)

        // Проверяем и мигрируем данные при первом запуске новой версии
        checkAndMigrateData()

        readium = Readium(this)

        storageDir = computeStorageDir()

        val database = AppDatabase.getDatabase(this)

        bookRepository = BookRepository(
            database.booksDao(),
            database.notesDao(),
            database.vocabularyDao()
        )

        sleepRepository = SleepRepository(database.sleepDao())
        alarmPreferencesDataStore = AlarmPreferencesDataStore(this)

        val downloadsDir = File(cacheDir, "downloads")
        tryOrLog { downloadsDir.delete() }

        val publicationRetriever = PublicationRetriever(
            context = applicationContext,
            assetRetriever = readium.assetRetriever,
            bookshelfDir = storageDir,
            tempDir = downloadsDir,
            httpClient = readium.httpClient,
            lcpService = readium.lcpService.getOrNull()
        )

        bookshelf = Bookshelf(
            bookRepository,
            CoverStorage(storageDir, httpClient = readium.httpClient),
            readium.publicationOpener,
            readium.assetRetriever,
            publicationRetriever
        )

        readerRepository = ReaderRepository(
            this@Application,
            readium,
            bookRepository,
            navigatorPreferences
        )

        syncManager = SyncManager(this, bookRepository)
        historySyncManager = HistorySyncManager(this, this)

        startAlarmServices()
    }

    // ===== НОВЫЙ МЕТОД: Проверка и миграция данных =====
    private fun checkAndMigrateData() {
        try {
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val lastVersion = prefs.getInt("app_version_code", 0)
            val currentVersion = BuildConfig.VERSION_CODE

            Timber.d("Last version: $lastVersion, Current version: $currentVersion")

            if (lastVersion < currentVersion) {
                Timber.d("App updated from $lastVersion to $currentVersion")
                prefs.edit().putInt("app_version_code", currentVersion).apply()
            }

            // Проверяем целостность БД
            val database = File(applicationContext.filesDir, "database")
            if (!database.exists()) {
                Timber.d("Database file not found, will be created on first use")
            }

        } catch (e: Exception) {
            Timber.e(e, "Failed to check app version")
        }
    }

    // ===== СУЩЕСТВУЮЩИЕ МЕТОДЫ (не меняем) =====
    private fun enableStrictMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return
        }

        val executor = Executors.newSingleThreadExecutor()
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyListener(executor) { violation ->
                    Timber.e(violation, "Thread policy violation")
                }
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyListener(executor) { violation ->
                    Timber.e(violation, "VM policy violation")
                }
                .build()
        )
    }

    private fun computeStorageDir(): File {
        val properties = Properties()
        val inputStream = assets.open("configs/config.properties")
        properties.load(inputStream)
        val useExternalFileDir = properties.getProperty("useExternalFileDir", "false")!!.toBoolean()

        return File(
            if (useExternalFileDir) {
                getExternalFilesDir(null)?.path + "/"
            } else {
                filesDir?.path + "/"
            }
        )
    }

    private fun startAlarmServices() {
        try {
            val intent = Intent(this, AlarmSoundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Timber.d("AlarmSoundService started")
        } catch (e: Exception) {
            Timber.e(e, "Failed to start AlarmSoundService")
        }

        coroutineScope.launch(Dispatchers.IO) {
            alarmPreferencesDataStore.alarmPreferencesFlow.collect { prefs ->
                AlarmScheduler.rescheduleAllAlarms(this@Application, prefs)
            }
        }
    }
}