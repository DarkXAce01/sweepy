package io.github.darkxace01.sweepy.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.annotation.PluralsRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.darkxace01.sweepy.MainActivity
import io.github.darkxace01.sweepy.R
import io.github.darkxace01.sweepy.SweepyApplication
import io.github.darkxace01.sweepy.data.Decision
import io.github.darkxace01.sweepy.data.DeckSource
import io.github.darkxace01.sweepy.data.MediaAccess
import io.github.darkxace01.sweepy.data.MediaPermissions
import io.github.darkxace01.sweepy.data.decisionFor
import io.github.darkxace01.sweepy.data.reminders.Reminder
import io.github.darkxace01.sweepy.data.reminders.ReminderFacts
import io.github.darkxace01.sweepy.data.reminders.ReminderKind
import io.github.darkxace01.sweepy.data.reminders.ReminderTiming
import io.github.darkxace01.sweepy.data.reminders.Reminders
import io.github.darkxace01.sweepy.ui.components.formatCount
import io.github.darkxace01.sweepy.ui.formatSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Wakes up every few hours (Android picks the exact moment, to save battery), looks at the gallery
 * on the phone and, if the rules in [Reminders] allow it, posts one notification. Nothing is sent anywhere.
 */
class ReminderJobService : JobService() {
    private var work: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        work = CoroutineScope(Dispatchers.IO).launch {
            runCatching { ReminderCheck.run(applicationContext) }
            jobFinished(params, false)
        }
        return true
    }

    // The job repeats on its own, so there's nothing to retry.
    override fun onStopJob(params: JobParameters): Boolean {
        work?.cancel()
        return false
    }
}

object ReminderSchedule {
    private const val JOB_ID = 4_201
    private const val EVERY = 6L * 60 * 60 * 1000
    private const val FLEX = 2L * 60 * 60 * 1000

    /** Starts the periodic check when reminders are on and stops it when they're off. */
    fun sync(context: Context, enabled: Boolean) {
        val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
        if (!enabled) {
            scheduler.cancel(JOB_ID)
            return
        }
        if (scheduler.getPendingJob(JOB_ID) != null) return
        scheduler.schedule(
            JobInfo.Builder(JOB_ID, ComponentName(context, ReminderJobService::class.java))
                .setPeriodic(EVERY, FLEX)
                .setRequiresBatteryNotLow(true)
                .setPersisted(true) // survives a restart of the phone
                .build(),
        )
    }
}

/** Where a tapped notification should take you. Read by [MainActivity]. */
object ReminderLink {
    const val EXTRA = "io.github.darkxace01.sweepy.OPEN"

    fun intent(context: Context, kind: ReminderKind): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA, kind.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun kindOf(intent: Intent?): ReminderKind? =
        intent?.getStringExtra(EXTRA)?.let { name -> ReminderKind.entries.firstOrNull { it.name == name } }

    /** The deck to open for [kind]; null means the review bin. */
    fun deckFor(kind: ReminderKind): DeckSource? = when (kind) {
        ReminderKind.STORAGE -> DeckSource.LargeVideos
        ReminderKind.SCREENSHOTS -> DeckSource.Screenshots
        ReminderKind.NEW_PHOTOS -> DeckSource.All
        ReminderKind.BIN -> null
    }
}

internal object ReminderCheck {
    private const val CHANNEL = "reminders"
    private const val NOTIFICATION_ID = 1

    suspend fun run(context: Context) {
        val container = (context.applicationContext as SweepyApplication).container
        val prefs = container.prefs
        if (!prefs.settings.value.reminders || !canNotify(context)) return
        if (MediaPermissions.current(context) == MediaAccess.NONE) return

        val now = System.currentTimeMillis()
        val timing = ReminderTiming(
            now = now,
            hour = LocalTime.now().hour,
            lastOpenedAt = prefs.lastOpenedAt,
            lastReminderAt = prefs.lastReminderAt,
        )
        // Cheap checks first: most wake-ups end here without touching the gallery.
        if (!Reminders.mayCheck(timing)) return

        val media = container.media.loadAll()
        container.decisions.ensureLoaded()
        val decisions = container.decisions.state.value.orEmpty()
        val unsorted = media.filter { decisions.decisionFor(it) == null }
        val bin = media.filter { decisions.decisionFor(it) == Decision.DELETE }
        val openedSeconds = timing.lastOpenedAt / 1000
        val fresh = unsorted.filter { it.dateAddedSeconds > openedSeconds }

        val facts = ReminderFacts(
            binCount = bin.size,
            binBytes = bin.sumOf { it.sizeBytes },
            binNewestAt = bin.maxOfOrNull { decisions[it.id]?.decidedAt ?: 0L } ?: 0L,
            newSinceOpen = fresh.size,
            newScreenshotsSinceOpen = fresh.count { DeckSource.Screenshots.matches(it) },
            percentFull = percentFull(),
            bigVideoBytes = unsorted.filter { DeckSource.LargeVideos.matches(it) }.sumOf { it.sizeBytes },
        )
        val reminder = Reminders.pick(timing, facts) ?: return
        val wording = (prefs.lastReminderWording + 1).mod(WORDINGS)
        if (show(context, reminder, wording)) prefs.markReminderSent(wording, now)
    }

    fun canNotify(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                )

    private fun percentFull(): Int {
        val stat = StatFs(Environment.getDataDirectory().path)
        if (stat.totalBytes <= 0) return 0
        return (100 - stat.availableBytes * 100 / stat.totalBytes).toInt()
    }

    private fun show(context: Context, reminder: Reminder, wording: Int): Boolean {
        if (!canNotify(context)) return false
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = context.getString(R.string.reminder_channel_body) },
        )
        val (title, body) = words(context, reminder, wording)
        val tap = PendingIntent.getActivity(
            context,
            reminder.kind.ordinal,
            ReminderLink.intent(context, reminder.kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(context.getColor(R.color.ic_launcher_accent))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        return true
    }

    private const val WORDINGS = 3

    private val storage = listOf(
        R.string.reminder_storage_title_0 to R.string.reminder_storage_body_0,
        R.string.reminder_storage_title_1 to R.string.reminder_storage_body_1,
        R.string.reminder_storage_title_2 to R.string.reminder_storage_body_2,
    )
    private val bin = listOf(
        R.plurals.reminder_bin_title_0 to R.plurals.reminder_bin_body_0,
        R.plurals.reminder_bin_title_1 to R.plurals.reminder_bin_body_1,
        R.plurals.reminder_bin_title_2 to R.plurals.reminder_bin_body_2,
    )
    private val screenshots = listOf(
        R.plurals.reminder_screenshots_title_0 to R.plurals.reminder_screenshots_body_0,
        R.plurals.reminder_screenshots_title_1 to R.plurals.reminder_screenshots_body_1,
        R.plurals.reminder_screenshots_title_2 to R.plurals.reminder_screenshots_body_2,
    )
    private val fresh = listOf(
        R.plurals.reminder_new_title_0 to R.plurals.reminder_new_body_0,
        R.plurals.reminder_new_title_1 to R.plurals.reminder_new_body_1,
        R.plurals.reminder_new_title_2 to R.plurals.reminder_new_body_2,
    )

    /** Title and text for [reminder], in the [wording]th of its three phrasings. */
    fun words(context: Context, reminder: Reminder, wording: Int): Pair<String, String> {
        val res = context.resources
        val size = formatSize(context, reminder.bytes)
        if (reminder.kind == ReminderKind.STORAGE) {
            val (title, body) = storage[wording]
            return res.getString(title, reminder.percentFull) to res.getString(body, size)
        }
        val (title, body) = when (reminder.kind) {
            ReminderKind.BIN -> bin[wording]
            ReminderKind.SCREENSHOTS -> screenshots[wording]
            else -> fresh[wording]
        }
        return res.plural(title, reminder.count, size) to res.plural(body, reminder.count, size)
    }

    private fun Resources.plural(@PluralsRes id: Int, count: Int, size: String) =
        getQuantityString(id, count, formatCount(count), size)
}
