package org.goyda.todo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.goyda.todo.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.add_item.view.*
import kotlinx.android.synthetic.main.settings.view.*
import org.jetbrains.anko.alert
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.util.Linkify
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
//import kotlinx.android.synthetic.main.about.view.bOk
import java.io.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.abs

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), OnItemClick {

    private val EXPORT_DB_REQUEST_CODE = 1001
    private val IMPORT_DB_REQUEST_CODE = 1002
    private val EXPORT_ICS_REQUEST_CODE = 1003
    private val IMPORT_ICS_REQUEST_CODE = 1004

    private val list = mutableListOf<ToDoListData>()

    private val c: Calendar = Calendar.getInstance()

    private val month: Int = c.get(Calendar.MONTH)
    private val year: Int = c.get(Calendar.YEAR)
    private val day: Int = c.get(Calendar.DAY_OF_MONTH)

    private var cal: Calendar = Calendar.getInstance()

    private val listAdapter = ListAdapter(list, this)

    private lateinit var binding: ActivityMainBinding

    private lateinit var viewModel: ToDoListViewModel

    private val THEME_SYSTEM = R.style.AppThemeSystem
    private val THEME_LIGHT = R.style.AppThemeLight
    private val THEME_DARK = R.style.AppThemeDark

    private lateinit var gestureDetector: GestureDetector

    private fun switchTheme(themeId: Int) {
        setTheme(themeId)
    }

    private fun saveTheme(themeId: Int) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("Theme", themeId).apply()
    }

    private fun getSavedTheme(): Int {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("Theme", THEME_SYSTEM) // По умолчанию используем системную тему
    }

    private fun saveSetupPass() {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("SetupPass", true).apply()
    }

    private fun getSetupPass(): Boolean {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("SetupPass", false)
    }

    private fun getImportDB(): Boolean {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean("ImportDB", false)
    }

    private fun saveImportDB(boolean: Boolean) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("ImportDB", boolean).apply()
    }

    private val taskCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (viewModel.isAuthenticated){
                //val dbId = intent?.getLongExtra("id", -1) ?: -1
                viewModel.compUpdateNotify()
                if (!viewModel.isOpenActiveTask){
                    if (etSearch.text.toString() != "")
                        viewModel.filterListByTitleAndDesc(etSearch.text.toString())
                    else
                        viewModel.getPreviousList()
                }
                else
                    if (etSearch.text.toString() != "")
                        viewModel.filterListByTitleAndDesc(etSearch.text.toString())
                    else
                        viewModel.filterListByActiveTask()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        //Безопасность и скрытие содержимого приложения в меню многозадачности
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        // Запрос на уведомления для Android 13 и выше
        if (Build.VERSION.SDK_INT >= 33) {
            checkNotificationPermission()
        } //else {
            // Для более старых версий Android разрешение не требуется
            //createNotificationChannel()
        //}

        switchTheme(getSavedTheme())

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        viewModel = ViewModelProviders.of(this).get(ToDoListViewModel::class.java)

        //Инверсированый список задач
        rvTodoList.layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }
        rvTodoList.adapter = listAdapter

        if (!getSetupPass()){
            showPasswordDialog(getString(R.string.crtpass), getString(R.string.crtpassdesc))
            saveSetupPass()
        }
        else{
            showPasswordDialog(getString(R.string.entpass), getString(R.string.entpassdesc))
        }

        super.onCreate(savedInstanceState)

        //Разрешить работу в фоне (уведомления)
        disableBatteryOptimization(this)
    }

    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            // Разрешение не предоставлено, запрашиваем его
            ActivityCompat.requestPermissions(this, arrayOf("android.permission.POST_NOTIFICATIONS"), 100)
        } //else {
            // Разрешение уже предоставлено
            //createNotificationChannel()
        //}
    }

    //private fun createNotificationChannel() {
    //    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    //        val channel = NotificationChannel("YOUR_CHANNEL_ID", "Channel Name", NotificationManager.IMPORTANCE_DEFAULT).apply {
    //            description = "Channel Description"
    //        }
    //        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    //        notificationManager.createNotificationChannel(channel)
    //    }
    //}

    //override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    //    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    //    if (requestCode == 100) {
    //        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Разрешение предоставлено
                //createNotificationChannel()
    //        } else {
                // Разрешение отклонено, можно направить пользователя в настройки
                // Например, показать диалог или уведомление
                // Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
    //        }
    //    }
    //}

    private var dialogView = false
    private var password = ""
    private fun showPasswordDialog(title: String, message: String) {
        dialogView = true
        val passwordEditText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setTextColor(Color.WHITE)
        }
        val dialog = AlertDialog.Builder(this, R.style.PasswordDialogTheme)
            .setTitle(title)
            .setMessage(message)
            .setView(passwordEditText)
            .setPositiveButton("OK", null)
            .setNegativeButton(getString(R.string.exit)) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .create()

        dialog.show()
        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                finish()
                true
            } else {
                false
            }
        }
        dialog.setOnCancelListener {
            finish()
        }

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!viewModel.isAuthenticated){
                password = passwordEditText.text.toString()
                viewModel.initializeDatabase(password)
                if (getImportDB()){
                    viewModel.setAllAlarm()
                    saveImportDB(false)
                }
                if (viewModel.isAuthenticated){
                    showAll()
                    dialogView = false
                    dialog.dismiss()
                }
                else{
                    passwordEditText.text.clear()
                    Toast.makeText(this, getString(R.string.badpass), Toast.LENGTH_SHORT).show()
                }
            }
            else
                if (passwordEditText.text.toString() == password){
                    dialogView = false
                    dialog.dismiss()
                }
                else{
                    passwordEditText.text.clear()
                    Toast.makeText(this, getString(R.string.badpass), Toast.LENGTH_SHORT).show()
                }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showAll() {
        //Жесты
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private var lastTapTime: Long = 0

            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                if (e1 != null && e2 != null) {
                    // Проверка, что с момента последнего касания прошло достаточно времени
                    if (System.currentTimeMillis() - lastTapTime > 250) {
                        if (e1.x - e2.x > 215 && Math.abs(velocityX) > 215) {
                            // Swipe left - show next tab
                            val currentTab = tabs.selectedTabPosition
                            val nextTab = (currentTab + 1) % tabs.tabCount
                            tabs.getTabAt(nextTab)?.select()
                            return true
                        } else if (e2.x - e1.x > 215 && abs(velocityX) > 215) {
                            // Swipe right - show previous tab
                            val currentTab = tabs.selectedTabPosition
                            val previousTab = if (currentTab - 1 < 0) tabs.tabCount - 1 else currentTab - 1
                            tabs.getTabAt(previousTab)?.select()
                            return true
                        }
                    }
                }
                return false
            }

            override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
                lastTapTime = System.currentTimeMillis()
                e?.let {
                    val childView = rvTodoList.findChildViewUnder(it.x, it.y)
                    if (childView != null && rvTodoList.scrollState == RecyclerView.SCROLL_STATE_IDLE) {
                        val position = rvTodoList.getChildAdapterPosition(childView)
                        onItemClick(childView, position)
                        return true
                    }
                }
                return false
            }
        })
        rvTodoList.setOnTouchListener { v, event ->
            if (gestureDetector.onTouchEvent(event)) {
                true
            } else {
                v.onTouchEvent(event)
            }
        }

        rvTodoList.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val action = e.action
                when (action) {
                    MotionEvent.ACTION_DOWN -> {
                        rv.parent.requestDisallowInterceptTouchEvent(true)
                    }
                }
                gestureDetector.onTouchEvent(e)
                return false
            }
        })

        //Кнопка +
        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            //viewModel.clearList()
            //viewModel.getPreviousList() // Повторная загрузка данных
            dialogAddAndEditItem("","","","", false)
        }

        //Вкладки
        setupTabs()

        binding.vieModel = viewModel
        viewModel.getPreviousList()

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                viewModel.filterListByTitleAndDesc(query)
            }
        })

        viewModel.toDoList.observe(this, androidx.lifecycle.Observer { it ->
            //list.addAll(it)
            if (it == null)
                return@Observer

            list.clear()
            val tempList = mutableListOf<ToDoListData>()

            it.forEach {
                tempList.add(
                    ToDoListData(
                        title = it.title,
                        desc = it.desc,
                        date = it.date,
                        time = it.time,
                        indexDb = it.id,
                        isShow = it.isShow,
                        comp = it.comp
                    )
                )
            }
            //list.addAll(tempList.filter { it.isShow > 1 }?.sortedByDescending { it.isShow })
            val formatter = DateTimeFormatter.ofPattern("d/M/yyyy HH:mm")
            val sortedList = tempList.sortedBy { LocalDateTime.parse(it.date+" "+it.time, formatter) }
            list.addAll(sortedList)
            //list.addAll(tempList)
            listAdapter.notifyDataSetChanged()
            viewModel.position = -1

            viewModel.toDoList.value = null
        })

        viewModel.toDoListData.observe(this, androidx.lifecycle.Observer {
            if (viewModel.position != -1) {
                list[viewModel.position] = it
                listAdapter.notifyItemChanged(viewModel.position)
            } else {
                list.add(it)
                listAdapter.notifyDataSetChanged()
            }
            viewModel.position = -1
        })

        val filter = IntentFilter("org.goyda.todo.ACTION_TASK_COMPLETE")
        registerReceiver(taskCompleteReceiver, filter)
    }

    @SuppressLint("BatteryLife")
    private fun disableBatteryOptimization(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:" + context.packageName)
            }
            context.startActivity(intent)
        }
    }

    // Сохранение состояния
    private var allState: Parcelable? = null
    private var activeState: Parcelable? = null
    private fun setupTabs() {
        val tabLayout: TabLayout = findViewById(R.id.tabs)
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.all)))
        tabLayout.addTab(tabLayout.newTab().setText(getString(R.string.active)))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        if (viewModel.isOpenActiveTask)
                            activeState = rvTodoList?.layoutManager?.onSaveInstanceState()
                        viewModel.isOpenActiveTask = false
                        if (etSearch.text.toString() != "")
                            viewModel.filterListByTitleAndDesc(etSearch.text.toString())
                        else
                            viewModel.getPreviousList()
                        rvTodoList?.layoutManager?.onRestoreInstanceState(allState)
                    }
                    1 -> {
                        if (!viewModel.isOpenActiveTask)
                            allState = rvTodoList?.layoutManager?.onSaveInstanceState()
                        viewModel.isOpenActiveTask = true
                        if (etSearch.text.toString() != "")
                            viewModel.filterListByTitleAndDesc(etSearch.text.toString())
                        else
                            viewModel.filterListByActiveTask()
                        rvTodoList?.layoutManager?.onRestoreInstanceState(activeState)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)

    private var addItemDialog: AlertDialog? = null

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    fun dialogAddAndEditItem(title: String, desc: String, date: String, time: String, edit: Boolean) {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.add_item, null)

        builder.setView(dialogView)

        addItemDialog = builder.create()
        addItemDialog?.show()

        if (edit) {
            dialogView.bAddAndEditList.text = getString(R.string.edit)
            dialogView.editText.text = title.toEditable()
            dialogView.etDesc.text = desc.toEditable()
            dialogView.etdate.text = date.toEditable()
            dialogView.etTime.text = time.toEditable()
            viewModel.title.set(title)
            viewModel.desc.set(desc)
            viewModel.date.set(date)
            viewModel.time.set(time)
            dialogView.editText.isFocusable = true
        }

        dialogView.editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.title.set(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        dialogView.etdate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.date.set(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        dialogView.etDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.desc.set(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        dialogView.etTime.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.time.set(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        dialogView.etdate.setOnClickListener {
            val dpd = DatePickerDialog(this, { _, year, monthOfYear, dayOfMonth ->
                dialogView.etdate.setText("$dayOfMonth/${monthOfYear + 1}/$year")
                viewModel.month = monthOfYear
                viewModel.year = year
                viewModel.day = dayOfMonth
            }, year, month, day)

            dpd.datePicker.minDate = System.currentTimeMillis() - 1000
            dpd.show()
        }

        dialogView.etTime.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                this.cal.set(Calendar.HOUR_OF_DAY, hour)
                this.cal.set(Calendar.MINUTE, minute)

                viewModel.hour = hour
                viewModel.minute = minute

                dialogView.etTime.setText(SimpleDateFormat("HH:mm").format(cal.time))
            }

            this.cal = cal
            TimePickerDialog(
                this,
                timeSetListener,
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
            ).show()
        }

        dialogView.bAddAndEditList.setOnClickListener {
            viewModel.click(it)
            if (!viewModel.Error)
                addItemDialog?.dismiss()
        }

        dialogView.bCancel.setOnClickListener { addItemDialog?.dismiss() }
    }

    // Переинициализация активности
    private fun reinitactive(){
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
    }

    private var settingsDialog: AlertDialog? = null

    @SuppressLint("CommitPrefEdits")
    private fun dialogSettings() {
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.settings, null)

        builder.setView(dialogView)

        settingsDialog = builder.create()
        settingsDialog?.show()

        when (getSavedTheme()) {
            THEME_SYSTEM -> dialogView.rBSystemTheme.isChecked = true
            THEME_DARK -> dialogView.rBDark.isChecked = true
            THEME_LIGHT -> dialogView.rBWhiteTheme.isChecked = true
        }

        dialogView.rBSystemTheme.setOnClickListener {
            saveTheme(R.style.AppThemeSystem)
            setTheme(R.style.AppThemeSystem)
            reinitactive()
        }

        dialogView.rBWhiteTheme.setOnClickListener {
            saveTheme(R.style.AppThemeLight)
            setTheme(R.style.AppThemeLight)
            reinitactive()
        }

        dialogView.rBDark.setOnClickListener {
            saveTheme(R.style.AppThemeDark)
            setTheme(R.style.AppThemeDark)
            reinitactive()
        }

        dialogView.bExportDB.setOnClickListener {
            val timeStamp = SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault()).format(Date())
            val exportIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
                putExtra(Intent.EXTRA_TITLE, "krokus-backup_$timeStamp.zip")
            }
            startActivityForResult(exportIntent, EXPORT_DB_REQUEST_CODE)
        }

        dialogView.bImportDB.setOnClickListener {
            val importIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/zip"
            }
            startActivityForResult(importIntent, IMPORT_DB_REQUEST_CODE)
        }

        // tasks.ics: Выбирите папку и нажмите использовать
        // в неё создаться файл с именем tasks.ics и его можно будет использовать в календаре
        dialogView.bExportICS.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, EXPORT_ICS_REQUEST_CODE)
        }

        dialogView.bImportICS.setOnClickListener {
            // Вызываем Intent для выбора файла для импорта
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "text/calendar"
            startActivityForResult(intent, IMPORT_ICS_REQUEST_CODE)
        }

        dialogView.cancel.setOnClickListener { settingsDialog?.dismiss() }
    }

    @SuppressLint("SdCardPath", "Recycle")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == EXPORT_DB_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            val sourceDir = "/data/data/org.goyda.todo/databases"
            val outputFile = uri?.let { contentResolver.openFileDescriptor(it, "w")?.fileDescriptor }
            zipFiles(sourceDir, outputFile)
        }

        if (requestCode == IMPORT_DB_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            val zipFile = uri?.let { contentResolver.openInputStream(it) }
            val outputDir = "/data/data/org.goyda.todo/databases"
            viewModel.cancelAllAlarm()
            unzipFiles(zipFile, outputDir)
            //Перезапуск RecyclerView
            saveImportDB(true)
            val intent = Intent(applicationContext, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
            finish()
        }

        if (requestCode == EXPORT_ICS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                val documentFile = DocumentFile.fromTreeUri(this, uri)
                if (documentFile != null) {
                    exportTasksToICS(list, documentFile)
                }
            }
        }

        if (requestCode == IMPORT_ICS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                importTasksFromICS(uri)
            }
        }
    }

    private val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault())
    private val inputDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val outputDateFormat = SimpleDateFormat("dd/M/yyyy", Locale.getDefault())
    private val outputTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    private fun formatDateAndTime(date: String, time: String): String {
        val dateTime = "$date $time"
        val parsedDate = inputDateFormat.parse(dateTime)
        return dateFormat.format(parsedDate)
    }

    private fun importTasksFromICS(fileUri: Uri) {
        val inputStream = contentResolver.openInputStream(fileUri)
        inputStream?.use { stream ->
            val icsContent = stream.bufferedReader().use { it.readText() }
            parseICSContent(icsContent)
        }
        Toast.makeText(this, getString(R.string.comp_import_ics), Toast.LENGTH_SHORT).show()
    }

    private fun parseICSContent(icsContent: String) {
        val lines = icsContent.lines()

        var title = ""
        var desc = ""
        var date = ""
        var time = ""
        var formatloc = ""

        lines.forEach { line ->
            when {
                line.startsWith("DTSTART;") -> {
                    val startDateString = line.substringAfter("DTSTART;")
                    val dateStringWithoutTZID = startDateString.substringAfterLast(":")
                    val parsedDateTime = dateFormat.parse(dateStringWithoutTZID)

                    if (parsedDateTime != null) {
                        viewModel.hour = parsedDateTime.hours
                        viewModel.minute = parsedDateTime.minutes
                        viewModel.day = parsedDateTime.date
                        viewModel.month = parsedDateTime.month
                        viewModel.year = dateStringWithoutTZID.substring(0,4).toInt()
                        date = outputDateFormat.format(parsedDateTime)
                        time = outputTimeFormat.format(parsedDateTime)
                    }

                }
                line.startsWith("DTSTART:") -> {
                    val startDateString = line.substringAfter("DTSTART:")
                    val parsedDateTime = dateFormat.parse(startDateString)

                    if (parsedDateTime != null) {
                        viewModel.hour = parsedDateTime.hours
                        viewModel.minute = parsedDateTime.minutes
                        viewModel.day = parsedDateTime.date
                        viewModel.month = parsedDateTime.month
                        viewModel.year = startDateString.substring(0,4).toInt()
                        date = outputDateFormat.format(parsedDateTime)
                        time = outputTimeFormat.format(parsedDateTime)
                    }
                }
                line.startsWith("SUMMARY:") -> {
                    title = line.substringAfter("SUMMARY:").replace("\\,", ",")
                                                                   .replace("\\\"", "\"")
                }
                line.startsWith("LOCATION:") -> {
                    formatloc = line.substringAfter("LOCATION:").replace("\\,", ",")
                }
                line.startsWith("DESCRIPTION:") -> {
                    val formatDesc = line.substringAfter("DESCRIPTION:").replace("\\n", "\n")
                                                                                .replace("\\,", ",")
                    desc = if (formatloc != ""){
                        "Где проходит: $formatloc\n\n$formatDesc"
                    } else{
                        formatDesc
                    }
                    formatloc = ""
                }
                line.startsWith("END:VEVENT") -> {
                    if (title != "" && desc != "") {
                        viewModel.addData(title,desc,date,time,viewModel.index)
                    }

                    // Сбрасываем значения
                    title = ""
                    date = ""
                    time = ""
                    desc = ""
                }
            }
        }
    }

    private fun exportTasksToICS(tasks: List<ToDoListData>, directory: DocumentFile) {
        val timeStamp = SimpleDateFormat("dd-MM-yyyy_HH:mm:ss", Locale.getDefault()).format(Date())
        val outputFile = directory.createFile("text/calendar", "tasks_$timeStamp.ics")
        val outputStream = outputFile?.uri?.let { contentResolver.openOutputStream(it) }
        outputStream.use { stream ->
            val icsContent = generateICSContent(tasks)
            stream?.write(icsContent.toByteArray())
        }
        Toast.makeText(this, getString(R.string.comp_export_ics), Toast.LENGTH_SHORT).show()
    }

    private fun generateICSContent(tasks: List<ToDoListData>): String {
        val icsBuilder = StringBuilder()
        icsBuilder.appendln("BEGIN:VCALENDAR")
        icsBuilder.appendln("VERSION:2.0")
        icsBuilder.appendln("PRODID:-//My App//My App 1.0//EN")

        //val dateFormat = SimpleDateFormat("yyyyMMdd'T'HHmmss", Locale.getDefault())

        tasks.forEach { task ->
            icsBuilder.appendln("BEGIN:VEVENT")
            icsBuilder.appendln("UID:${task.indexDb}")
            icsBuilder.appendln("SUMMARY:${task.title}")
            icsBuilder.appendln("DESCRIPTION:${task.desc}")
            val formattedDateTime = formatDateAndTime(task.date, task.time)
            icsBuilder.appendln("DTSTART:$formattedDateTime")
            //заглушка
            icsBuilder.appendln("DTEND:$formattedDateTime")
            icsBuilder.appendln("END:VEVENT")
        }

        icsBuilder.appendln("END:VCALENDAR")
        return icsBuilder.toString()
    }

    private fun zipFiles(sourceDir: String, outputFile: FileDescriptor?) {
        val sourceDirFile = File(sourceDir)
        if (!sourceDirFile.isDirectory) return

        val files = sourceDirFile.listFiles() ?: return

        val outputStream = FileOutputStream(outputFile)

        ZipOutputStream(outputStream).use { zipOutputStream ->
            files.forEach { file ->
                FileInputStream(file).use { fileInputStream ->
                    zipOutputStream.putNextEntry(ZipEntry(file.name))
                    fileInputStream.copyTo(zipOutputStream)
                    zipOutputStream.closeEntry()
                }
            }
        }
        Toast.makeText(this, getString(R.string.comp_export_db), Toast.LENGTH_SHORT).show()
    }

    private fun unzipFiles(zipFile: InputStream?, outputDir: String) {
        if (zipFile == null) return

        val buffer = ByteArray(1024)

        ZipInputStream(zipFile).use { zipInputStream ->
            var zipEntry = zipInputStream.nextEntry
            while (zipEntry != null) {
                val newFile = File(outputDir, zipEntry.name)
                FileOutputStream(newFile).use { fileOutputStream ->
                    var len: Int
                    while (zipInputStream.read(buffer).also { len = it } > 0) {
                        fileOutputStream.write(buffer, 0, len)
                    }
                }
                zipEntry = zipInputStream.nextEntry
            }
        }
        Toast.makeText(this, getString(R.string.comp_import_db), Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    private var aboutDialog: AlertDialog? = null

    @SuppressLint("SetTextI18n")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Обработка нажатия на пункт меню "Settings"
                dialogSettings()
                true
            }
            R.id.about_info -> {
                val builder = AlertDialog.Builder(this)
                val dialogView = layoutInflater.inflate(R.layout.about, null)
                builder.setView(dialogView)
                val dialog = builder.create()
                aboutDialog = dialog
                dialog.show()

                val versionLabel = dialogView.findViewById<TextView>(R.id.versionLabel2)

                try {
                    val versionName = packageManager.getPackageInfo(packageName, 0).versionName
                    versionLabel.text = versionName
                } catch (e: PackageManager.NameNotFoundException) {
                    versionLabel.text = "Неизвестная версия"
                }

                dialogView.findViewById<View>(R.id.bOk).setOnClickListener { dialog.dismiss() }

                true
            }
            R.id.stats -> {
                val builder = AlertDialog.Builder(this)
                val dialogView = layoutInflater.inflate(R.layout.stat, null)
                builder.setView(dialogView)
                val dialog = builder.create()
                aboutDialog = dialog
                dialog.show()

                // Fetch statistics
                val statistics = viewModel.getTaskStatistics()

                // Update the TextViews with the statistics
                val labelTaskValue = dialogView.findViewById<TextView>(R.id.label_tasks_value)
                labelTaskValue.text = statistics["total"].toString()

                val labelTaskCompValue = dialogView.findViewById<TextView>(R.id.label_tasks_comp_value)
                labelTaskCompValue.text = statistics["completed"].toString()

                val labelTaskNotCompValue = dialogView.findViewById<TextView>(R.id.label_tasks_not_comp_value)
                labelTaskNotCompValue.text = statistics["notCompleted"].toString()

                val labelTaskEfficiencyValue = dialogView.findViewById<TextView>(R.id.label_tasks_efficiency_value)
                val efficiency = if (statistics["total"] != 0) {
                    (statistics["completed"]!!.toDouble() / statistics["total"]!! * 100).toInt()
                } else {
                    0
                }
                labelTaskEfficiencyValue.text = "$efficiency %"

                dialogView.findViewById<View>(R.id.bOk).setOnClickListener { dialog.dismiss() }

                true
            }
            // Добавьте обработку других пунктов меню, если необходимо
            else -> super.onOptionsItemSelected(item)
        }
    }

    private var isOpenTask = false
    private var isOpenCheckBoxComplete = false

    override fun onItemClick(v: View, position: Int) {
        if (v.id == R.id.checkBoxComplete) {
            val item = list[position]
            viewModel.compUpdate(item.indexDb,!item.comp)
            if (viewModel.isOpenActiveTask){
                if (etSearch.text.toString() != "")
                    viewModel.filterListByTitleAndDesc(etSearch.text.toString())
                else
                    viewModel.getPreviousList()
            }
            else
                listAdapter.notifyItemChanged(viewModel.position)
            isOpenCheckBoxComplete = true
            return
        }
        if (!isOpenCheckBoxComplete) {
            if (!isOpenTask) {
                isOpenTask = true

                val title = list[position].title + "\n"
                val desc = "\n" + list[position].desc + "\n\n\n"
                val dateTime = list[position].date + " " + list[position].time

                val boldTitle = SpannableString(title).apply {
                    setSpan(StyleSpan(Typeface.BOLD), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(RelativeSizeSpan(1.25f), 0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }

                val spannableDesc = SpannableString(desc)
                val spannableDateTime = SpannableString(dateTime)

                val spannableMessage = SpannableStringBuilder().apply {
                    append(boldTitle)
                    append(spannableDesc)
                    append(spannableDateTime)
                }
                Linkify.addLinks(spannableMessage, Linkify.WEB_URLS)

                val alert = alert {
                    //title = list[position].title
                    message = spannableMessage
                    positiveButton(getString(R.string.edit)) {
                        viewModel.position = position
                        viewModel.index = list[position].indexDb
                        dialogAddAndEditItem(
                            list[position].title,
                            list[position].desc,
                            list[position].date,
                            list[position].time,
                            true
                        )
                        isOpenTask = false
                    }
                    negativeButton(getString(R.string.delete)) {
                        viewModel.delete(list[position].indexDb)
                        isOpenTask = false
                    }
                    onCancelled {
                        // Действие при отмене alert
                        isOpenTask = false
                    }
                }.show()

                (alert as? AlertDialog)?.findViewById<TextView>(android.R.id.message)?.movementMethod =
                    LinkMovementMethod.getInstance()
            }
        }
        else{
            isOpenCheckBoxComplete = false
        }
    }

    private var isReturningFromBackground = false

    override fun onResume() {
        super.onResume()
        // Проверяем, возвращается ли пользователь из фонового режима
        if (isReturningFromBackground) {
            if (!dialogView)
                showPasswordDialog(getString(R.string.entpass), getString(R.string.entpassdesc))
        }
        isReturningFromBackground = false
        // Регистрация ресивера снова
        val filter = IntentFilter("org.goyda.todo.ACTION_TASK_COMPLETE")
        registerReceiver(taskCompleteReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        isReturningFromBackground = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (settingsDialog?.isShowing == true) {
            settingsDialog?.dismiss()
        }
        if (addItemDialog?.isShowing == true) {
            addItemDialog?.dismiss()
        }
        if (aboutDialog?.isShowing == true) {
            aboutDialog?.dismiss()
        }
        //settingsDialog?.dismiss()
        //addItemDialog?.dismiss()
        //aboutDialog?.dismiss()
        //passwordProblem = true
        unregisterReceiver(taskCompleteReceiver)
    }

    //override fun onStop() {
    //    super.onStop()
    //}
}