package com.rendyazhari.bluetoothrc.presentation.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DeviceItem(
    val name: String = "",
    val macAddress: String = ""
) : Parcelable