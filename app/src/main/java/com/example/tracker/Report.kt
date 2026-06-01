package com.example.tracker

import Data.Expense
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.Locale

class Report : AppCompatActivity() {
    private lateinit var edtStartDate: EditText
    private lateinit var edtEndDate: EditText
    private lateinit var edtSearchName: EditText
    private lateinit var btnFilter: Button
    private lateinit var txtTotal: TextView
    private lateinit var expensesContainer: LinearLayout


    private lateinit var returnhome: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report)


        edtStartDate = findViewById(R.id.startdate)
        edtEndDate = findViewById(R.id.enddate)
        edtSearchName = findViewById(R.id.searchname)
        btnFilter = findViewById(R.id.filter)
        txtTotal = findViewById(R.id.Total)
        expensesContainer = findViewById(R.id.expensesContainer)
        returnhome = findViewById(R.id.Return_From_Expenses)

        edtStartDate.setOnClickListener {
            showStartDatePicker()
        }
        edtEndDate.setOnClickListener {
            showEndDatePicker()
        }
        btnFilter.setOnClickListener {
            filterExpenses()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //home
        returnhome.setOnClickListener {
            Toast.makeText(this, "Going home", Toast.LENGTH_SHORT).show()

            // Point the intent specifically to your Home class
            val intent = Intent(this, Home::class.java)

            // Clear out any other screens sitting on top of the Home page
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

            startActivity(intent)
            finish()
        }
    }

    @SuppressLint("SetTextI18n", "UseKtx")
    private fun filterExpenses() {
        val startDate = edtStartDate.text.toString()
        val endDate = edtEndDate.text.toString()
        val searchName = edtSearchName.text.toString().trim().lowercase(Locale.getDefault())

        // Validation checks (kept exactly the same)
        if (startDate.isEmpty() && endDate.isEmpty() && searchName.isEmpty()) {
            Toast.makeText(this, "Please enter dates or a search term", Toast.LENGTH_SHORT).show()
            return
        }
        if ((startDate.isNotEmpty() && endDate.isEmpty()) || (startDate.isEmpty() && endDate.isNotEmpty())) {
            Toast.makeText(this, "Please select both start and end dates", Toast.LENGTH_LONG).show()
            return
        }

        // Pull from Firebase instead of Room
        val dbRef = FirebaseDatabase.getInstance().getReference("expenses")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                expensesContainer.removeAllViews() // Clear old list rows from layout
                var totalAmount = 0.0

                for (dataSnapshot in snapshot.children) {
                    val item = dataSnapshot.getValue(Expense::class.java)
                    if (item != null) {

                        // 1. Filter by Date range if dates are provided
                        var matchesDate = true
                        if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                            // Simple string comparison works perfectly for YYYY-MM-DD formats
                            matchesDate = item.date in startDate..endDate
                        }

                        // 2. Filter by Name/Category search keyword if provided
                        var matchesName = true
                        if (searchName.isNotEmpty()) {
                            matchesName = item.category.lowercase(Locale.getDefault()).contains(searchName) ||
                                    item.description.lowercase(Locale.getDefault()).contains(searchName)
                        }

                        // 3. If it passes our filters, add it to the UI view list container
                        if (matchesDate && matchesName) {
                            totalAmount += item.amount

                            // Create a simple text view dynamically for this item row
                            val textView = TextView(this@Report).apply {
                                text = "${item.date} - ${item.category}: R${item.amount}\n${item.description}\n"
                                textSize = 16f
                            }
                            expensesContainer.addView(textView)
                        }
                    }
                }
                // Update the grand total display text component
                txtTotal.text = "Total: R$totalAmount"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Report, "Failed to load report", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @SuppressLint("DefaultLocale")
    private fun showStartDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedMonth = String.format("%02d", selectedMonth + 1)
                val formattedDay = String.format("%02d", selectedDay)
                val selectedDate = "$selectedYear-$formattedMonth-$formattedDay"
                edtStartDate.setText(selectedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

    @SuppressLint("DefaultLocale")
    private fun showEndDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedMonth = String.format("%02d", selectedMonth + 1)
                val formattedDay = String.format("%02d", selectedDay)
                val selectedDate = "$selectedYear-$formattedMonth-$formattedDay"
                edtEndDate.setText(selectedDate)
            },
            year,
            month,
            day
        )
        datePickerDialog.show()
    }

 // return to home

}