package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfCraftDao {
    @Query("SELECT * FROM pdf_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<PdfRecordEntity>>

    @Query("SELECT * FROM pdf_records WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteRecords(): Flow<List<PdfRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: PdfRecordEntity): Long

    @Query("UPDATE pdf_records SET title = :newTitle WHERE id = :id")
    suspend fun renameRecord(id: Long, newTitle: String)

    @Query("UPDATE pdf_records SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean)

    @Query("DELETE FROM pdf_records WHERE id = :id")
    suspend fun deleteRecordById(id: Long)

    @Query("SELECT * FROM saved_signatures ORDER BY createdAt DESC")
    fun getAllSignatures(): Flow<List<SignatureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSignature(signature: SignatureEntity): Long

    @Query("DELETE FROM saved_signatures WHERE id = :id")
    suspend fun deleteSignatureById(id: Long)
}
