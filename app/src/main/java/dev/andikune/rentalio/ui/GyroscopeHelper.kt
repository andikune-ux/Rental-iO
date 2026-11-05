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
    private val filterFactor = 0.10f // Smoothing low-pass filter
    private var isUsingGyro = false

    fun startListening() {
        if (gyroSensor != null) {
            sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_GAME)
            isUsingGyro = true
        } else if (accelSensor != null) {
            sensorManager.registerListener(this, accelSensor, SensorManager.SENSOR_DELAY_GAME)
            isUsingGyro = false
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            // Gyroscope angular speed (rad/s)
            // In Landscape orientation:
            // event.values[0] is X axis (pitch), event.values[1] is Y axis (roll), event.values[2] is Z axis (yaw)
            val gyroX = event.values[0]
            val gyroY = event.values[1]

            // Integrate and translate opposite to tilt direction for parallax depth
            val targetX = -gyroY * (maxOffsetPx * 1.2f)
            val targetY = -gyroX * (maxOffsetPx * 1.2f)

            currentX = lerp(currentX, targetX, filterFactor)
            currentY = lerp(currentY, targetY, filterFactor)
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Accelerometer gravity tilt
            // In landscape: values[1] correlates with lateral tilt, values[0] with vertical
            val tiltX = event.values[1] / 9.8f
            val tiltY = (event.values[0] - 5f) / 9.8f

            val targetX = -tiltX * maxOffsetPx
            val targetY = tiltY * maxOffsetPx

            currentX = lerp(currentX, targetX, filterFactor)
            currentY = lerp(currentY, targetY, filterFactor)
        }

        // Clamp to max offset limits
        val clampedX = max(-maxOffsetPx, min(maxOffsetPx, currentX))
        val clampedY = max(-maxOffsetPx, min(maxOffsetPx, currentY))

        _offsetFlow.value = ParallaxOffset(clampedX, clampedY)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun lerp(start: Float, stop: Float, amount: Float): Float {
        return start + (stop - start) * amount
    }
}
