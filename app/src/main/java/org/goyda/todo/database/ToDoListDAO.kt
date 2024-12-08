package org.goyda.todo.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ToDoListDAO {
    @Query("SELECT * from todolist")
    fun getAll(): List<ToDoListDataEntity>

    @Insert
    fun insert(toDoListData: ToDoListDataEntity) : Long

    @Query("UPDATE todolist  SET title = :title, desc = :desc, date = :date, time = :time where id LIKE :id")
    fun update(
        title: String,
        desc: String,
        date: String,
        time: String,
        id: Long
    )

    @Query("DELETE From todolist where id = :id")
    fun delete(id : Long)

    @Query("UPDATE todolist Set isShow = :isShow where id LIKE :id")
    fun isShownUpdate(id:Long , isShow : Int)

    @Query("UPDATE todolist Set comp = :comp where id LIKE :id")
    fun compUpdate(id:Long , comp : Boolean)

    @Query("SELECT * from todolist where id Like :id")
    fun get(id : Long): ToDoListDataEntity
    
    @Query("SELECT COUNT(*) FROM todolist")
    fun getTotalTasks(): Int

    @Query("SELECT COUNT(*) FROM todolist WHERE comp = 1")
    fun getCompletedTasks(): Int

    @Query("SELECT COUNT(*) FROM todolist WHERE comp = 0")
    fun getNotCompletedTasks(): Int
}