package io.github.martystev.motionsentinel

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

class AlarmPlayer(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private val loop = object : Runnable {
        override fun run() {
            if (!running) return
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 400)
            handler.postDelayed(this, 700)
        }
    }

    fun start() {
        if (running) return
        running = true
        toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        handler.post(loop)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(loop)
        toneGenerator?.release()
        toneGenerator = null
    }
}
