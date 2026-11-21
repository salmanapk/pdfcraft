package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import java.io.File

@Database(
    entities = [PdfRecordEntity::class, SignatureEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PdfCraftDatabase : RoomDatabase() {
    abstract fun pdfCraftDao(): PdfCraftDao

    companion object {
        @Volatile
        private var INSTANCE: PdfCraftDatabase? = null

        fun getInstance(context: Context): PdfCraftDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PdfCraftDatabase::class.java,
                    "pdfcraft_local.db"
                ).fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

class PdfCraftRepository(private val dao: PdfCraftDao) {
    val allRecords: Flow<List<PdfRecordEntity>> = dao.getAllRecords()
    val allSignatures: Flow<List<SignatureEntity>> = dao.getAllSignatures()

    suspend fun insertRecord(record: PdfRecordEntity): Long = dao.insertRecord(record)

    suspend fun renameRecord(id: Long, newTitle: String) = dao.renameRecord(id, newTitle)

    suspend fun toggleFavorite(record: PdfRecordEntity) =
        dao.setFavorite(record.id, !record.isFavorite)

    suspend fun deleteRecord(record: PdfRecordEntity) {
        dao.deleteRecordById(record.id)
        runCatching { File(record.filePath).delete() }
    }

    suspend fun insertSignature(signature: SignatureEntity): Long = dao.insertSignature(signature)

    suspend fun deleteSignature(signature: SignatureEntity) {
        dao.deleteSignatureById(signature.id)
        runCatching { File(signature.filePath).delete() }
    }
}
