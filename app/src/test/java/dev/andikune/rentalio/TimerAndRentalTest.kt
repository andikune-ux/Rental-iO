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
        // 30 menit pertama flat Rp 3.000
        val zeroCostExact = SecurityHelper.calculateUnlimitedExactCost(0L)
        assertEquals(3000L, zeroCostExact)

        val zeroCostRounded = SecurityHelper.calculateUnlimitedCost(0L)
        assertEquals(3000L, zeroCostRounded)

        val under30MinCost = SecurityHelper.calculateUnlimitedCost(25 * 60L)
        assertEquals(3000L, under30MinCost)

        val thirtyMinCost = SecurityHelper.calculateUnlimitedCost(30 * 60L)
        assertEquals(3000L, thirtyMinCost)

        // 31 menit: 3000 + 100 = 3100 -> dibulatkan jadi 3000
        val thirtyOneMinExact = SecurityHelper.calculateUnlimitedExactCost(31 * 60L)
        assertEquals(3100L, thirtyOneMinExact)
        val thirtyOneMinCost = SecurityHelper.calculateUnlimitedCost(31 * 60L)
        assertEquals(3000L, thirtyOneMinCost)

        // 34 menit: 3000 + 400 = 3400 -> dibulatkan jadi 3000
        val thirtyFourMinCost = SecurityHelper.calculateUnlimitedCost(34 * 60L)
        assertEquals(3000L, thirtyFourMinCost)

        // 35 menit: 3000 + 500 = 3500 -> pas 3500
        val thirtyFiveMinCost = SecurityHelper.calculateUnlimitedCost(35 * 60L)
        assertEquals(3500L, thirtyFiveMinCost)

        // 36 menit: 3000 + 600 = 3600 -> dibulatkan jadi 3500
        val thirtySixMinExact = SecurityHelper.calculateUnlimitedExactCost(36 * 60L)
        assertEquals(3600L, thirtySixMinExact)
        val thirtySixMinCost = SecurityHelper.calculateUnlimitedCost(36 * 60L)
        assertEquals(3500L, thirtySixMinCost)

        // 39 menit: 3000 + 900 = 3900 -> dibulatkan jadi 3500
        val thirtyNineMinCost = SecurityHelper.calculateUnlimitedCost(39 * 60L)
        assertEquals(3500L, thirtyNineMinCost)

        // 60 menit: 3000 + (30 * 100) = 6000
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

    @Test
    fun `test brute force failed attempts threshold logic`() {
        var failedAttempts = 0
        var cooldownSeconds = 0

        fun onWrongPin() {
            failedAttempts++
            if (failedAttempts >= 5) {
                cooldownSeconds = 30
                failedAttempts = 0
            }
        }

        repeat(4) { onWrongPin() }
        assertEquals(4, failedAttempts)
        assertEquals(0, cooldownSeconds)

        onWrongPin() // 5th attempt
        assertEquals(0, failedAttempts)
        assertEquals(30, cooldownSeconds)
    }

    @Test
    fun `test whitelist lock task array generation`() {
        val appPkg = context.packageName
        val whitelisted = listOf("com.mobile.legends", "com.dts.freefireth")
        val allPackages = (listOf(appPkg) + whitelisted).distinct().toTypedArray()

        assertTrue(allPackages.contains(appPkg))
        assertTrue(allPackages.contains("com.mobile.legends"))
        assertTrue(allPackages.contains("com.dts.freefireth"))
        assertEquals(3, allPackages.size)
    }
}
