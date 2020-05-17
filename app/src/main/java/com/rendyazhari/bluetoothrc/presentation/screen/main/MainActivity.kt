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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.rendyazhari.bluetoothrc.R
import com.rendyazhari.bluetoothrc.databinding.AppActivityMainBinding
import com.rendyazhari.bluetoothrc.presentation.enum.DeviceStatus
import com.rendyazhari.bluetoothrc.presentation.screen.bluetoothlist.BluetoothListActivity
import com.harrysoft.androidbluetoothserial.BluetoothManager
import com.harrysoft.androidbluetoothserial.BluetoothSerialDevice
import com.harrysoft.androidbluetoothserial.SimpleBluetoothDeviceInterface
import com.orhanobut.logger.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlin.math.absoluteValue


class MainActivity : AppCompatActivity(), SensorEventListener {

    private val binding by lazy {
        AppActivityMainBinding.inflate(layoutInflater)
    }

    private val bluetoothManager by lazy { BluetoothManager.getInstance() }

    private val disposables by lazy { CompositeDisposable() }

    private var deviceStatus: DeviceStatus = DeviceStatus.Disconnected

    private var connectedDevice: Pair<BluetoothDevice, BluetoothSerialDevice>? = null

    private var deviceInterface: SimpleBluetoothDeviceInterface? = null

    private val sensorManager by lazy { getSystemService(Context.SENSOR_SERVICE) as SensorManager }

    private var sensor: Sensor? = null

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
        connectedDevice?.second?.mac?.let { bluetoothManager.closeDevice(it) }
        bluetoothManager.close()

        deviceStatus = DeviceStatus.Disconnected
        binding.appTextviewMainStatus.text = getString(R.string.app_text_main_bluetoothdisconnected)
        binding.appButtonMainConnect.text = getString(R.string.app_action_main_bluetoothconnect)
    }

    private fun onConnected(device: BluetoothDevice, connectedDevice: BluetoothSerialDevice) {
        this.connectedDevice = device to connectedDevice

        deviceInterface = connectedDevice.toSimpleDeviceInterface().apply {
            setListeners(
                { onMessageReceived(it) },
                { onMessageSent(it) },
                { onError(it) }
            )
//            sendMessage("Alhamdulillah")
            sendMessage("255$255$")
        }

        deviceStatus = DeviceStatus.Connected
        binding.appTextviewMainStatus.text = getString(R.string.app_text_main_bluetoothconnected)
        binding.appButtonMainConnect.text = getString(R.string.app_action_main_bluetoothdisconnect)
        registerSensorListener()

        showMessage(getString(R.string.app_message_main_devicevonnected, device.name))
    }

    private fun onMessageSent(message: String) {
        Logger.i("Sent: $message")
    }

    private fun onMessageReceived(message: String) {
        Logger.i("Received: $message")
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
        Logger.i("Accuracy Changed: ${sensor.toString()} -> $accuracy")
    }

    override fun onSensorChanged(sensorEvent: SensorEvent?) {
        val event = sensorEvent ?: return
        var x: Float = event.values[0]
        var y: Float = event.values[1]
        val z: Float = event.values[2]

        // Range = -4 - 4

        x = getSensorValue(x)
        y = getSensorValue(y)

        binding.appTextviewMainSensorvalue.text = getString(R.string.app_text_main_sensorvalues, x, y)

        var pwmSpeed = getPwmSpeed(x)
        var speedLeft = 0
        var speedRight = 0

        if (x > 0) { // Mundur
            x = x.absoluteValue
            pwmSpeed = pwmSpeed.absoluteValue
            if (y < 0) { //Kiri
                speedRight = pwmSpeed
                speedLeft = (((MIN - y) / MIN) * pwmSpeed).toInt()
            } else {
                speedLeft = pwmSpeed
                speedRight = (((MAX - y) / MAX) * pwmSpeed).toInt()
            }
//            speedLeft = 255 - speedLeft
//            speedRight = 255 - speedRight

            speedLeft *= -1
            speedRight *= -1
        } else if (x < 0) {
            x = x.absoluteValue
            pwmSpeed = pwmSpeed.absoluteValue
            if (y < 0) { //Kiri
                speedRight = pwmSpeed
                speedLeft = (((MIN - y) / MIN) * pwmSpeed).toInt()
            } else {
                speedLeft = pwmSpeed
                speedRight = (((MAX - y) / MAX) * pwmSpeed).toInt()
            }
//            speedLeft = 255 - speedLeft
//            speedRight = 255 - speedRight
        } else {
            pwmSpeed = getPwmSpeed(y)
            if (pwmSpeed == 0) {
                speedLeft = 0
                speedRight = 0
            } else {
                if (y < 0) {
                    speedRight = 0
                    speedLeft = pwmSpeed
                } else {
                    speedLeft = 0
                    speedRight = pwmSpeed
                }
            }
//            speedLeft = 255 - speedLeft
//            speedRight = 255 - speedRight
        }

        val content = "$speedLeft$$speedRight$\n"
        binding.appTextviewMainPwmvalue.text = getString(R.string.app_text_main_pwmvalues, speedLeft, speedRight)
        binding.appTextviewMainComvalue.text = content
        deviceInterface?.sendMessage(content)
    }

    private fun getPwmSpeed(value: Float): Int {
        return ((value / MAX) * MAX_PWM).toInt()
    }

    private fun getSensorValue(value: Float) =
        when {
            value < MIN -> {
                MIN
            }
            value > MAX -> {
                MAX
            }
            value.absoluteValue < MID -> 0f
            else -> {
                value
            }
        }

    override fun onDestroy() {
        disconnectDevice()
        disposables.dispose()
        super.onDestroy()
    }

    companion object {
        private const val REQUEST_CODE_BLUETOOTH_LIST = 101

        private const val MAX = 4.5f
        private const val MIN = -4.5f
        private const val MID = 0.5f
        private const val MAX_PWM = 255
    }
}