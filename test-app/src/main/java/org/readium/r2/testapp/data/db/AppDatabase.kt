package org.readium.r2.testapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
    version = 8,  // Увеличиваем версию
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "database"
                )
                    .fallbackToDestructiveMigration()  // При конфликте — пересоздаёт БД
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}