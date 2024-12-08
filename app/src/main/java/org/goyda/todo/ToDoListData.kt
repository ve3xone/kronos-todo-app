package org.goyda.todo

class ToDoListData(
    var indexDb: Long = 0,
    val title: String = "",
    var desc: String = "",
    val date: String = "",
    val time: String = "",
    val isShow : Int = 0,
    var comp: Boolean = false
)