package io.github.darkxace01.sweepy.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
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

@Database(entities = [DecisionEntity::class], version = 1)
abstract class SweepyDatabase : RoomDatabase() {
    abstract fun decisions(): DecisionDao
}
