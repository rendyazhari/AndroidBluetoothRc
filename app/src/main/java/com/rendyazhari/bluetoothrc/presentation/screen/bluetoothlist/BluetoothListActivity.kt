package com.rendyazhari.bluetoothrc.presentation.screen.bluetoothlist

import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.rendyazhari.bluetoothrc.databinding.AppActivityBluetoothlistBinding
import com.rendyazhari.bluetoothrc.presentation.adapter.BluetoothDeviceAdapter
import com.harrysoft.androidbluetoothserial.BluetoothManager

class BluetoothListActivity : AppCompatActivity() {

    private val binding by lazy {
        AppActivityBluetoothlistBinding.inflate(layoutInflater)
    }

    private val bluetoothManager by lazy { BluetoothManager.getInstance() }

    private val deviceAdapter by lazy {
        BluetoothDeviceAdapter() {
            returnResult(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initAdapter()
        loadDevices()
    }

    private fun initAdapter() {
        binding.appRecyclerBluetoothlist.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = deviceAdapter
        }
    }

    private fun loadDevices() {
        val devices = bluetoothManager.pairedDevicesList
        deviceAdapter.setItems(devices)
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun returnResult(device: BluetoothDevice) {
        val data = Intent().apply {
            putExtra(ExtraKey.DEVICE, device)
        }
//        data.putExtras(
//            bundleOf(
//                ExtraKey.DEVICE to device
//            )
//        )
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    object ExtraKey {
        const val DEVICE = "BluetoothListActivity.DEVICE"
    }
}