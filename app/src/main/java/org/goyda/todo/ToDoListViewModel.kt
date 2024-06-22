package org.goyda.todo

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.databinding.ObservableField
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import org.goyda.todo.database.ToDoListDataEntity
import org.goyda.todo.database.ToDoListDatabase
import org.goyda.todo.notification.AlarmReceiver
import java.util.*

class ToDoListViewModel(val context: Application) : AndroidViewModel(context) {
    var toDoListData = MutableLiveData<ToDoListData>()

    private var database: ToDoListDatabase? = null

    private var getAllData = mutableListOf(ToDoListDataEntity())
    val toDoList = MutableLiveData<List<ToDoListDataEntity>>()

    //init {
        //database = ToDoListDatabase.getInstance(context)
        //database?.toDoListDao()?.getAll()?.let {
        //    getAllData = it as MutableList<ToDoListDataEntity>
        //}
    //}

    private var pass = ""
    public var isAuthenticated = false
    fun initializeDatabase(password: String) {
        pass = password
        database = ToDoListDatabase.getInstance(getApplication(), password)
        try {
            database?.toDoListDao()?.getAll()?.let {
                getAllData = it as MutableList<ToDoListDataEntity>
            }
            isAuthenticated = true
        }
        catch (_: net.sqlcipher.database.SQLiteException){
            isAuthenticated = false
        }
    }

    var title = ObservableField<String>("")
    var desc = ObservableField<String>("")
    var date = ObservableField<String>("")
    var time = ObservableField<String>("")

    var month = 0
    var day = 0
    var year = 0

    var hour = 0
    var minute = 0

    var position: Int = -1
    var index: Long = -1

    var Error: Boolean = false

    fun click(v: View) {
        Log.d("Click", "click")
        if (title.get().toString().isNotBlank() && desc.get().toString().isNotBlank() && date.get().toString().isNotBlank() && time.get().toString().isNotBlank()) {
            addData(title.get().toString(), desc.get().toString(), date.get().toString(), time.get().toString(), id = index)
            Error = false
            title.set("")
            desc.set("")
            date.set("")
            time.set("")
        }else{
            Error = true
            Toast.makeText(context,context.getString(R.string.EntAllFdata),Toast.LENGTH_SHORT).show()
        }
    }

    fun compUpdate(id:Long , comp : Boolean){
        database?.toDoListDao()?.compUpdate(id, comp)
        compUpdateNotify()
    }

    fun compUpdateNotify(){
        database?.toDoListDao()?.getAll().let {
            getAllData = it as MutableList<ToDoListDataEntity>
        }
    }

    @SuppressLint("SimpleDateFormat")
    @WorkerThread
    fun addData(title: String, desc: String, date: String, time: String, id: Long) {
        //database?.toDoListDao()?.insert(ToDoListDataEntity(title = title, date = date, time = time))
        if (position != -1)
        {
            val oldId = database?.toDoListDao()?.update(title = title, desc = desc, date = date, time = time, id = id)
            //val cal : Calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())

            // Парсинг даты
            val dateFormat = SimpleDateFormat("dd/M/yyyy")
            val dateString = dateFormat.parse(date)

            // Парсинг времени
            val timeFormat = SimpleDateFormat("HH:mm")
            val timeString = timeFormat.parse(time)

            // Объединение даты и времени в один объект Calendar
            val calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
            calendar.time = dateString
            calendar.set(Calendar.HOUR_OF_DAY, timeString.hours)
            calendar.set(Calendar.MINUTE, timeString.minutes)
            calendar.set(Calendar.SECOND, 0) // обнуляем секунды, если они не нужны

            month = calendar.get(Calendar.MONTH)
            year = calendar.get(Calendar.YEAR)
            day = calendar.get(Calendar.DAY_OF_MONTH)

            hour = calendar.get(Calendar.HOUR_OF_DAY)
            minute = calendar.get(Calendar.MINUTE)

            //cal.set(Calendar.MONTH, month)
            //cal.set(Calendar.YEAR, year)
            //cal.set(Calendar.DAY_OF_MONTH, day)

            //cal.set(Calendar.SECOND, 0);
            //cal.set(Calendar.HOUR_OF_DAY, hour)
            //cal.set(Calendar.MINUTE, minute)

            Log.d("Alarm Title","$month , $date : ${calendar.time}")
            oldId?.let {
                setAlarm(calendar, 1, id, title,desc,hour,minute)
            }
            oldId?.let {
                setAlarm(calendar, 0, id, title,desc,hour,minute)
            }
        }
        else
        {
            val newId = database?.toDoListDao()?.insert(ToDoListDataEntity(title = title, desc = desc, date = date, time = time, isShow = 0))

            val cal : Calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())

            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.DAY_OF_MONTH, day)

            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)

            Log.d("Alarm Title","$month , $date : ${cal.time}")
            newId?.let {
                setAlarm(cal, 0, it, title,desc,hour,minute)
            }
        }

        database?.toDoListDao()?.getAll().let {
            getAllData = it as MutableList<ToDoListDataEntity>
            getPreviousList()
        }
    }

    fun getPreviousList() {
        if (!isOpenActiveTask)
            toDoList.value = getAllData
        else
            filterListByActiveTask()
    }

    fun clearList() {
        toDoList.value = emptyList()
    }

    fun filterListByTitleAndDesc(query: String) {
        if (!isOpenActiveTask){
            val filteredList = getAllData.filter { it.title.contains(query, ignoreCase = true) ||
                                                   it.desc.contains(query, ignoreCase = true) ||
                                                   it.date.contains(query, ignoreCase = true) ||
                                                   it.time.contains(query, ignoreCase = true)  }
            toDoList.value = filteredList
        }
        else{
            val filteredList = getAllData.filter { it.title.contains(query, ignoreCase = true) ||
                                                   it.desc.contains(query, ignoreCase = true) ||
                                                   it.date.contains(query, ignoreCase = true) ||
                                                   it.time.contains(query, ignoreCase = true) }
                                         .filter { !it.comp }
            toDoList.value = filteredList
        }
    }

    public var isOpenActiveTask = false
    fun filterListByActiveTask() {
        //val formatter = DateTimeFormatter.ofPattern("d/M/yyyy HH:mm")
        val filteredList = getAllData.filter {
            !it.comp
            //LocalDateTime.parse(it.date + " " + it.time, formatter).isAfter(LocalDateTime.now())
        }
        toDoList.value = filteredList
    }

    private fun parseDateAndTime(dateStr: String, timeStr: String): Calendar {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())

        // Предполагается, что dateStr и timeStr в формате "dd/MM/yy" и "HH:mm"
        val date = sdf.parse("$dateStr $timeStr") ?: return cal // Возвращает текущий календарь, если разбор не удался

        cal.time = date

        return cal
    }

    fun setAllAlarm(){
        database?.toDoListDao()?.getAll()?.forEach() {
            val cal = parseDateAndTime(it.date, it.time)
            setAlarm(cal, 0, it.id, it.title,it.desc,cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE))
        }
    }

    fun cancelAllAlarm(){
        database?.toDoListDao()?.getAll()?.forEach() {
            val cal = parseDateAndTime(it.date, it.time)
            setAlarm(cal, 1, it.id, it.title,it.desc,cal.get(Calendar.HOUR_OF_DAY),cal.get(Calendar.MINUTE))
        }
    }

    fun delete(id: Long) {
        val oldId = database?.toDoListDao()?.get(id)
        val cal : Calendar = Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())

        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.DAY_OF_MONTH, day)

        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        oldId?.let {
            setAlarm(cal, 1, id, oldId.title,oldId.desc,hour,minute)
        }
        database?.toDoListDao()?.delete(id)
        database?.toDoListDao()?.getAll().let {
            getAllData = it as MutableList<ToDoListDataEntity>
            getPreviousList()
        }
    }

    private fun setAlarm(calender: Calendar, i: Int, id: Long, title: String, desc: String, hour:Int, minute:Int)
    {
        if (calender.timeInMillis <= System.currentTimeMillis()) {
            // Время уже прошло, не устанавливаем будильник
            return
        }

        val alarmManager: AlarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        //val time = "%02d".format(hour) + ":" + "%02d".format(minute)

        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra("INTENT_NOTIFY", true)
        intent.putExtra("isShow", i)
        intent.putExtra("id", id)
        intent.putExtra("title", title)
        intent.putExtra("desc", desc)
        intent.putExtra("date",  calender.get(Calendar.DAY_OF_MONTH).toString() + "/" +
                                             (calender.get(Calendar.MONTH).toInt() + 1).toString() + "/" +
                                             calender.get(Calendar.YEAR).toString() + " " +
                                             String.format("%02d", calender.get(Calendar.HOUR_OF_DAY))+ ":" +
                                             String.format("%02d", calender.get(Calendar.MINUTE)))
        //Log.d("Alarm Title","$month , $date : ${ca.time}")
        intent.putExtra("pass", pass)
        val pandingIntent: PendingIntent = PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT)

        if (i == 0)
        {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,  calender.timeInMillis , pandingIntent)
        }
        else
        {
            alarmManager.cancel(pandingIntent)
        }
    }
}