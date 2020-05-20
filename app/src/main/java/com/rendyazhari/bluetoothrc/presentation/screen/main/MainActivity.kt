package com.rendyazhari.bluetoothrc.presentation.screen.main

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rendyazhari.bluetoothrc.R
import com.rendyazhari.bluetoothrc.databinding.AppActivityMainBinding
import com.rendyazhari.bluetoothrc.presentation.enum.DeviceStatus
import com.rendyazhari.bluetoothrc.presentation.screen.bluetoothlist.BluetoothListActivity
import com.harrysoft.androidbluetoothserial.BluetoothManager
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlin.math.absoluteValue


class MainActivity : AppCompatActivity(), SensorEventListener {

    private val binding by lazy {
        AppActivityMainBinding.inflate(layoutInflater)
    }

    private val tag by lazy { localClassName }

    private val bluetoothManager by lazy { BluetoothManager.getInstance() }

    private val disposables by lazy { CompositeDisposable() }

    private var deviceStatus: DeviceStatus = DeviceStatus.Disconnected

    private var connectedDevice: Pair<BluetoothDevice, BluetoothSerialDevice>? = null

    private var deviceInterface: SimpleBluetoothDeviceInterface? = null

    private val sensorManager by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    private var sensor: Sensor? = null

    private val disconnectHandler by lazy { Handler() }

    private val disconnectRunnable by lazy {
        Runnable {
            disconnectBluetooth()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initUi()
        initListener()
        initSensor()
    }

    private fun initUi() {
        binding.appTextviewMainStatus.text = getString(
            R.string.app_label_main_bluetoothstatus,
            getString(R.string.app_text_main_bluetoothdisconnected)
        )

        binding.appButtonMainConnect.text = getString(R.string.app_action_main_bluetoothconnect)
    }

    private fun initListener() {
        binding.appButtonMainConnect.setOnClickListener {
            when (deviceStatus) {
                is DeviceStatus.Disconnected -> showListDevice()
                is DeviceStatus.Connected -> disconnectDevice()
            }
        }
    }

    private fun initSensor() {
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            showMessage(getString(R.string.app_error_general_rotationsensornotfound))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        if (deviceStatus == DeviceStatus.Connected) {
            registerSensorListener()
        }
    }

    override fun onPause() {
        if (deviceStatus == DeviceStatus.Connected) {
            unregisterSensorListener()
        }
        super.onPause()
    }

    private fun registerSensorListener() {
        sensor?.let {
            sensorManager.registerListener(
                this,
                sensor,
                100_000,
                100_000
            )
        }
    }

    private fun unregisterSensorListener() {
        sensorManager.unregisterListener(this)
    }

    private fun showListDevice() {
        if (bluetoothManager == null) {
            showMessage(getString(R.string.app_error_main_bluetoothunavailable))
        } else {
            navigateToBluetoothList()
        }
    }

    private fun connectDevice(device: BluetoothDevice) {
        val mac = device.address
        val disposable = bluetoothManager.openSerialDevice(mac)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError {
                it.printStackTrace()
            }
            .subscribe({
                onConnected(device, it)
            }, this::onError)
        disposables.add(disposable)
    }

    private fun disconnectDevice() {
        binding.appButtonMainConnect.isEnabled = false
        unregisterSensorListener()

        sendMessage("0$0$\n", false)

        disconnectHandler.postAtTime(disconnectRunnable, DISCONNECT_DELAY)
    }

    private fun setView(isEnable: Boolean) {
        with(binding) {
            if (isEnable) {
                appTextviewMainStatus.text = getString(R.string.app_text_main_bluetoothconnected)
                appButtonMainConnect.text = getString(R.string.app_action_main_bluetoothdisconnect)

                appTextviewMainSensorvalue.visibility = View.VISIBLE
                appTextviewMainPwmvalue.visibility = View.VISIBLE
                appTextviewMainComvalue.visibility = View.VISIBLE
            } else {
                appTextviewMainStatus.text = getString(R.string.app_text_main_bluetoothdisconnected)
                appButtonMainConnect.text = getString(R.string.app_action_main_bluetoothconnect)

                appTextviewMainSensorvalue.visibility = View.INVISIBLE
                appTextviewMainPwmvalue.visibility = View.INVISIBLE
                appTextviewMainComvalue.visibility = View.INVISIBLE
            }
            appButtonMainConnect.isEnabled = true
        }
    }

    private fun disconnectBluetooth() {
        connectedDevice?.second?.mac?.let { bluetoothManager.closeDevice(it) }
        bluetoothManager.close()
        deviceStatus = DeviceStatus.Disconnected

        setView(false)
    }

    private fun onConnected(device: BluetoothDevice, connectedDevice: BluetoothSerialDevice) {
        this.connectedDevice = device to connectedDevice

        deviceInterface = connectedDevice.toSimpleDeviceInterface().apply {
            setListeners(
                { onMessageReceived(it) },
                { onMessageSent(it) },
                { onError(it) }
            )
            sendMessage("255$255$")
        }

        deviceStatus = DeviceStatus.Connected
        setView(true)
        registerSensorListener()

        showMessage(getString(R.string.app_message_main_devicevonnected, device.name))
    }

    private fun onMessageSent(message: String) {
        Log.i(tag, "Sent: $message")
    }

    private fun onMessageReceived(message: String) {
        Log.i(tag, "Received: $message")
    }

    private fun onError(throwable: Throwable) {
        with(throwable) {
            printStackTrace()
            message?.let { showMessage(it) }
        }
    }

    private fun navigateToBluetoothList() {
        startActivityForResult(
            Intent(this, BluetoothListActivity::class.java),
            REQUEST_CODE_BLUETOOTH_LIST
        )
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_BLUETOOTH_LIST && resultCode == Activity.RESULT_OK) {
            if (data?.hasExtra(BluetoothListActivity.ExtraKey.DEVICE) == true) {
                val device =
                    data.getParcelableExtra<BluetoothDevice>(BluetoothListActivity.ExtraKey.DEVICE)
                device?.let { connectDevice(it) }
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        Log.i(tag, "Accuracy Changed: ${sensor.toString()} -> $accuracy")
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        val event = sensorEvent ?: return
        var axisX: Float = event.values[0]
        var axisY: Float = event.values[1]
        //val z: Float = event.values[2]

        axisX = getSensorValue(axisX)
        axisY = getSensorValue(axisY)

        binding.appTextviewMainSensorvalue.text =
            getString(R.string.app_text_main_sensorvalues, axisX, axisY)

        var pwmSpeed = getPwmSpeed(axisX)
        var speedLeft: Int
        var speedRight: Int

        if (axisX != 0f) {
            val motorSpeed = getMotorSpeed(axisY, pwmSpeed.absoluteValue)
            if (axisX > 0) {
                speedLeft = -motorSpeed.first
                speedRight = -motorSpeed.second
            } else {
                speedLeft = motorSpeed.first
                speedRight = motorSpeed.second
            }
        } else {
            pwmSpeed = getPwmSpeed(axisY)
            speedLeft = 0
            speedRight = 0

            if (pwmSpeed != 0) {
                speedLeft = pwmSpeed
                speedRight = -pwmSpeed
            }
        }

        val content = "$speedLeft$$speedRight$\n"
        binding.appTextviewMainPwmvalue.text =
            getString(R.string.app_text_main_pwmvalues, speedLeft, speedRight)
        binding.appTextviewMainComvalue.text = content

        sendMessage(content)
    }


    private fun sendMessage(content: String, handlingDisconnect: Boolean = true) {
        try {
            deviceInterface?.sendMessage(content)
        } catch (iae: IllegalArgumentException) {
            iae.printStackTrace()
            if (handlingDisconnect) disconnectDevice()
        }
    }

    private fun getPwmSpeed(value: Float) = ((value / MAX) * MAX_PWM).toInt()

    private fun getMotorSpeed(y: Float, pwmSpeed: Int): Pair<Int, Int> {
        val speedLeft: Int
        val speedRight: Int

        if (y < 0) { //Kiri
            speedRight = pwmSpeed
            speedLeft = (((MIN - y) / MIN) * pwmSpeed).toInt()
        } else {
            speedLeft = pwmSpeed
            speedRight = (((MAX - y) / MAX) * pwmSpeed).toInt()
        }

        return speedLeft to speedRight
    }

    private fun getSensorValue(value: Float) =
        when {
            value < MIN -> MIN
            value > MAX -> MAX
            value.absoluteValue < MID -> 0f
            else -> value - MID
        }

    override fun onDestroy() {
        disconnectBluetooth()
        disposables.dispose()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_CODE_BLUETOOTH_LIST = 101

        private const val MAX = 5.5f
        private const val MIN = -5.5f
        private const val MID = 0.7f
        private const val MAX_PWM = 255

        private const val DISCONNECT_DELAY = 500L
    }
}