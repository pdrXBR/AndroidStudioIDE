package com.androidide.termux

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.androidide.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val CHANNEL_ID = "terminal_service"
private const val NOTIF_ID = 1001

@AndroidEntryPoint
class TerminalService : Service() {

    @Inject lateinit var termuxBridge: TermuxBridge

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    var currentSession: TerminalSession? = null
        private set

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun newSession(workDir: String? = null): TerminalSession {
        currentSession?.kill()
        return TerminalSession(termuxBridge, workDir, scope).also {
            currentSession = it
            it.start()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Terminal", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "AndroidIDE terminal session" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AndroidIDE Terminal")
            .setContentText("Terminal session active")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    override fun onDestroy() {
        currentSession?.kill()
        scope.cancel()
        super.onDestroy()
    }
}
