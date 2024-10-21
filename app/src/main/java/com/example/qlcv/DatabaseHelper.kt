package com.example.qlcv

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class DatabaseHelper(private val context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "task.db" // Tên cơ sở dữ liệu
        private const val DB_VERSION = 1 // Phiên bản cơ sở dữ liệu
        const val TABLE_NAME = "tasks" // Tên bảng
        const val COLUMN_ID = "id" // Cột ID
        const val COLUMN_NAME = "name" // Cột tên
        const val COLUMN_DESCRIPTION = "description" // Cột mô tả
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Không cần tạo bảng mới nếu đã có từ file .db
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Kiểm tra xem cơ sở dữ liệu đã tồn tại hay chưa
    private fun checkDatabase(): Boolean {
        var checkDB: SQLiteDatabase? = null
        return try {
            val path = context.getDatabasePath(DB_NAME).absolutePath
            checkDB = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY)
            checkDB != null
        } catch (e: SQLiteException) {
            false
        } finally {
            checkDB?.close()
        }
    }

    // Sao chép cơ sở dữ liệu từ assets
    @Throws(IOException::class)
    private fun copyDatabase() {
        val input: InputStream = context.assets.open(DB_NAME)
        val output: OutputStream = FileOutputStream(context.getDatabasePath(DB_NAME).absolutePath)

        val buffer = ByteArray(1024)
        var length: Int
        while (input.read(buffer).also { length = it } > 0) {
            output.write(buffer, 0, length)
        }

        output.flush()
        output.close()
        input.close()
    }

    @Throws(IOException::class)
    fun createDatabase() {
        val dbExist = checkDatabase()
        if (!dbExist) {
            this.readableDatabase // Tạo cơ sở dữ liệu
            try {
                copyDatabase() // Sao chép cơ sở dữ liệu từ assets
            } catch (e: IOException) {
                throw Error("Lỗi sao chép cơ sở dữ liệu")
            }
        }
    }

    fun openDatabase(): SQLiteDatabase {
        return SQLiteDatabase.openDatabase(context.getDatabasePath(DB_NAME).absolutePath, null, SQLiteDatabase.OPEN_READWRITE)
    }


    // Thêm công việc
    fun addTask(name: String, description: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_DESCRIPTION, description)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    // Lấy tất cả công việc
    fun getAllTasks(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }

    // Cập nhật công việc
    fun updateTask(id: Int, name: String, description: String): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, name)
            put(COLUMN_DESCRIPTION, description)
        }
        return db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    // Xóa công việc
    fun deleteTask(id: Int): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }
}
