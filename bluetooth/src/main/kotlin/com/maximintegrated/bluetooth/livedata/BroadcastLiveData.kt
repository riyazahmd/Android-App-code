package com.maximintegrated.bluetooth.livedata

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData

abstract class BroadcastLiveData<T>(context: Context) : LiveData<T>() {

    internal abstract val intentFilter: IntentFilter
    internal abstract val broadcastReceiver: BroadcastReceiver

    internal val context: Context = context.applicationContext

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActive() {
        super.onActive()
        context.registerReceiver(broadcastReceiver, intentFilter, RECEIVER_EXPORTED)
    }

    override fun onInactive() {
        super.onInactive()
        context.unregisterReceiver(broadcastReceiver)
    }

}