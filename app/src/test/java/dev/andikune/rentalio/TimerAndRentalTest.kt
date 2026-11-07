package dev.andikune.rentalio

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.andikune.rentalio.data.local.RentalPreferences
import dev.andikune.rentalio.service.KioskForegroundService
import dev.andikune.rentalio.utils.SecurityHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TimerAndRentalTest {

    private lateinit var context: Context
    private lateinit var preferences: RentalPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferences = RentalPreferences(context)
    }

    @Test
    fun `test custom duration calculation total seconds`() {
        val hours = 2
        val minutes = 30
        val seconds = 15
        val bonusMinutes = 10

        val totalSeconds = (hours * 3600L) + (minutes * 60L) + seconds + (bonusMinutes * 60L)
        // 2 * 3600 = 7200
        // 30 * 60 = 1800
        // 15
        // 10 * 60 = 600
        // total = 7200 + 1800 + 15 + 600 = 9615
        assertEquals(9615L, totalSeconds)

        val formatted = SecurityHelper.formatDuration(totalSeconds)
        assertEquals("02j : 40m : 15s", formatted)

        val formattedDigital = SecurityHelper.formatTimeDigital(totalSeconds)
        assertEquals("02:40:15", formattedDigital)
    }

    @Test
    fun `test unlimited mode billing starting from zero`() {
        // At 0 seconds, cost should be 0
        val zeroCost = SecurityHelper.calculateUnlimitedCost(0L)
        assertEquals(0L, zeroCost)

        // At 59 seconds, minutes = 59/60 = 0 -> cost = 0
        val underMinuteCost = SecurityHelper.calculateUnlimitedCost(59L)
        assertEquals(0L, underMinuteCost)

        // At 60 seconds (1 minute), cost should be 100
        val oneMinuteCost = SecurityHelper.calculateUnlimitedCost(60L)
        assertEquals(100L, oneMinuteCost)

        // At 3600 seconds (60 minutes), cost should be 6000
        val oneHourCost = SecurityHelper.calculateUnlimitedCost(3600L)
        assertEquals(6000L, oneHourCost)
    }

    @Test
    fun `test admin pin verification`() {
        preferences.saveAdminPin("123456")
        val currentPin = preferences.getSettings().adminPin
        assertEquals("123456", currentPin)

        assertTrue(currentPin == "123456")
        assertFalse(currentPin == "000000")
        assertFalse(currentPin == "8899")

        preferences.saveAdminPin("889900")
        assertEquals("889900", preferences.getSettings().adminPin)
    }

    @Test
    fun `test kiosk service action constants`() {
        assertEquals("dev.andikune.rentalio.action.START_TIMER", KioskForegroundService.ACTION_START)
        assertEquals("dev.andikune.rentalio.action.STOP_TIMER", KioskForegroundService.ACTION_STOP)
        assertEquals("dev.andikune.rentalio.action.ADD_TIME", KioskForegroundService.ACTION_ADD_TIME)
        assertEquals("dev.andikune.rentalio.action.SET_TIME", KioskForegroundService.ACTION_SET_TIME)
        assertEquals("dev.andikune.rentalio.action.DISMISS_ALARM", KioskForegroundService.ACTION_DISMISS_ALARM)
    }
}
