package org.goyda.todo

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        switchTheme(getSavedTheme())
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        viewModel = ViewModelProviders.of(this).get(ToDoListViewModel::class.java)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            //viewModel.clearList()
            //viewModel.getPreviousList() // Повторная загрузка данных
            dialogAddAndEditItem("","","","", false)
        }

        // Проверяем, было ли уже дано разрешение на уведомления
        if (!isNotificationPermissionGranted()) {
            // Если разрешение не было дано, запрашиваем его
            requestNotificationPermission()
        }

        //Инверсированый список задач
        rvTodoList.layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }
        rvTodoList.adapter = listAdapter
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
                        isShow = it.isShow
                    )
                )

            }
            //list.addAll(tempList.filter { it.isShow > 1 }?.sortedByDescending { it.isShow })
            list.addAll(tempList.sortedBy { it.date + it.time })
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
    }

    // Для первого запуска приложения
    // Взять сразу разрешение на уведомления
    private fun isNotificationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NOTIFICATION_POLICY) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_NOTIFICATION_POLICY), 123)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 123) {
            // Проверяем, дано ли разрешение на уведомления
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение дано, выполните действия, которые нужно выполнить после получения разрешения
            } else {
                // Разрешение не дано, выполните другие действия, чтобы обработать отказ
            }
        }
    }

    private fun String.toEditable(): Editable =  Editable.Factory.getInstance().newEditable(this)

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    fun dialogAddAndEditItem(title: String, desc: String, date: String, time:String, edit: Boolean){
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.add_item, null)

        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        if (edit){
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
                // При изменении текста в EditText обновляем значение переменной viewModel.title
                viewModel.title.set(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        dialogView.etdate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // При изменении текста в EditText обновляем значение переменной viewModel.title
                viewModel.date.set(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        dialogView.etDesc.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // При изменении текста в EditText обновляем значение переменной viewModel.title
                viewModel.desc.set(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        dialogView.etTime.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // При изменении текста в EditText обновляем значение переменной viewModel.title
                viewModel.time.set(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        dialogView.etdate.setOnClickListener {

            val dpd = DatePickerDialog(this, { view, year, monthOfYear, dayOfMonth ->

                // Display Selected date in textbox
                dialogView.etdate.setText("" + dayOfMonth + "/" + (monthOfYear + 1) + "/" + year)
                viewModel.month = monthOfYear
                viewModel.year = year
                viewModel.day = dayOfMonth
            }, year, month, day)

            dpd.datePicker.minDate = System.currentTimeMillis() - 1000
            dpd.show()

        }

        dialogView.etTime.setOnClickListener {
            val cal = Calendar.getInstance()
            val timeSetListener = TimePickerDialog.OnTimeSetListener { timePicker, hour, minute ->
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
            dialog.dismiss()
        }

        dialogView.bCancel.setOnClickListener { dialog.dismiss() }
    }

    @SuppressLint("CommitPrefEdits")
    private fun dialogSettings(){
        val builder = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.settings, null)

        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        when (getSavedTheme()) {
            THEME_SYSTEM -> dialogView.rBSystemTheme.isChecked = true
            THEME_DARK -> dialogView.rBDark.isChecked = true
            THEME_LIGHT -> dialogView.rBWhiteTheme.isChecked = true
        }

        dialogView.rBSystemTheme.setOnClickListener{
            saveTheme(R.style.AppThemeSystem)
            setTheme(R.style.AppThemeSystem)

            // Перезапускаем активность, чтобы применить новую тему
            recreate()
        }

        dialogView.rBWhiteTheme.setOnClickListener{
            saveTheme(R.style.AppThemeLight)
            setTheme(R.style.AppThemeLight)

            recreate()
        }

        dialogView.rBDark.setOnClickListener{
            saveTheme(R.style.AppThemeDark)
            setTheme(R.style.AppThemeDark)

            recreate()
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
        dialogView.bExportICS.setOnClickListener{
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            startActivityForResult(intent, EXPORT_ICS_REQUEST_CODE)
        }

        dialogView.bImportICS.setOnClickListener{
            // Вызываем Intent для выбора файла для импорта
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.type = "text/calendar"
            startActivityForResult(intent, IMPORT_ICS_REQUEST_CODE)
        }

        dialogView.cancel.setOnClickListener { dialog.dismiss() }
    }

    @SuppressLint("SdCardPath")
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
            unzipFiles(zipFile, outputDir)
            //Перезапуск RecyclerView
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
    private val outputDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Обработка нажатия на пункт меню "Settings"
                dialogSettings()
                true
            }
            // Добавьте обработку других пунктов меню, если необходимо
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClick(v: View, position: Int) {
        alert {
            title = list[position].title
            message = "\n" + list[position].desc + "\n\n\n${list[position].date} ${list[position].time}"
            positiveButton(getString(R.string.edit)) {
                viewModel.position = position
                viewModel.index = list[position].indexDb
                dialogAddAndEditItem(list[position].title,
                                     list[position].desc,
                                     list[position].date,
                                     list[position].time,
                                     true)
            }
            negativeButton(getString(R.string.delete)) {
                viewModel.delete(list[position].indexDb)
            }
        }.show()
    }

//    override fun onResume() {
//        super.onResume()
//    }
//
//    override fun onStop() {
//        super.onStop()
//    }

}