package com.czaplicki.eproba

import android.util.Log
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class EprobaFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "EprobaFirebaseMessagingService"

    @OptIn(DelicateCoroutinesApi::class)
    override fun onNewToken(token: String) {
        GlobalScope.launch {
            if (EprobaApplication.instance.apiHelper.user != null) {
                EprobaApplication.instance.apiHelper.registerFCMToken(token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
        Log.d(TAG, "From: ${remoteMessage.from}")

        EprobaApplication.instance.currentActivity?.runOnUiThread {
            Toast.makeText(
                EprobaApplication.instance.currentActivity,
                remoteMessage.notification?.body ?: "Received a new FCM message",
                Toast.LENGTH_SHORT
            ).show()
        }


        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
    }
}