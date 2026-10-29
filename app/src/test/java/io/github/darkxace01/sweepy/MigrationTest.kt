package io.github.darkxace01.sweepy

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.darkxace01.sweepy.data.Decision
import io.github.darkxace01.sweepy.data.db.SweepyDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Updating the app must never lose the user's swipes. */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @Test fun `version 1 decisions survive the upgrade to version 2`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "migration-test.db"
        context.deleteDatabase(name)

        // Recreate exactly what version 1 of the app wrote (see app/schemas/.../1.json).
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `decisions` (`mediaId` INTEGER NOT NULL, `decision` TEXT NOT NULL, " +
                    "`fingerprint` TEXT NOT NULL, `decidedAt` INTEGER NOT NULL, PRIMARY KEY(`mediaId`))",
            )
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'def5d60a92e250e35bf0975eac69e9a9')")
            db.execSQL("INSERT INTO decisions VALUES (7, 'DELETE', '100:5', 123)")
            db.version = 1
        }

        // Room checks the upgraded schema against the current entities when it opens.
        val database = Room.databaseBuilder(context, SweepyDatabase::class.java, name)
            .addMigrations(SweepyDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        runBlocking {
            val decisions = database.decisions().all()
            assertEquals(1, decisions.size)
            assertEquals(Decision.DELETE, decisions[0].decision)
            assertEquals(0, database.duplicates().hashes().size)
        }
        database.close()
    }
}
