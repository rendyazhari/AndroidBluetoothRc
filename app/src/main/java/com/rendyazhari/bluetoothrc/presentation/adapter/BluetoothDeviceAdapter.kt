package com.rendyazhari.bluetoothrc.presentation.adapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rendyazhari.bluetoothrc.databinding.AppItemBlutoothdeviceBinding

class BluetoothDeviceAdapter(
    private val clickListener: (device: BluetoothDevice) -> Unit
) : RecyclerView.Adapter<BluetoothDeviceAdapter.DeviceVH>() {

    private val dataSet: MutableList<BluetoothDevice> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceVH =
        DeviceVH(
            AppItemBlutoothdeviceBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            clickListener
        )

    override fun getItemCount(): Int = dataSet.size

    override fun onBindViewHolder(holder: DeviceVH, position: Int) {
        if (dataSet.isEmpty()) return
        holder.bind(dataSet[position])
    }

    fun clearItem() {
        val size = itemCount
        dataSet.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun setItems(items: List<BluetoothDevice>) {
        with(dataSet) {
            clear()
            addAll(items)
        }
        notifyDataSetChanged()
    }

    fun addItem(item: BluetoothDevice) {
        dataSet.add(item)
        notifyItemInserted(itemCount.dec())
    }

    inner class DeviceVH(
        private val itemBinding: AppItemBlutoothdeviceBinding,
        private val clickListener: (device: BluetoothDevice) -> Unit
    ) : RecyclerView.ViewHolder(itemBinding.root) {

        fun bind(item: BluetoothDevice) {
            with(itemBinding) {
                appTextBluetoothdeviceName.text = item.name
                appTextBluetoothdeviceId.text = item.address
                root.rootView.setOnClickListener { clickListener.invoke(item) }
            }
        }
    }

}