package io.github.martystev.motionsentinel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import androidx.core.app.NotificationCompat

class MotionService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var handlerThread: HandlerThread? = null
    private var sensorHandler: Handler? = null

    private lateinit var settings: SettingsManager
    private lateinit var telegramSender: TelegramSender
    private lateinit var gpsProvider: GpsProvider
    private lateinit var alertManager: AlertManager
    private lateinit var alarmPlayer: AlarmPlayer
    private var motionDetector: MotionDetector? = null
    private var inAlert = false
    private var lastMonitoringCheckMs: Long = 0L
    private var activationStartMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        settings = SettingsManager(this)
        telegramSender = TelegramSender(settings)
        gpsProvider = GpsProvider(this)
        alarmPlayer = AlarmPlayer(this)
        alertManager = AlertManager(
            telegramSender,
            gpsProvider,
            repeatIntervalMs = settings.getRepeatIntervalSec().toLong() * 1000L,
            gpsDelayMs = settings.getGpsDelaySec().toLong() * 1000L,
            onAlertStarted = {
                inAlert = true
                if (settings.isHighFreqInAlert()) {
                    restartSensors(SensorManager.SENSOR_DELAY_UI)
                }
                if (settings.isSirenEnabled()) {
                    alarmPlayer.start()
                }
            },
            onAlertStopped = {
                inAlert = false
                restartSensors(SensorManager.SENSOR_DELAY_NORMAL)
                alarmPlayer.stop()
            }
        )

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        handlerThread = HandlerThread("MotionSensors").apply { start() }
        sensorHandler = Handler(handlerThread!!.looper)

        motionDetector = MotionDetector(settings.getSensitivity()) { event ->
            alertManager.onMovement(event)
        }

        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                activationStartMs = System.currentTimeMillis()
                startSensors(SensorManager.SENSOR_DELAY_NORMAL)
            }
            ACTION_STOP -> {
                stopSensors()
                stopSelf()
            }
            else -> {
                activationStartMs = System.currentTimeMillis()
                startSensors(SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
        return START_STICKY
    }

    private fun startSensors(delay: Int) {
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorHandler?.let { handler ->
            accel?.let { sensorManager.registerListener(this, it, delay, handler) }
            gyro?.let { sensorManager.registerListener(this, it, delay, handler) }
        }
    }

    private fun stopSensors() {
        sensorManager.unregisterListener(this)
        alertManager.onMovementStopped()
    }

    override fun onSensorChanged(event: SensorEvent) {
        val activationDelayMs = settings.getActivationDelaySec().toLong() * 1000L
        if (activationDelayMs > 0L) {
            val now = System.currentTimeMillis()
            if (now - activationStartMs < activationDelayMs) {
                return
            }
        }
        if (!inAlert) {
            val intervalMs = settings.getMonitoringIntervalSec().toLong() * 1000L
            val now = System.currentTimeMillis()
            if (now - lastMonitoringCheckMs < intervalMs) {
                return
            }
            lastMonitoringCheckMs = now
        }
        motionDetector?.onSensorChanged(event.sensor.type, event.values.clone())
        // No auto-stop while alert is active; manual stop only.
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    private fun restartSensors(delay: Int) {
        stopSensors()
        startSensors(delay)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIF_CHANNEL_ID)
            .setContentTitle(getString(R.string.status_active))
            .setSmallIcon(R.drawable.ic_bike_notif)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIF_CHANNEL_ID,
                "MotionSentinel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSensors()
        alertManager.shutdown()
        alarmPlayer.stop()
        handlerThread?.quitSafely()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "io.github.martystev.motionsentinel.action.START"
        const val ACTION_STOP = "io.github.martystev.motionsentinel.action.STOP"
        private const val NOTIF_CHANNEL_ID = "motionsentinel"
        private const val NOTIF_ID = 1001
    }
}
