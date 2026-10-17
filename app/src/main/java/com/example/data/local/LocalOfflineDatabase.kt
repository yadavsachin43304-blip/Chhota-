package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.security.KeystoreEncryptionManager
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Room Database Entity for Offline AI Mode (Section 9 & Section 17).
 * Stores local notes, local reminders/timers, and offline cached knowledge packages.
 */
@Entity(tableName = "offline_items")
data class LocalOfflineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String, // "NOTE", "REMINDER", "OFFLINE_KNOWLEDGE", "MODEL_PACKAGE"
    val isDownloaded: Boolean = true,
    val sizeMb: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis()
)

/**
 * In-App Encrypted Storage Vault Entity.
 * Stores files directly inside the app (Room + internal encrypted storage file)
 * so all user documents, notes, and business files stay inside the app even offline.
 */
@Entity(tableName = "in_app_vault_files")
data class InAppVaultFileEntity(
    @PrimaryKey val fileId: String,
    val fileName: String,
    val mimeType: String,
    val folder: String,
    val tags: String,
    val encryptedCiphertext: String,
    val sha256Checksum: String,
    val aiSummary: String,
    val sizeBytes: Long,
    val internalFilePath: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Dao
interface LocalOfflineDao {
    @Query("SELECT * FROM offline_items ORDER BY createdAtMillis DESC")
    fun observeAllItems(): Flow<List<LocalOfflineItemEntity>>

    @Query("SELECT * FROM offline_items WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY createdAtMillis DESC")
    suspend fun searchOfflineItems(query: String): List<LocalOfflineItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: LocalOfflineItemEntity): Long

    @Query("DELETE FROM offline_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("SELECT COUNT(*) FROM offline_items")
    suspend fun countItems(): Int

    // --- IN-APP ENCRYPTED STORAGE VAULT QUERIES ---

    @Query("SELECT * FROM in_app_vault_files ORDER BY createdAtMillis DESC")
    fun observeAllVaultFiles(): Flow<List<InAppVaultFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultFile(file: InAppVaultFileEntity)

    @Query("DELETE FROM in_app_vault_files WHERE fileId = :fileId")
    suspend fun deleteVaultFileById(fileId: String)

    @Query("SELECT COUNT(*) FROM in_app_vault_files")
    suspend fun countVaultFiles(): Int
}

@Database(
    entities = [LocalOfflineItemEntity::class, InAppVaultFileEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ChhotuLocalDatabase : RoomDatabase() {
    abstract fun offlineDao(): LocalOfflineDao

    companion object {
        @Volatile
        private var INSTANCE: ChhotuLocalDatabase? = null

        fun getInstance(context: Context): ChhotuLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChhotuLocalDatabase::class.java,
                    "chhotu_offline_room.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class LocalOfflineRepository(
    private val dao: LocalOfflineDao,
    private val appContext: Context? = null
) {
    val allOfflineItems: Flow<List<LocalOfflineItemEntity>> = dao.observeAllItems()
    val allInAppVaultFiles: Flow<List<InAppVaultFileEntity>> = dao.observeAllVaultFiles()

    suspend fun addOfflineItem(
        title: String,
        content: String,
        category: String,
        sizeMb: Int = 0
    ): Long {
        return dao.insertItem(
            LocalOfflineItemEntity(
                title = title,
                content = content,
                category = category,
                sizeMb = sizeMb
            )
        )
    }

    suspend fun searchOffline(query: String): List<LocalOfflineItemEntity> {
        return dao.searchOfflineItems(query)
    }

    suspend fun deleteOfflineItem(id: Int) {
        dao.deleteItemById(id)
    }

    /**
     * Saves an encrypted file directly inside the app's internal storage directory (`filesDir/chhotu_vault`)
     * AND indexes it in the local Room `in_app_vault_files` table.
     */
    suspend fun saveFileToInAppVault(
        fileId: String,
        fileName: String,
        mimeType: String,
        folder: String,
        tags: String,
        rawTextContent: String,
        aiSummary: String
    ): InAppVaultFileEntity {
        val ciphertext = KeystoreEncryptionManager.encryptText(rawTextContent)
        val checksum = KeystoreEncryptionManager.sha256Hex(rawTextContent)
        val vaultDir = appContext?.let { File(it.filesDir, "chhotu_vault").apply { mkdirs() } }
        val diskFile = vaultDir?.let { File(it, "${fileId}_${fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}.enc") }
        diskFile?.writeText(ciphertext)

        val entity = InAppVaultFileEntity(
            fileId = fileId,
            fileName = fileName,
            mimeType = mimeType,
            folder = folder,
            tags = tags,
            encryptedCiphertext = ciphertext,
            sha256Checksum = checksum,
            aiSummary = aiSummary,
            sizeBytes = rawTextContent.toByteArray().size.toLong(),
            internalFilePath = diskFile?.absolutePath ?: "/data/data/chhotu_vault/$fileId.enc"
        )
        dao.insertVaultFile(entity)
        return entity
    }

    suspend fun deleteFileFromInAppVault(fileId: String) {
        dao.deleteVaultFileById(fileId)
        appContext?.let { ctx ->
            val vaultDir = File(ctx.filesDir, "chhotu_vault")
            vaultDir.listFiles()?.filter { it.name.startsWith("${fileId}_") }?.forEach { it.delete() }
        }
    }

    fun decryptInAppVaultFile(entity: InAppVaultFileEntity): String {
        return KeystoreEncryptionManager.decryptText(entity.encryptedCiphertext)
    }

    suspend fun seedDefaultsIfEmpty() {
        if (dao.countItems() == 0) {
            dao.insertItem(
                LocalOfflineItemEntity(
                    title = "Hinglish & Hindi Offline Command Pack",
                    content = "Offline mode mein available: Wake-word ('Chhotu'), Timer, Reminder, Calculator, In-App Encrypted Vault, aur installed apps open karna.",
                    category = "OFFLINE_KNOWLEDGE",
                    sizeMb = 18
                )
            )
            dao.insertItem(
                LocalOfflineItemEntity(
                    title = "On-Device Indic ASR + Realistic Male Voice Pack (hi-IN / en-IN)",
                    content = "Local speech recognition & warm human male friend voice synthesis package for Chhotu AI.",
                    category = "MODEL_PACKAGE",
                    sizeMb = 64
                )
            )
            dao.insertItem(
                LocalOfflineItemEntity(
                    title = "Daily Security & Business Check Reminder",
                    content = "Shaam 7 baje business invoice aur encrypted vault backup check karein.",
                    category = "REMINDER",
                    sizeMb = 1
                )
            )
        }

        if (dao.countVaultFiles() == 0) {
            saveFileToInAppVault(
                fileId = "vault_tax_2026",
                fileName = "Q4_Tax_Audit.pdf",
                mimeType = "application/pdf",
                folder = "Finance_Vault",
                tags = "tax,gst,audit,confidential",
                rawTextContent = "FY2026 Q4 GST & Income Tax Summary:\n• Total Revenue: INR 12,80,000\n• Deductible Expenses: INR 4,20,000\n• Net Taxable Profit: INR 8,60,000\n• Status: Verified & Encrypted inside Chhotu In-App Vault.",
                aiSummary = "Gemini AI Summary: Q4 Tax & GST Audit Report (INR 12.8L Revenue). Stored securely inside app."
            )
            saveFileToInAppVault(
                fileId = "vault_biz_plan",
                fileName = "D2C_Brand_Strategy_2026.docx",
                mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                folder = "Business_Docs",
                tags = "business,d2c,marketing,strategy",
                rawTextContent = "Bharat Organic Spices D2C Expansion Plan:\n1. Launch WhatsApp ordering catalog\n2. YouTube Shorts & Instagram Reels creator campaign\n3. Target ROI: 2.8x in Q1.",
                aiSummary = "Gemini AI Summary: D2C Brand Strategy & Marketing Roadmap stored in In-App Encrypted Vault."
            )
            saveFileToInAppVault(
                fileId = "vault_api_notes",
                fileName = "Private_Server_Architecture_Notes.txt",
                mimeType = "text/plain",
                folder = "Coding_Vault",
                tags = "architecture,security,keystore",
                rawTextContent = "Zero-Trust Architecture Checklist:\n- Android Keystore AES-256-GCM enabled\n- BiometricPrompt + Fallback PIN active\n- PromptInjectionGuard active on all external inputs.",
                aiSummary = "Gemini AI Summary: Security & Architecture notes encrypted with Android Keystore."
            )
        }
    }
}
