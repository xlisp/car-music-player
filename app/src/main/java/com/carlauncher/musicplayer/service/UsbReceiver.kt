package com.carlauncher.musicplayer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 监听USB设备挂载/卸载事件
 */
class UsbReceiver : BroadcastReceiver() {

    var onUsbMounted: (() -> Unit)? = null
    var onUsbUnmounted: (() -> Unit)? = null

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_MEDIA_MOUNTED -> {
                onUsbMounted?.invoke()
            }
            Intent.ACTION_MEDIA_UNMOUNTED,
            Intent.ACTION_MEDIA_REMOVED -> {
                onUsbUnmounted?.invoke()
            }
        }
    }
}
