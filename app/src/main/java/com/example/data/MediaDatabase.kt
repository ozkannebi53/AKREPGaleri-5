package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "media_files")
data class MediaFile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uri: String,
    val name: String,
    val type: String, // "IMAGE", "VIDEO", "AUDIO"
    val size: Long,
    val dateAdded: Long,
    val duration: Long = 0L,
    val objectClass: String? = null, // "Araba", "Manzara", "Yemek"
    val isScreenshot: Boolean = false,
    val faceGroupId: String? = null,
    val isInVault: Boolean = false,
    val resourceId: Int = 0, // Reference to app resources for pre-loaded content
    val location: String? = null,
    val event: String? = null
)

@Entity(tableName = "face_groups")
data class FaceGroup(
    @PrimaryKey val id: String,
    val name: String,
    val avatarResourceId: Int = 0
)

@Entity(tableName = "subtitle_caches")
data class SubtitleCache(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mediaId: Int,
    val timestampMs: Long,
    val originalText: String,
    val translatedText: String,
    val language: String // "TR_AR", "AR_TR"
)

@Dao
interface MediaDao {
    @Query("SELECT * FROM media_files WHERE isInVault = 0 ORDER BY dateAdded DESC")
    fun getPublicMedia(): Flow<List<MediaFile>>

    @Query("SELECT * FROM media_files WHERE isInVault = 1 ORDER BY dateAdded DESC")
    fun getVaultMedia(): Flow<List<MediaFile>>

    @Query("SELECT * FROM media_files WHERE type = 'IMAGE' AND isInVault = 0 ORDER BY dateAdded DESC")
    fun getPublicImages(): Flow<List<MediaFile>>

    @Query("SELECT * FROM media_files WHERE type = 'VIDEO' AND isInVault = 0 ORDER BY dateAdded DESC")
    fun getPublicVideos(): Flow<List<MediaFile>>

    @Query("SELECT * FROM media_files WHERE type = 'AUDIO' AND isInVault = 0 ORDER BY dateAdded DESC")
    fun getPublicAudio(): Flow<List<MediaFile>>

    @Query("SELECT * FROM media_files WHERE isScreenshot = 1 AND isInVault = 0 ORDER BY dateAdded DESC")
    fun getScreenshots(): Flow<List<MediaFile>>

    @Query("SELECT * FROM media_files WHERE objectClass = :category AND isInVault = 0 ORDER BY dateAdded DESC")
    fun getByCategory(category: String): Flow<List<MediaFile>>

    @Query("SELECT * FROM media_files WHERE faceGroupId = :groupId AND isInVault = 0 ORDER BY dateAdded DESC")
    fun getByFaceGroup(groupId: String): Flow<List<MediaFile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedia(media: MediaFile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMediaList(mediaList: List<MediaFile>)

    @Update
    suspend fun updateMedia(media: MediaFile)

    @Query("UPDATE media_files SET isInVault = :isInVault WHERE id = :id")
    suspend fun updateVaultStatus(id: Int, isInVault: Boolean)

    @Query("DELETE FROM media_files WHERE id = :id")
    suspend fun deleteMedia(id: Int)

    // Face Groups
    @Query("SELECT * FROM face_groups")
    fun getAllFaceGroups(): Flow<List<FaceGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaceGroup(faceGroup: FaceGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaceGroups(groups: List<FaceGroup>)

    // Subtitle Cache
    @Query("SELECT * FROM subtitle_caches WHERE mediaId = :mediaId")
    suspend fun getSubtitlesForMedia(mediaId: Int): List<SubtitleCache>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitle(subtitle: SubtitleCache)
}

@Database(entities = [MediaFile::class, FaceGroup::class, SubtitleCache::class], version = 1, exportSchema = false)
abstract class MediaDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao

    companion object {
        @Volatile
        private var INSTANCE: MediaDatabase? = null

        fun getDatabase(context: Context): MediaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediaDatabase::class.java,
                    "scorpio_gallery_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MediaRepository(private val mediaDao: MediaDao) {
    val publicMedia: Flow<List<MediaFile>> = mediaDao.getPublicMedia()
    val vaultMedia: Flow<List<MediaFile>> = mediaDao.getVaultMedia()
    val publicImages: Flow<List<MediaFile>> = mediaDao.getPublicImages()
    val publicVideos: Flow<List<MediaFile>> = mediaDao.getPublicVideos()
    val publicAudio: Flow<List<MediaFile>> = mediaDao.getPublicAudio()
    val screenshots: Flow<List<MediaFile>> = mediaDao.getScreenshots()
    val faceGroups: Flow<List<FaceGroup>> = mediaDao.getAllFaceGroups()

    fun getByCategory(category: String): Flow<List<MediaFile>> = mediaDao.getByCategory(category)
    fun getByFaceGroup(groupId: String): Flow<List<MediaFile>> = mediaDao.getByFaceGroup(groupId)

    suspend fun insertMedia(media: MediaFile) = mediaDao.insertMedia(media)
    suspend fun insertMediaList(mediaList: List<MediaFile>) = mediaDao.insertMediaList(mediaList)
    suspend fun updateMedia(media: MediaFile) = mediaDao.updateMedia(media)
    suspend fun updateVaultStatus(id: Int, isInVault: Boolean) = mediaDao.updateVaultStatus(id, isInVault)
    suspend fun deleteMedia(id: Int) = mediaDao.deleteMedia(id)

    suspend fun insertFaceGroup(group: FaceGroup) = mediaDao.insertFaceGroup(group)
    suspend fun insertFaceGroups(groups: List<FaceGroup>) = mediaDao.insertFaceGroups(groups)

    suspend fun getSubtitles(mediaId: Int) = mediaDao.getSubtitlesForMedia(mediaId)
    suspend fun insertSubtitle(sub: SubtitleCache) = mediaDao.insertSubtitle(sub)
}
