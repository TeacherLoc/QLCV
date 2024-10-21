package com.example.qlcv

import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var listView: ListView
    private lateinit var taskAdapter: ArrayAdapter<String>
    private val taskList = mutableListOf<String>()
    private var currentTaskId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dbHelper = DatabaseHelper(this)

        try {
            dbHelper.createDatabase() // Tạo cơ sở dữ liệu nếu chưa có
        } catch (e: IOException) {
            e.printStackTrace()
        }

        dbHelper.openDatabase() // Mở cơ sở dữ liệu

        listView = findViewById(R.id.taskListView)
        loadTasks() // Tải danh sách công việc

        // Thêm công việc mới
        findViewById<Button>(R.id.btnAdd).setOnClickListener {
            showAddEditTaskDialog()
        }

        // Nhấp vào một công việc để chỉnh sửa hoặc xóa
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedTask = taskList[position]
            val taskDetails = selectedTask.split(": ")
            currentTaskId = taskDetails[0].toInt() // Giả sử ID là số đầu tiên
            showAddEditTaskDialog(taskDetails[1], taskDetails[2])
        }
    }

    private fun loadTasks() {
        taskList.clear()
        val cursor: Cursor = dbHelper.getAllTasks()
        if (cursor.moveToFirst()) {
            do {
                val taskId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                val taskName = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
                val taskDescription = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DESCRIPTION))
                taskList.add("$taskId: $taskName - $taskDescription")
            } while (cursor.moveToNext())
        }
        cursor.close()

        taskAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, taskList)
        listView.adapter = taskAdapter
    }

    private fun showAddEditTaskDialog(name: String = "", description: String = "") {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_task, null)
        val editTextName: EditText = dialogView.findViewById(R.id.editTextTaskName)
        val editTextDescription: EditText = dialogView.findViewById(R.id.editTextTaskDescription)

        editTextName.setText(name)
        editTextDescription.setText(description)

        AlertDialog.Builder(this)
            .setTitle(if (currentTaskId == -1) "Thêm công việc" else "Chỉnh sửa công việc")
            .setView(dialogView)
            .setPositiveButton(if (currentTaskId == -1) "Thêm" else "Cập nhật") { _, _ ->
                if (currentTaskId == -1) {
                    // Thêm công việc mới
                    val newName = editTextName.text.toString()
                    val newDescription = editTextDescription.text.toString()
                    dbHelper.addTask(newName, newDescription)
                } else {
                    // Cập nhật công việc hiện tại
                    val updatedName = editTextName.text.toString()
                    val updatedDescription = editTextDescription.text.toString()
                    dbHelper.updateTask(currentTaskId, updatedName, updatedDescription)
                    currentTaskId = -1 // Đặt lại id
                }
                loadTasks() // Tải lại danh sách công việc
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // Xóa công việc
    private fun showDeleteTaskDialog(taskId: Int) {
        AlertDialog.Builder(this)
            .setTitle("Xóa công việc")
            .setMessage("Bạn có chắc chắn muốn xóa công việc này?")
            .setPositiveButton("Xóa") { _, _ ->
                dbHelper.deleteTask(taskId)
                loadTasks() // Tải lại danh sách công việc
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
