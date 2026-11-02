package io.github.darkxace01.sweepy

import androidx.test.core.app.ApplicationProvider
import io.github.darkxace01.sweepy.data.reminders.Reminder
import io.github.darkxace01.sweepy.data.reminders.ReminderKind
import io.github.darkxace01.sweepy.reminders.ReminderCheck
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Every wording of every reminder fills in its numbers, with nothing left over like "%1$s". */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ReminderWordsTest {
    @Test fun allWordingsFillIn() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val lines = mutableListOf<String>()
        for (kind in ReminderKind.entries) {
            for (count in listOf(1, 214)) {
                val reminder = Reminder(kind, count = count, bytes = 1_340_000_000, percentFull = 93)
                for (wording in 0..2) {
                    val (title, body) = ReminderCheck.words(context, reminder, wording)
                    for (text in listOf(title, body)) {
                        assertTrue(text.isNotBlank())
                        assertFalse(text, text.contains('%'.toString() + "1") || text.contains('%'.toString() + "2"))
                    }
                    if (count > 1) lines += "$kind/$wording  $title — $body"
                }
            }
        }
        println(lines.joinToString("\n"))
    }
}
