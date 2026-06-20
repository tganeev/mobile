// файл: src/main/java/org/readium/r2/testapp/data/db/AppDatabase.kt

package org.readium.r2.testapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.readium.r2.testapp.data.model.*

@Database(
    entities = [
        Book::class,
        Bookmark::class,
        Highlight::class,
        Catalog::class,
        ReadingStat::class,
        SleepRecord::class,
        Note::class,
        Vocabulary::class
    ],
    version = 13,  // Увеличиваем версию
    exportSchema = false
)
@TypeConverters(HighlightConverters::class, Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun booksDao(): BooksDao
    abstract fun catalogDao(): CatalogDao
    abstract fun sleepDao(): SleepDao
    abstract fun notesDao(): NotesDao
    abstract fun vocabularyDao(): VocabularyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ===== МИГРАЦИИ =====

        // Миграция с версии 12 на 13 (добавляем новые поля)
        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Добавляем новые поля в таблицу books, если их нет
                    database.execSQL("ALTER TABLE books ADD COLUMN is_deleted INTEGER DEFAULT 0")
                    database.execSQL("ALTER TABLE books ADD COLUMN has_file INTEGER DEFAULT 1")
                    database.execSQL("ALTER TABLE books ADD COLUMN last_synced INTEGER DEFAULT 0")
                    database.execSQL("ALTER TABLE books ADD COLUMN server_identifier TEXT")

                    // Добавляем новые поля в таблицу reading_stats, если их нет
                    database.execSQL("ALTER TABLE reading_stats ADD COLUMN hours_read REAL DEFAULT 0")

                    // Создаем новые таблицы, если их нет
                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS `notes` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `content` TEXT NOT NULL,
                            `my_comment` TEXT,
                            `book_title` TEXT NOT NULL,
                            `book_author` TEXT,
                            `category` TEXT NOT NULL DEFAULT 'Общее',
                            `creation_date` INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                        )
                    """)

                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS `vocabulary` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `source_word` TEXT NOT NULL,
                            `translated_word` TEXT NOT NULL,
                            `source_language` TEXT NOT NULL DEFAULT 'en',
                            `target_language` TEXT NOT NULL DEFAULT 'ru',
                            `created_date` INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                        )
                    """)

                    database.execSQL("""
                        CREATE TABLE IF NOT EXISTS `sleep_records` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `date` TEXT NOT NULL,
                            `wake_time` TEXT,
                            `bed_time` TEXT,
                            `is_manual` INTEGER NOT NULL DEFAULT 0,
                            `synced` INTEGER NOT NULL DEFAULT 0,
                            `created_at` INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000),
                            `updated_at` INTEGER NOT NULL DEFAULT (strftime('%s', 'now') * 1000)
                        )
                    """)

                    // Добавляем индексы
                    database.execSQL("CREATE INDEX IF NOT EXISTS `idx_books_server_identifier` ON `books` (`server_identifier`)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS `idx_reading_stats_book_id` ON `reading_stats` (`book_id`)")
                    database.execSQL("CREATE INDEX IF NOT EXISTS `idx_reading_stats_date` ON `reading_stats` (`date`)")

                } catch (e: Exception) {
                    // Логируем ошибку, но не прерываем миграцию
                    android.util.Log.e("AppDatabase", "Migration 12->13 error: ${e.message}")
                }
            }
        }

        // Общая миграция для любых версий (безопасная)
        val SAFE_MIGRATION = object : Migration(1, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    // Проверяем и добавляем колонки по очереди
                    val cursor = database.query("PRAGMA table_info(books)")
                    val columns = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        columns.add(cursor.getString(1))
                    }
                    cursor.close()

                    // Добавляем отсутствующие колонки
                    if (!columns.contains("is_deleted")) {
                        database.execSQL("ALTER TABLE books ADD COLUMN is_deleted INTEGER DEFAULT 0")
                    }
                    if (!columns.contains("has_file")) {
                        database.execSQL("ALTER TABLE books ADD COLUMN has_file INTEGER DEFAULT 1")
                    }
                    if (!columns.contains("last_synced")) {
                        database.execSQL("ALTER TABLE books ADD COLUMN last_synced INTEGER DEFAULT 0")
                    }
                    if (!columns.contains("server_identifier")) {
                        database.execSQL("ALTER TABLE books ADD COLUMN server_identifier TEXT")
                    }

                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Safe migration error: ${e.message}")
                }
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "database"
                )
                    .addMigrations(MIGRATION_12_13)  // Добавляем миграцию
                    .fallbackToDestructiveMigration()  // На случай критической ошибки
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}