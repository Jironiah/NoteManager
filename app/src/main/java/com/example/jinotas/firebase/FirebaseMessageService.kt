package com.example.jinotas.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.jinotas.AdapterNotes
import com.example.jinotas.MainActivity
import com.example.jinotas.NotesFragment
import com.example.jinotas.R
import com.example.jinotas.api.CrudApi
import com.example.jinotas.api.tokenusernocodb.ApiTokenUser
import com.example.jinotas.db.AppDatabase
import com.example.jinotas.db.Note
import com.example.jinotas.db.Token
import com.example.jinotas.utils.Utils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext


class FirebaseMessageService : FirebaseMessagingService(), CoroutineScope {
    private lateinit var db: AppDatabase
    private lateinit var adapterNotes: AdapterNotes
    private lateinit var fragment: NotesFragment
    private lateinit var newNotes: ArrayList<Note>
    private val activity = MainActivity.instance

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        db = AppDatabase.getDatabase(this@FirebaseMessageService)
        // Verificar si el mensaje contiene datos personalizados
        if (remoteMessage.data.isNotEmpty()) {
            val receivedDeviceId = remoteMessage.data["deviceId"]
            val currentDeviceId = Utils.getIdDevice(context = this@FirebaseMessageService)

            if (receivedDeviceId != currentDeviceId) {
                // Crear manualmente el objeto Note a partir de los datos recibidos
                val note = Note(
                    code = remoteMessage.data["code"]?.toInt() ?: 0,
                    id = remoteMessage.data["id"]?.toIntOrNull(),
                    title = remoteMessage.data["title"] ?: "Sin título",
                    textContent = remoteMessage.data["textContent"] ?: "",
                    date = remoteMessage.data["date"] ?: "",
                    userFrom = remoteMessage.data["userFrom"] ?: "Desconocido",
                    userTo = remoteMessage.data["userTo"],
                    createdAt = remoteMessage.data["createdAt"],
                    updatedAt = remoteMessage.data["updatedAt"]
                )

                sendNotification(note.title, "${note.userFrom} te ha enviado una nueva nota")
                handleReceivedNote(note)
            }
        }
    }

    private fun handleReceivedNote(note: Note) {
        // Aquí puedes manejar lo que quieres hacer con la nota
        // Por ejemplo, actualizar la UI si la app está en primer plano, guardar la nota, etc.
        Log.i("FirebaseMessageService", "Nota recibida: $note")

        // Inserta la nota en la base de datos en un hilo de IO
        CoroutineScope(Dispatchers.IO).launch {
            db.noteDAO().insertNote(note)
            newNotes = db.noteDAO().getNotesList() as ArrayList<Note>

            // Actualizar la UI en el hilo principal
            withContext(Dispatchers.Main) {
                if (activity != null) {
                    val fragment =
                        activity.supportFragmentManager.findFragmentById(R.id.fragment_container_view) as? NotesFragment
                    fragment?.let {
                        // Llamar a loadNotes si existe
                        it.loadNotes()

                        // Actualizar el Adapter en el hilo principal
                        adapterNotes = AdapterNotes(newNotes, coroutineContext)
                        adapterNotes.updateList(newNotes)
                    }
                }

                // Mostrar el Toast en el hilo principal
                Toast.makeText(
                    this@FirebaseMessageService,
                    "Has recibido una nota de ${note.userFrom}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val db: AppDatabase = AppDatabase.getDatabase(this@FirebaseMessageService)
        CoroutineScope(Dispatchers.IO).launch {
            db.tokenDAO().updateToken(token = Token(token = token))
            val sharedPreferences: SharedPreferences =
                this@FirebaseMessageService.getSharedPreferences(
                    "MyPrefsFile", Context.MODE_PRIVATE
                )
            val userNameFrom = sharedPreferences.getString("userFrom", null) ?: ""
            if (userNameFrom.isNotEmpty()) {
                val actualToken = ApiTokenUser(userName = userNameFrom!!, token = token)
                CrudApi().patchUserToken(actualToken)
            }
        }
        Log.i("OnNewToken", "New Token -> $token")
    }

    private fun sendNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "Default"
        val notificationBuilder =
            NotificationCompat.Builder(this, channelId).setContentTitle(title).setContentText(body)
                .setSmallIcon(R.drawable.ic_notification).setAutoCancel(true)
                .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

//    private fun sendNotificationNewNote(title: String, body: String) {
//        Log.e("title", title)
//        Log.e("body", body)
//        val intent = Intent(this, MainActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        val pendingIntent = PendingIntent.getActivity(
//            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val channelId = "Default"
//        val notificationBuilder =
//            NotificationCompat.Builder(this, channelId).setContentTitle(title).setContentText(body)
//                .setSmallIcon(R.drawable.ic_notification).setAutoCancel(true)
//                .setContentIntent(pendingIntent)
//
//        val notificationManager =
//            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
//            val channel = NotificationChannel(
//                channelId, "Channel human readable title", NotificationManager.IMPORTANCE_DEFAULT
//            )
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        notificationManager.notify(0, notificationBuilder.build())
//    }
}