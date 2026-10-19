package io.github.martystev.motionsentinel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlertManager(
    private val telegramSender: TelegramSender,
    private val gpsProvider: GpsProvider,
    private val repeatIntervalMs: Long,
    private val gpsDelayMs: Long,
    private val onAlertStarted: () -> Unit,
    private val onAlertStopped: () -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var alertActive = false
    private var gpsSent = false
    private var repeatJob: Job? = null
    private var gpsJob: Job? = null
    private var lastEvent: MotionEvent? = null

    fun onMovement(event: MotionEvent) {
        lastEvent = event
        if (!alertActive) {
            alertActive = true
            gpsSent = false
            sendFirstAlert(event)
            onAlertStarted()
            startRepeatNotifications()
            startGpsTimer()
        }
    }

    fun onMovementStopped() {
        if (!alertActive) return
        alertActive = false
        repeatJob?.cancel()
        repeatJob = null
        gpsJob?.cancel()
        gpsJob = null
        onAlertStopped()
    }

    fun shutdown() {
        repeatJob?.cancel()
        gpsJob?.cancel()
        scope.cancel()
    }

    private fun sendFirstAlert(event: MotionEvent) {
        val typeText = if (event.type == MotionType.ROTATION) "вибрация" else "наклон/вибрация"
        val text = buildString {
            append("🚨 Обнаружено движение!\n")
            append("Тип: ").append(typeText).append("\n")
            append("Угол: ").append(event.angleDeg.formatOne()).append("°\n")
            append("Ускорение: ").append(event.acceleration.formatOne())
        }
        telegramSender.sendMessage(text)
    }

    private fun startRepeatNotifications() {
        repeatJob?.cancel()
        repeatJob = scope.launch {
            while (alertActive) {
                delay(repeatIntervalMs)
                val event = lastEvent ?: continue
                val text = buildString {
                    append("🚨 Движение продолжается\n")
                    append("Угол: ").append(event.angleDeg.formatOne()).append("°\n")
                    append("Ускорение: ").append(event.acceleration.formatOne())
                }
                telegramSender.sendMessage(text)
            }
        }
    }

    private fun startGpsTimer() {
        gpsJob?.cancel()
        gpsJob = scope.launch {
            delay(gpsDelayMs)
            if (!alertActive || gpsSent) return@launch
            gpsSent = true
            gpsProvider.getCurrentLocation { location ->
                val text = if (location != null) {
                    "📍 Координаты:\nlat: ${location.latitude}\nlon: ${location.longitude}"
                } else {
                    "⚠️ Не удалось получить GPS координаты"
                }
                telegramSender.sendMessage(text)
            }
        }
    }

    private fun Float.formatOne(): String = String.format("%.1f", this)
}
