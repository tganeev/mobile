package org.readium.r2.testapp

import android.content.Context
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
import org.readium.r2.testapp.data.AlarmPreferencesDataStore
import org.readium.r2.testapp.data.BookRepository
import org.readium.r2.testapp.data.SleepRepository
import org.readium.r2.testapp.data.db.AppDatabase
import org.readium.r2.testapp.domain.Bookshelf
import org.readium.r2.testapp.domain.CoverStorage
import org.readium.r2.testapp.domain.PublicationRetriever
import org.readium.r2.testapp.reader.ReaderRepository
import org.readium.r2.testapp.sync.SyncManager
import org.readium.r2.testapp.utils.tryOrLog
import timber.log.Timber
import java.io.File
import java.util.Properties
import java.util.concurrent.Executors
import org.readium.r2.testapp.alarm.AlarmForegroundService
import org.readium.r2.testapp.sync.HistorySyncManager

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

    private val coroutineScope: CoroutineScope = MainScope()

    private val Context.navigatorPreferences: DataStore<Preferences>
        by preferencesDataStore(name = "navigator-preferences")

    lateinit var historySyncManager: HistorySyncManager
        private set



    override fun onCreate() {
        if (DEBUG) {
            enableStrictMode()
            Timber.plant(Timber.DebugTree())
        }

        super.onCreate()

        historySyncManager = HistorySyncManager(this, this)


        DynamicColors.applyToActivitiesIfAvailable(this)

        readium = Readium(this)

        storageDir = computeStorageDir()

        val database = AppDatabase.getDatabase(this)


        bookRepository = BookRepository(database.booksDao())
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

        // Восстанавливаем будильники после установки приложения
        coroutineScope.launch(Dispatchers.IO) {
            alarmPreferencesDataStore.alarmPreferencesFlow.collect { prefs ->
                AlarmScheduler.rescheduleAllAlarms(this@Application, prefs)
            }
        }
        // Запускаем Foreground Service для будильника
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AlarmForegroundService.start(this)
        }

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
}