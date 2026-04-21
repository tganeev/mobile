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
        SleepRecord::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(HighlightConverters::class, Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun booksDao(): BooksDao
    abstract fun catalogDao(): CatalogDao
    abstract fun sleepDao(): SleepDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE books ADD COLUMN reading_time INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE books ADD COLUMN pages_read INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE books ADD COLUMN last_read_date INTEGER")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS reading_stats (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        book_id INTEGER NOT NULL,
                        date TEXT NOT NULL,
                        pages_read INTEGER NOT NULL DEFAULT 0,
                        hours_read REAL NOT NULL DEFAULT 0,
                        FOREIGN KEY(book_id) REFERENCES books(id) ON DELETE CASCADE,
                        UNIQUE(book_id, date)
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_reading_stats_book_id ON reading_stats(book_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_reading_stats_date ON reading_stats(date)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS sleep_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL UNIQUE,
                        wake_time TEXT,
                        bed_time TEXT,
                        is_manual INTEGER NOT NULL DEFAULT 0,
                        synced INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_sleep_records_date ON sleep_records(date)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_sleep_records_synced ON sleep_records(synced)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE sleep_records RENAME TO sleep_records_old")

                database.execSQL("""
                    CREATE TABLE sleep_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL UNIQUE,
                        wake_time TEXT,
                        bed_time TEXT,
                        is_manual INTEGER NOT NULL DEFAULT 0,
                        synced INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """)

                database.execSQL("""
                    INSERT INTO sleep_records (id, date, wake_time, bed_time, is_manual, synced, created_at, updated_at)
                    SELECT id, date, wake_time, bed_time, 
                           COALESCE(is_manual, 0) as is_manual,
                           COALESCE(synced, 0) as synced,
                           created_at, updated_at
                    FROM sleep_records_old
                """)

                database.execSQL("DROP TABLE sleep_records_old")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_sleep_records_date ON sleep_records(date)")
                database.execSQL("CREATE INDEX IF NOT EXISTS idx_sleep_records_synced ON sleep_records(synced)")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Удаляем старую таблицу и индексы
                database.execSQL("DROP INDEX IF EXISTS idx_sleep_records_date")
                database.execSQL("DROP INDEX IF EXISTS idx_sleep_records_synced")
                database.execSQL("DROP TABLE IF EXISTS sleep_records")

                // Создаём таблицу заново (без DEFAULT)
                database.execSQL("""
                    CREATE TABLE sleep_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL UNIQUE,
                        wake_time TEXT,
                        bed_time TEXT,
                        is_manual INTEGER NOT NULL,
                        synced INTEGER NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                """)

                database.execSQL("CREATE INDEX idx_sleep_records_date ON sleep_records(date)")
                database.execSQL("CREATE INDEX idx_sleep_records_synced ON sleep_records(synced)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}