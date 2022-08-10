package com.app.lockapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.io.File
import androidx.core.content.ContextCompat.startActivity
import java.time.LocalDateTime


class AlarmBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // toast で受け取りを確認
        startMainActivity(context)
    }

    private fun startMainActivity(context: Context) {
        val intent: Intent = Intent(context, MainActivity().javaClass)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(context, intent, null)
    }
}