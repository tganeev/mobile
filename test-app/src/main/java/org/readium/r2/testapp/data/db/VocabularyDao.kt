package org.readium.r2.testapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.readium.r2.testapp.data.model.Vocabulary

@Dao
interface VocabularyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWord(word: Vocabulary): Long

    @Query("SELECT * FROM vocabulary ORDER BY created_date DESC")
    fun getAllWords(): Flow<List<Vocabulary>>

    @Query("DELETE FROM vocabulary WHERE id = :id")
    suspend fun deleteWord(id: Long)

    @Query("DELETE FROM vocabulary")
    suspend fun deleteAllWords()

    @Query("SELECT * FROM vocabulary WHERE source_word LIKE '%' || :query || '%' OR translated_word LIKE '%' || :query || '%' ORDER BY created_date DESC")
    fun searchWords(query: String): Flow<List<Vocabulary>>
}