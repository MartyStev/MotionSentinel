package io.github.martystev.motionsentinel

import android.Manifest
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var settings: SettingsManager
    private lateinit var statusText: TextView
    private lateinit var tokenInput: EditText
    private lateinit var chatIdInput: EditText
    private lateinit var sensitivitySeek: SeekBar
    private lateinit var sirenSwitch: SwitchMaterial
    private lateinit var repeatIntervalInput: EditText
    private lateinit var gpsDelayInput: EditText
    private lateinit var monitorIntervalInput: EditText
    private lateinit var highFreqAlertSwitch: SwitchMaterial
    private lateinit var activationDelayInput: EditText
    private lateinit var biometricSwitch: SwitchMaterial
    private var authenticated = false
    private lateinit var lockOverlay: View

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (!hasLocationPermission()) {
            Toast.makeText(this, getString(R.string.gps_permission_needed), Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settings = SettingsManager(this)

        statusText = findViewById(R.id.statusText)
        tokenInput = findViewById(R.id.tokenInput)
        chatIdInput = findViewById(R.id.chatIdInput)
        sensitivitySeek = findViewById(R.id.sensitivitySeek)
        sirenSwitch = findViewById(R.id.sirenSwitch)
        repeatIntervalInput = findViewById(R.id.repeatIntervalInput)
        gpsDelayInput = findViewById(R.id.gpsDelayInput)
        monitorIntervalInput = findViewById(R.id.monitorIntervalInput)
        highFreqAlertSwitch = findViewById(R.id.highFreqAlertSwitch)
        activationDelayInput = findViewById(R.id.activationDelayInput)
        biometricSwitch = findViewById(R.id.biometricSwitch)
        lockOverlay = findViewById(R.id.lockOverlay)

        val activateButton = findViewById<MaterialButton>(R.id.activateButton)
        val deactivateButton = findViewById<MaterialButton>(R.id.deactivateButton)
        val saveButton = findViewById<MaterialButton>(R.id.saveButton)

        tokenInput.setText(settings.getTelegramToken())
        chatIdInput.setText(settings.getTelegramChatId())
        sensitivitySeek.progress = settings.getSensitivity()
        sirenSwitch.isChecked = settings.isSirenEnabled()
        repeatIntervalInput.setText(settings.getRepeatIntervalSec().toString())
        gpsDelayInput.setText(settings.getGpsDelaySec().toString())
        monitorIntervalInput.setText(settings.getMonitoringIntervalSec().toString())
        highFreqAlertSwitch.isChecked = settings.isHighFreqInAlert()
        activationDelayInput.setText(settings.getActivationDelaySec().toString())
        biometricSwitch.isChecked = settings.isBiometricEnabled()

        if (settings.isBiometricEnabled()) {
            lockOverlay.visibility = View.VISIBLE
        }

        activateButton.setOnClickListener {
            requestRuntimePermissions()
            checkGpsEnabled()
            startMotionService()
            statusText.text = getString(R.string.status_active)
        }

        deactivateButton.setOnClickListener {
            stopMotionService()
            statusText.text = getString(R.string.status_inactive)
        }

        saveButton.setOnClickListener {
            settings.setTelegramToken(tokenInput.text.toString())
            settings.setTelegramChatId(chatIdInput.text.toString())
            settings.setSensitivity(sensitivitySeek.progress)
            settings.setSirenEnabled(sirenSwitch.isChecked)
            settings.setRepeatIntervalSec(parseIntOrDefault(repeatIntervalInput.text.toString(), 5))
            settings.setGpsDelaySec(parseIntOrDefault(gpsDelayInput.text.toString(), 60))
            settings.setMonitoringIntervalSec(parseIntOrDefault(monitorIntervalInput.text.toString(), 10))
            settings.setHighFreqInAlert(highFreqAlertSwitch.isChecked)
            settings.setActivationDelaySec(parseIntOrDefault(activationDelayInput.text.toString(), 10))
            settings.setBiometricEnabled(biometricSwitch.isChecked)
        }
    }

    override fun onResume() {
        super.onResume()
        maybeLockApp()
    }

    private fun startMotionService() {
        val intent = Intent(this, MotionService::class.java).apply {
            action = MotionService.ACTION_START
        }
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopMotionService() {
        val intent = Intent(this, MotionService::class.java).apply {
            action = MotionService.ACTION_STOP
        }
        startService(intent)
    }

    private fun requestRuntimePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissions.launch(permissions.toTypedArray())
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == android.content.pm.PackageManager.PERMISSION_GRANTED ||
            coarse == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun checkGpsEnabled() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!enabled) {
            AlertDialog.Builder(this)
                .setMessage(getString(R.string.gps_enable_needed))
                .setPositiveButton(getString(R.string.open_settings)) { _, _ ->
                    startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
    }

    private fun parseIntOrDefault(value: String, def: Int): Int {
        return value.trim().toIntOrNull() ?: def
    }

    private fun maybeLockApp() {
        if (authenticated) return
        val biometricEnabled = settings.isBiometricEnabled()
        if (!biometricEnabled) {
            authenticated = true
            lockOverlay.visibility = View.GONE
            return
        }

        lockOverlay.visibility = View.VISIBLE
        if (canUseBiometrics()) {
            showBiometricPrompt()
        } else {
            Toast.makeText(this, getString(R.string.auth_required), Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun canUseBiometrics(): Boolean {
        val bm = BiometricManager.from(this)
        return bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    authenticated = true
                    lockOverlay.visibility = View.GONE
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(this@MainActivity, getString(R.string.auth_required), Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    finish()
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.auth_required))
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(info)
    }
}
