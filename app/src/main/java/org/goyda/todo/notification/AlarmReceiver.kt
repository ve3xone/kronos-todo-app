package org.goyda.todo.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import org.goyda.todo.R
import org.goyda.todo.database.ToDoListDatabase
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AlarmReceiver : BroadcastReceiver() {
    private val GROUP_MESSAGE: String = "TODOLIST"
    private var toDoListDatabase: ToDoListDatabase? = null

    @SuppressLint("WrongConstant")
    override fun onReceive(context: Context?, intent: Intent?) {
        val dbId = intent?.getLongExtra("id", -1) ?: -1
        val pass = intent?.getStringExtra("pass") ?: ""

        context?.let { initiateDatabase(it, pass) }

        val notificationManager: NotificationManager =
            context?.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
        val isShow = intent?.getIntExtra("isShow", 0) ?: 0
        val title = intent?.getStringExtra("title") ?: ""
        val desc = intent?.getStringExtra("desc") ?: ""
        val time = intent?.getStringExtra("date") ?: ""
        Log.d("Alarm Title", "title : $title")

        val icon = R.drawable.ic_notify

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel("0")
            if (existingChannel == null) {
                val notificationChannel =
                    NotificationChannel(
                        "0",
                        "$GROUP_MESSAGE Notifications",
                        NotificationManager.IMPORTANCE_MAX
                    )
                notificationChannel.description = GROUP_MESSAGE
                notificationChannel.enableVibration(false)
                notificationManager.createNotificationChannel(notificationChannel)
            }
        }

        val completeIntent = Intent(context, CompleteActionReceiver::class.java).apply {
            putExtra("id", dbId)
            putExtra("pass", pass)
            putExtra("title", title)
            putExtra("desc", desc)
            putExtra("date", time)
        }
        val completePendingIntent = PendingIntent.getBroadcast(context, dbId.toInt(), completeIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, "0")
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(time)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$time\n\n$desc"))
            .setPriority(NotificationCompat.VISIBILITY_PUBLIC)
            .setGroup(GROUP_MESSAGE)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addAction(0, context.getString(R.string.done), completePendingIntent)
            .build()

        toDoListDatabase?.toDoListDao()?.get(dbId)?.let { task ->
            if (!task.comp && task.title == title && task.desc == desc && "${task.date} ${task.time}" == time) {
                notificationManager.notify(dbId.toInt(), notification)
                toDoListDatabase!!.toDoListDao().isShownUpdate(id = dbId, isShow = 1)
                Log.d("IsRead", "isRead 1")
            }
        }
    }

    private fun initiateDatabase(context: Context, pass: String) {
        if (toDoListDatabase == null)
            toDoListDatabase = ToDoListDatabase.getInstance(context, pass)
    }

    class CompleteActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val dbId = intent.getLongExtra("id", -1)
            val pass = intent.getStringExtra("pass") ?: ""
            val title = intent.getStringExtra("title") ?: ""
            val desc = intent.getStringExtra("desc") ?: ""
            val time = intent.getStringExtra("date") ?: ""

            val toDoListDatabase = ToDoListDatabase.getInstance(context, pass)
            toDoListDatabase?.toDoListDao()?.get(dbId)?.let { task ->
                if (!task.comp && task.title == title && task.desc == desc && "${task.date} ${task.time}" == time) {
                    toDoListDatabase.toDoListDao().compUpdate(dbId, true)
                    Log.d("CompleteActionReceiver", "Task $dbId marked as complete")
                }
            }

            val notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(dbId.toInt())

            // Отправка широковещательного сообщения
            val broadcastIntent = Intent("org.goyda.todo.ACTION_TASK_COMPLETE")
            broadcastIntent.putExtra("id", dbId)
            context.sendBroadcast(broadcastIntent)
        }
    }
}
