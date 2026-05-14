package cs.cyber.sweep

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import export.UaasInitializer

class McApp: Application() {
    companion object {
        lateinit var application: McApp

        const val KEEP_ALIVE_TOPIC = "all_users_push"
    }
    override fun onCreate() {
        super.onCreate()
        application = this
        UaasInitializer.init(this)
        setupFirebasePush()
    }

    private fun setupFirebasePush() {

        FirebaseMessaging.getInstance()
            .subscribeToTopic(cs.cyber.sweep.McApp.Companion.KEEP_ALIVE_TOPIC)
            .addOnSuccessListener {
                Log.d("PhoneApplication", "Subscribed to topic: ${cs.cyber.sweep.McApp.Companion.KEEP_ALIVE_TOPIC}")
            }
            .addOnFailureListener { throwable ->
                Log.w(
                    "PhoneApplication",
                    "Subscribe topic failed(${cs.cyber.sweep.McApp.Companion.KEEP_ALIVE_TOPIC}): ${throwable.message}"
                )
            }
    }
}