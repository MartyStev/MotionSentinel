package io.github.martystev.motionsentinel

import android.hardware.Sensor
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

class MotionDetector(
    private val sensitivity: Int,
    private val onMotion: (MotionEvent) -> Unit
) {
    private var lastAccel: FloatArray? = null

    fun onSensorChanged(sensorType: Int, values: FloatArray) {
        when (sensorType) {
            Sensor.TYPE_GYROSCOPE -> handleGyro(values)
            Sensor.TYPE_ACCELEROMETER -> handleAccel(values)
        }
    }

    private fun handleGyro(values: FloatArray) {
        val magnitude = sqrt(values[0].pow(2) + values[1].pow(2) + values[2].pow(2))
        val threshold = mapSensitivity(sensitivity, min = 0.2f, max = 1.5f)
        if (magnitude >= threshold) {
            onMotion(
                MotionEvent(
                    type = MotionType.ROTATION,
                    angleDeg = 0f,
                    acceleration = magnitude
                )
            )
        }
    }

    private fun handleAccel(values: FloatArray) {
        val x = values[0]
        val y = values[1]
        val z = values[2]
        val mag = sqrt(x * x + y * y + z * z)
        val gravity = 9.81f
        val deltaFromGravity = abs(mag - gravity)

        val last = lastAccel
        val deltaFromLast = if (last != null) {
            val dx = x - last[0]
            val dy = y - last[1]
            val dz = z - last[2]
            sqrt(dx * dx + dy * dy + dz * dz)
        } else {
            0f
        }
        lastAccel = floatArrayOf(x, y, z)

        val threshold = mapSensitivity(sensitivity, min = 0.3f, max = 2.2f)
        if (deltaFromGravity >= threshold || deltaFromLast >= threshold) {
            val angle = if (mag > 0f) {
                Math.toDegrees(acos((z / mag).toDouble())).toFloat()
            } else 0f
            onMotion(
                MotionEvent(
                    type = MotionType.TILT_OR_VIBRATION,
                    angleDeg = angle,
                    acceleration = deltaFromGravity
                )
            )
        }
    }

    private fun mapSensitivity(value: Int, min: Float, max: Float): Float {
        val clamped = value.coerceIn(0, 100)
        val t = (100 - clamped) / 100f
        return min + (max - min) * t
    }
}

enum class MotionType {
    ROTATION,
    TILT_OR_VIBRATION
}

data class MotionEvent(
    val type: MotionType,
    val angleDeg: Float,
    val acceleration: Float
)
