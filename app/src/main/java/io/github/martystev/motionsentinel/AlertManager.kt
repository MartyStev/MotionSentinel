package io.github.martystev.motionsentinel

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AlertManager(
    private val context: Context,
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
        val typeText = context.getString(
            if (event.type == MotionType.ROTATION) R.string.alert_type_vibration else R.string.alert_type_tilt
        )
        telegramSender.sendMessage(
            context.getString(R.string.alert_first, typeText, event.angleDeg.formatOne(), event.acceleration.formatOne())
        )
    }

    private fun startRepeatNotifications() {
        repeatJob?.cancel()
        repeatJob = scope.launch {
            while (alertActive) {
                delay(repeatIntervalMs)
                val event = lastEvent ?: continue
                val text = context.getString(R.string.alert_repeat, event.angleDeg.formatOne(), event.acceleration.formatOne())
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
                    context.getString(R.string.alert_gps, location.latitude.toString(), location.longitude.toString())
                } else {
                    context.getString(R.string.alert_gps_failed)
                }
                telegramSender.sendMessage(text)
            }
        }
    }

    private fun Float.formatOne(): String = String.format("%.1f", this)
}
