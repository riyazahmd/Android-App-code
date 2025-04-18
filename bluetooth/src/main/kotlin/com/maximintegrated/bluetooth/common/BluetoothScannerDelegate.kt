package com.maximintegrated.bluetooth.common

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import com.maximintegrated.bluetooth.R
import com.maximintegrated.bluetooth.ble.BleScannerActivity.Companion.REQUEST_LOCATION_PERMISSION
import com.maximintegrated.bluetooth.devicelist.BluetoothDeviceListController
import com.maximintegrated.bluetooth.livedata.AdapterState
import com.maximintegrated.bluetooth.livedata.DiscoveryState
import com.maximintegrated.bluetooth.util.enableBluetooth
import com.maximintegrated.bluetooth.view.ErrorModel

class BluetoothScannerDelegate(
    lifecycleOwner: LifecycleOwner,
    viewModel: BaseBluetoothViewModel,
    private val deviceNamePrefixList: ArrayList<String>? = null
) {
    private val applicationContext = lifecycleOwner as Activity
    private val deviceListController = BluetoothDeviceListController()

    private val bluetoothOffErrorModel = ErrorModel(
        R.drawable.ic_bluetooth_off,
        R.string.bluetooth_off_error_message,
        R.string.bluetooth_off_error_action
    )

    private val locationPermissionErrorModel = ErrorModel(
        R.drawable.ic_location,
        R.string.bluetooth_location_permission_message,
        R.string.bluetooth_location_permission_action
    )

    private val storagePermissionErrorModel = ErrorModel(
        R.drawable.ic_storage,
        R.string.bluetooth_storage_permission_message,
        R.string.bluetooth_storage_permission_action
    )

    private val defaultLocationErrorModel = ErrorModel(
        R.drawable.ic_storage,
        R.string.bluetooth_default_location_message,
        R.string.bluetooth_default_location_action
    )

    private var isBluetoothOn = false
        set(value) {
            field = value
            showBluetoothOffErrorIfNeeded()
            scanStateChangeHandler?.invoke()
        }

    var isDiscovering
        get() = deviceListController.isDiscovering
        private set(value) {
            deviceListController.isDiscovering = value
            scanStateChangeHandler?.invoke()
        }

    /*val isShowingBluetoothConnectionMessage
        get() = deviceListController.errorModel == bluetoothOffErrorModel
*/
    val isShowingLocationPermissionMessage
        get() = deviceListController.errorModel == locationPermissionErrorModel

    val isShowingStoragePermissionMessage
        get() = deviceListController.errorModel == storagePermissionErrorModel

    val isScanStartVisible
        get() = !isShowingLocationPermissionMessage && !isShowingStoragePermissionMessage /*&& !isShowingBluetoothConnectionMessage*/ && isBluetoothOn && !isDiscovering

    val isScanStopVisible
        get() = !isShowingLocationPermissionMessage && !isShowingStoragePermissionMessage /*&& !isShowingBluetoothConnectionMessage*/ && isBluetoothOn && isDiscovering

    var deviceClickListener
        get() = deviceListController.deviceClickListener
        set(value) {
            deviceListController.deviceClickListener = value
        }

    val deviceListAdapter: RecyclerView.Adapter<*> get() = deviceListController.adapter

    var scanStateChangeHandler: (() -> Unit)? = null

    val showingAnyErrorModel
        get() = deviceListController.errorModel != null

    init {
        viewModel.adapterState
            .observe(lifecycleOwner, Observer {
                isBluetoothOn = (it == AdapterState.ON)
            })

        viewModel.discoveryState
            .observe(lifecycleOwner, Observer {
                isDiscovering = (it == DiscoveryState.DISCOVERING)
            })

        viewModel.pairedDevices
            .observe(lifecycleOwner, Observer {
                deviceListController.pairedDevices = if (deviceNamePrefixList == null) {
                    it
                } else {
                    it.filter { device ->
                        val deviceName = device.name ?: ""
                       deviceNameCheck(deviceName)
                    }
                }
            })

        viewModel.availableDevices
            .observe(lifecycleOwner, Observer {
                deviceListController.availableDevices = if (deviceNamePrefixList == null) {
                    it
                } else {
                    it.filter { device ->
                        val deviceName = device.name ?: ""
                        deviceNameCheck(deviceName)
                    }
                }
            })
    }

    private fun deviceNameCheck(deviceName: String): Boolean{
        var found = false
        for(name in deviceNamePrefixList!!){
            if(deviceName.startsWith(name, true)){
                found = true
                break
            }
        }
        return found;
    }

    fun showLocationPermissionMessage(actionClickListener: View.OnClickListener) {
        deviceListController.errorButtonClickListener = actionClickListener
        deviceListController.errorModel = locationPermissionErrorModel
        scanStateChangeHandler?.invoke()
    }

    fun hideLocationPermissionMessage() {
        deviceListController.errorButtonClickListener = null
        deviceListController.errorModel = null
        scanStateChangeHandler?.invoke()
    }

    fun showStoragePermissionMessage(actionClickListener: View.OnClickListener) {
        deviceListController.errorButtonClickListener = actionClickListener
        deviceListController.errorModel = storagePermissionErrorModel
        scanStateChangeHandler?.invoke()
    }

    fun hideStoragePermissionMessage() {
        deviceListController.errorButtonClickListener = null
        deviceListController.errorModel = null
        scanStateChangeHandler?.invoke()
        showBluetoothOffErrorIfNeeded()
    }

    fun showSetDefaultLocationMessage(actionClickListener: View.OnClickListener) {
        deviceListController.errorButtonClickListener = actionClickListener
        deviceListController.errorModel = defaultLocationErrorModel
        scanStateChangeHandler?.invoke()
    }

    fun hideSetDefaultLocationMessage() {
        deviceListController.errorButtonClickListener = null
        deviceListController.errorModel = null
        scanStateChangeHandler?.invoke()
    }

    private fun showBluetoothOffErrorIfNeeded() {
        // ignore if location permission message is shown
        if (isShowingStoragePermissionMessage) return

        if (isBluetoothOn) {
            deviceListController.errorButtonClickListener = null
            deviceListController.errorModel = null
        } else {
            ActivityCompat.requestPermissions(
                applicationContext,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_PRIVILEGED,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADMIN
                ), REQUEST_LOCATION_PERMISSION
            )
            deviceListController.errorButtonClickListener =
                View.OnClickListener { enableBluetooth()
                    Toast.makeText(applicationContext, R.string.bluetooth_off_error_action, Toast.LENGTH_SHORT).show()
                }
            deviceListController.errorModel = bluetoothOffErrorModel
        }
    }
}