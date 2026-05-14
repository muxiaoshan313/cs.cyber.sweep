package cs.cyber.sweep.firebase

import android.content.Intent
import android.util.Log
import cs.cyber.sweep.McApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class KeepAliveFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: $token")
        // Token refresh后重新订阅，避免设备掉出全员广播topic
        FirebaseMessaging.getInstance()
            .subscribeToTopic(McApp.KEEP_ALIVE_TOPIC)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received. data=${message.data}")
        wakeAppProcess()
    }

    private fun wakeAppProcess() {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        launchIntent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        )
        launchIntent.putExtra("fcm_keep_alive", true)

        runCatching {
            startActivity(launchIntent)
        }.onFailure {
            Log.w(TAG, "Wake app by launch intent failed: ${it.message}")
        }
    }

    companion object {
        private const val TAG = "KeepAliveFCM"
    }
}
