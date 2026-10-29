package io.github.darkxace01.sweepy.data

import android.content.Context
import androidx.room.Room
import io.github.darkxace01.sweepy.data.db.SweepyDatabase
import io.github.darkxace01.sweepy.data.duplicates.DuplicateScanner
import io.github.darkxace01.sweepy.data.duplicates.MediaHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Creates the app-wide singletons once. (A tiny hand-written version of dependency injection.) */
class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database = Room.databaseBuilder(context, SweepyDatabase::class.java, "sweepy.db")
        .addMigrations(SweepyDatabase.MIGRATION_1_2)
        .build()

    val media = MediaRepository(context)
    val decisions = DecisionStore(database.decisions(), appScope)
    val prefs = Prefs(context)
    val duplicateDao = database.duplicates()
    val duplicates = DuplicateScanner(duplicateDao, MediaHasher(context.contentResolver))
}
