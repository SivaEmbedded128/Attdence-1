package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.BackupJsonCodec
import com.example.domain.DemoDataSeed
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read AttendX strings and firestore database id from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("AttendX", context.getString(R.string.app_name))
        assertTrue(context.getString(R.string.firestore_database_id).isNotBlank())
    }

    @Test
    fun `backup json codec roundtrip preserves subjects and records`() {
        val seed = DemoDataSeed.createSeedData("test_user")
        val encoded = BackupJsonCodec.encode(seed)
        val decoded = BackupJsonCodec.decode(encoded, "test_user")
        assertEquals(seed.subjects.size, decoded.subjects.size)
        assertEquals(seed.timetableSlots.size, decoded.timetableSlots.size)
        assertEquals(seed.attendanceRecords.size, decoded.attendanceRecords.size)
        assertEquals(seed.holidays.size, decoded.holidays.size)
    }
}
