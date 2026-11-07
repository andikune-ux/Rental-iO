package dev.andikune.rentalio.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.max
import kotlin.math.min

data class ParallaxOffset(
    val translationX: Float = 0f,
    val translationY: Float = 0f
)

class GyroscopeHelper(context: Context, private val maxOffsetPx: Float = 50f) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _offsetFlow = MutableStateFlow(ParallaxOffset())
    val offsetFlow: StateFlow<ParallaxOffset> = _offsetFlow.asStateFlow()

    private var currentX = 0f
    private var currentY = 0f
    private val filterFactor = 0.12f
    private var isListening = false
    private var lastEventNanos = 0L

    fun startListening() {
        if (isListening) return
        // Accelerometer lebih stabil untuk efek parallax berbasis kemiringan.
        val sensor = accelSensor ?: gyroSensor ?: return
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        isListening = true
        lastEventNanos = 0L
    }

    fun stopListening() {
        if (!isListening) return
        sensorManager.unregisterListener(this)
        isListening = false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val now = event.timestamp
        val deltaSeconds = if (lastEventNanos == 0L) {
            0f
        } else {
            ((now - lastEventNanos).coerceAtLeast(0L) / 1_000_000_000f).coerceAtMost(0.1f)
        }
        lastEventNanos = now

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Arah gravitasi: x = kemiringan kiri/kanan, y = depan/belakang.
                val tiltX = (event.values[1] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)
                val tiltY = (event.values[0] / SensorManager.GRAVITY_EARTH).coerceIn(-1f, 1f)

                val targetX = -tiltX * maxOffsetPx
                val targetY = tiltY * maxOffsetPx

                currentX = lerp(currentX, targetX, filterFactor)
                currentY = lerp(currentY, targetY, filterFactor)
            }

            Sensor.TYPE_GYROSCOPE -> {
                // Fallback perangkat tanpa accelerometer: integrasikan kecepatan
                // sudut dengan decay agar tidak melonjak saat berhenti bergerak.
                val gyroX = event.values[0]
                val gyroY = event.values[1]
                currentX += -gyroY * deltaSeconds * maxOffsetPx * 2.5f
                currentY += -gyroX * deltaSeconds * maxOffsetPx * 2.5f
                currentX *= 0.98f
                currentY *= 0.98f
            }
        }

        val clampedX = max(-maxOffsetPx, min(maxOffsetPx, currentX))
        val clampedY = max(-maxOffsetPx, min(maxOffsetPx, currentY))

        _offsetFlow.value = ParallaxOffset(clampedX, clampedY)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
    }
}
