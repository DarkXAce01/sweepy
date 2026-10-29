package io.github.darkxace01.sweepy.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.darkxace01.sweepy.data.Decision

/** One row per photo/video the user has decided on: the global "decision ledger". */
@Entity(tableName = "decisions")
data class DecisionEntity(
    @PrimaryKey val mediaId: Long,
    val decision: Decision,
    /** See [io.github.darkxace01.sweepy.data.MediaItem.fingerprint]. */
    val fingerprint: String,
    val decidedAt: Long,
)

@Dao
interface DecisionDao {
    @Query("SELECT * FROM decisions")
    suspend fun all(): List<DecisionEntity>

    @Upsert
    suspend fun upsert(entities: List<DecisionEntity>)

    @Query("DELETE FROM decisions WHERE mediaId IN (:ids)")
    suspend fun delete(ids: List<Long>)
}

/**
 * Cached results of the duplicate scan, so each file is only read once. Each field is filled in
 * only when needed: [visualHash] for every photo, [contentHash] only for files that share a size
 * with another file, [sharpness] only for photos that turn out to be look-alikes.
 */
@Entity(tableName = "media_hashes")
data class MediaHashEntity(
    @PrimaryKey val mediaId: Long,
    val fingerprint: String,
    val contentHash: String?,
    val visualHash: Long?,
    val sharpness: Float?,
)

/** Photos the user has already been through in the duplicates screen. */
@Entity(tableName = "duplicate_reviews")
data class DuplicateReviewEntity(
    @PrimaryKey val mediaId: Long,
    val fingerprint: String,
    val reviewedAt: Long,
)

@Dao
interface DuplicateDao {
    @Query("SELECT * FROM media_hashes")
    suspend fun hashes(): List<MediaHashEntity>

    @Upsert
    suspend fun upsertHashes(entities: List<MediaHashEntity>)

    @Query("SELECT * FROM duplicate_reviews")
    suspend fun reviews(): List<DuplicateReviewEntity>

    @Upsert
    suspend fun upsertReviews(entities: List<DuplicateReviewEntity>)

    @Query("DELETE FROM duplicate_reviews WHERE mediaId IN (:ids)")
    suspend fun deleteReviews(ids: List<Long>)

    @Query("DELETE FROM duplicate_reviews")
    suspend fun clearReviews()
}

@Database(
    entities = [DecisionEntity::class, MediaHashEntity::class, DuplicateReviewEntity::class],
    version = 2,
)
abstract class SweepyDatabase : RoomDatabase() {
    abstract fun decisions(): DecisionDao
    abstract fun duplicates(): DuplicateDao

    companion object {
        /** Version 2 adds the duplicate scan cache and review markers. Existing decisions are kept. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `media_hashes` (`mediaId` INTEGER NOT NULL, `fingerprint` TEXT NOT NULL, " +
                        "`contentHash` TEXT, `visualHash` INTEGER, `sharpness` REAL, PRIMARY KEY(`mediaId`))",
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `duplicate_reviews` (`mediaId` INTEGER NOT NULL, `fingerprint` TEXT NOT NULL, " +
                        "`reviewedAt` INTEGER NOT NULL, PRIMARY KEY(`mediaId`))",
                )
            }
        }
    }
}
