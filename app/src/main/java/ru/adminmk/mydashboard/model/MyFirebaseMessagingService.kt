package ru.adminmk.mydashboard.model

import android.content.Intent
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import ru.adminmk.mydashboard.MainActivity

private const val TAG = "MyFirebaseMessagingService"

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // [START on_new_token]
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * InstanceID token is initially generated so this is where you would retrieve
     * the token.
     */
    override fun onNewToken(p0: String) {
        Log.d(TAG, "Refreshed token: $p0")

        sendRegistrationToServer(p0)
    }
    // [END on_new_token]

    private fun sendRegistrationToServer(token: String?) {
        //отправить данные токена текущего устройства- телефона на сервере, если будет использоваться токен для обмена с 1С
    }

    //Make sure your app is running in the foreground. If your app is in the background,
    // the FCM message will trigger an automatic notification and the onMessageReceived
    // function will receive only the remoteMessage object when the user clicks the notification

    // The maximum payload size for both message types is 4 KB
    // (except when sending messages from the Firebase console, which enforces a 1024-character limit).
    override fun onMessageReceived(p0: RemoteMessage) {
        p0.data.let {
            Log.d(TAG, "Message data payload: $it")


            val iterator = it.iterator()
            val entry = iterator.next()

            Log.d(TAG, "Key0: ${entry.key}")

            //в foreground нотификация автоматически не происходит. если надо- сделать вручную отсюда
            // Check if the message contains a notification payload.
            p0.notification?.let { notificztion ->
                Log.d(TAG, "Message Notification Body: ${notificztion.body}")

                sendDataToActivity()
            }
        }

    }

    private fun sendDataToActivity() {
        val intent = Intent()
        intent.action = MainActivity.MY_PACK
        sendBroadcast(intent)
    }
}



fun subscribeToTopicInModel(topic:String, actionOnComplete: (Task<Void>)->Unit){
    FirebaseMessaging.getInstance().
    subscribeToTopic(topic).
    addOnCompleteListener(actionOnComplete)
}