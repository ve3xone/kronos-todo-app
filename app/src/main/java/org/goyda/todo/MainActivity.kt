package org.goyda.todo

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import org.jetbrains.anko.alert
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), OnItemClick {

    private val list = mutableListOf<ToDoListData>()

    private val c: Calendar = Calendar.getInstance()

    private val month: Int = c.get(Calendar.MONTH)
    private val year: Int = c.get(Calendar.YEAR)
    private val day: Int = c.get(Calendar.DAY_OF_MONTH)

    private var cal: Calendar = Calendar.getInstance()

    private val listAdapter = ListAdapter(list, this)

    private lateinit var binding: ActivityMainBinding

    private lateinit var viewModel: ToDoListViewModel


    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.lifecycleOwner = this
        viewModel = ViewModelProviders.of(this).get(ToDoListViewModel::class.java)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            dialogAddAndEditItem("","","","", false)
        }

        //Инверсированый список задач
        rvTodoList.layoutManager = LinearLayoutManager(this).apply {
            reverseLayout = true
            stackFromEnd = true
        }
        rvTodoList.adapter = listAdapter
        binding.vieModel = viewModel

        viewModel.getPreviousList()

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

            list.addAll(tempList)
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

        // Проверяем, было ли уже дано разрешение на уведомления
        if (!isNotificationPermissionGranted()) {
            // Если разрешение не было дано, запрашиваем его
            requestNotificationPermission()
        }
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                // Обработка нажатия на пункт меню "Settings"
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
