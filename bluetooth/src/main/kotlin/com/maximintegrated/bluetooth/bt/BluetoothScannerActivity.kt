package com.maximintegrated.bluetooth.bt

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.maximintegrated.bluetooth.R
import com.maximintegrated.bluetooth.common.BluetoothScannerDelegate
import com.maximintegrated.bluetooth.devicelist.OnBluetoothDeviceClickListener
import com.maximintegrated.bluetooth.extension.hasPermission
import com.maximintegrated.bluetooth.extension.hasPermissions
import com.maximintegrated.bluetooth.util.cancelBluetoothDeviceDiscovery
import com.maximintegrated.bluetooth.util.startBluetoothDeviceDiscovery
import com.maximintegrated.bluetooth.util.verifyPermissions

open class BluetoothScannerActivity : AppCompatActivity(), OnBluetoothDeviceClickListener {

    val devicesRecyclerView: RecyclerView by lazy { findViewById<RecyclerView>(R.id.devicesRecyclerView) }
    val scannerWarningCardView: MaterialCardView by lazy { findViewById<MaterialCardView>(R.id.scannerWarningCardView) }

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
        const val REQUEST_STORAGE_PERMISSION = 2
        const val REQUEST_BLUETOOTH_CONNECT_PERMISSION = 3

        fun start(context: Context) {
            val intent = Intent(context, BluetoothScannerActivity::class.java)
            context.startActivity(intent)
        }
    }

    private lateinit var scannerDelegate: BluetoothScannerDelegate


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_scanner)
        setTitle(R.string.bluetooth_title)

        scannerDelegate = BluetoothScannerDelegate(
            this,
            ViewModelProviders.of(this).get(BluetoothScannerViewModel::class.java)
        ).apply {
            deviceClickListener = this@BluetoothScannerActivity
            scanStateChangeHandler = this@BluetoothScannerActivity::invalidateOptionsMenu
        }

        with(devicesRecyclerView) {
            setHasFixedSize(true)
            adapter = scannerDelegate.deviceListAdapter
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onResume() {
        super.onResume()
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            scannerDelegate.showLocationPermissionMessage(View.OnClickListener {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    ), REQUEST_LOCATION_PERMISSION
                )
            })
        } else if (!hasPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            scannerDelegate.showStoragePermissionMessage(View.OnClickListener {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_STORAGE_PERMISSION
                )
            })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && verifyPermissions(grantResults)) {
            // permission was granted
            scannerDelegate.hideLocationPermissionMessage()
            startBluetoothDeviceDiscovery()
        }
        if (requestCode == REQUEST_STORAGE_PERMISSION && verifyPermissions(grantResults)) {
            // permission was granted
            scannerDelegate.hideStoragePermissionMessage()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_bluetooth_scanner, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_scan_start).isVisible = scannerDelegate.isScanStartVisible
        menu.findItem(R.id.action_scan_stop).isVisible = scannerDelegate.isScanStopVisible
        if (!scannerDelegate.showingAnyErrorModel) {
            scannerWarningCardView.visibility = View.VISIBLE
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.action_scan_start -> {
            startBluetoothDeviceDiscovery()
            true
        }

        R.id.action_scan_stop -> {
            cancelBluetoothDeviceDiscovery()
            true
        }

        else -> super.onOptionsItemSelected(item)
    }

    override fun onBluetoothDeviceClicked(bluetoothDevice: BluetoothDevice) {
        // will be overridden
    }
}
