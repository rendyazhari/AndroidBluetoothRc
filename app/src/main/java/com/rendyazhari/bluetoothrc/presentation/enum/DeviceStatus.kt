package com.rendyazhari.bluetoothrc.presentation.enum

sealed class DeviceStatus {
    object Disconnected : DeviceStatus()
    object Connected : DeviceStatus()
}