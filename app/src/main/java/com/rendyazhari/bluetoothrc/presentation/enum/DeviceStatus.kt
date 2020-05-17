package com.rendyazhari.bluetoothrc.presentation.enum

sealed class DeviceStatus {
    object Disconnected : DeviceStatus()
    object Connected : DeviceStatus()
    object Connecting : DeviceStatus()
    object Disconnecting : DeviceStatus()
}