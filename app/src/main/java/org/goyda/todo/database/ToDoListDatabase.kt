package org.goyda.todo.database

import android.content.Context
import android.util.Base64
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@Database(entities = arrayOf(ToDoListDataEntity::class), version = 1)
abstract class ToDoListDatabase : RoomDatabase()
{
    abstract fun toDoListDao() : ToDoListDAO

    companion object{
        @Volatile
        private var instanse : ToDoListDatabase? = null

        // Гавнокод)) : Моя кодовая фраза
        //BASE64 xD
        private const val passwordApp: String = "0JzQtNCwINGD0LYg0LrQsNC6INC20LUg0Y8g0LfQsNC00L7Qu9Cx0LDQu9GB0Y8g0LTQtdC70LDRgtGMINC/0YDQvtC10LrRgiDQtNC70Y8g0L/RgNC+0LXQutGC0L3QvtCz0L4g0L/RgNCw0LrRgtC40LrRg9C80LAg0LrQvtGC0L7RgNGL0Lkg0LzQvdC1INC90LjQutC+0LPQtNCwINC90LUg0L/RgNC40LPQvtC00LjRgtGB0Y8uLi4="

        fun getInstance(context: Context, passwordUser: String): ToDoListDatabase? {
            //if (instanse == null) {
                synchronized(ToDoListDatabase::class) {
                    val passphrase: ByteArray = sha256(passwordApp) +
                                                sha256(stringToBase64(passwordUser))
                    val factory = SupportFactory(passphrase)
                    instanse = Room.databaseBuilder(context.applicationContext,
                        ToDoListDatabase::class.java, "todo-db")
                        .openHelperFactory(factory)
                        .allowMainThreadQueries()
                        .build()
                }
            //}
            return instanse
        }

        private fun stringToBase64(input: String): String {
            return Base64.encodeToString(input.toByteArray(), Base64.DEFAULT).trim()
        }

        private fun sha256(input: String): ByteArray {
            val md = MessageDigest.getInstance("SHA-256")
            return md.digest(input.toByteArray(StandardCharsets.UTF_8))
        }
    }
}