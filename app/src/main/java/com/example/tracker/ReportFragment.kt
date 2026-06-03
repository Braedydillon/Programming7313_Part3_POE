package com.example.tracker

import Data.Expense
import Data.MonthlyGoal
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Base64 // ADDED
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class ReportFragment : Fragment() {
    private lateinit var edtStartDate: EditText
    private lateinit var edtEndDate: EditText
    private lateinit var edtSearchName: EditText
    private lateinit var btnFilter: Button
    private lateinit var txtTotal: TextView
    private lateinit var expensesContainer: LinearLayout
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_report, container, false)

        edtStartDate = view.findViewById(R.id.startdate)
        edtEndDate = view.findViewById(R.id.enddate)
        edtSearchName = view.findViewById(R.id.searchname)
        btnFilter = view.findViewById(R.id.filter)
        txtTotal = view.findViewById(R.id.Total)
        expensesContainer = view.findViewById(R.id.expensesContainer)

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance().getReference("users").child(userId).child("expenses")

        edtStartDate.setOnClickListener { showDatePicker(edtStartDate) }
        edtEndDate.setOnClickListener { showDatePicker(edtEndDate) }
        btnFilter.setOnClickListener { filterExpenses() }

        // Initial load
        filterExpenses()

        return view
    }

    private fun filterExpenses() {
        val startDate = edtStartDate.text.toString()
        val endDate = edtEndDate.text.toString()
        val searchName = edtSearchName.text.toString().trim().lowercase(Locale.getDefault())

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                expensesContainer.removeAllViews()
                var totalAmount = 0.0

                if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                    val emptyView = TextView(context).apply {
                        text = "No expenses found."
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                        setPadding(0, 50, 0, 0)
                    }
                    expensesContainer.addView(emptyView)
                }

                for (dataSnapshot in snapshot.children) {
                    val item = dataSnapshot.getValue(Expense::class.java)
                    if (item != null) {
                        val matchesDate = if (startDate.isNotEmpty() && endDate.isNotEmpty()) {
                            item.date in startDate..endDate
                        } else true

                        val matchesName = if (searchName.isNotEmpty()) {
                            item.category.lowercase().contains(searchName) ||
                                    item.description.lowercase().contains(searchName)
                        } else true

                        if (matchesDate && matchesName) {
                            totalAmount += item.amount
                            addExpenseCard(item)
                        }
                    }
                }
                txtTotal.text = String.format(Locale.getDefault(), "Total: R%.2f", totalAmount)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addExpenseCard(expense: Expense) {
        val context = context ?: return
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.item_expense_report, expensesContainer, false)

        val txtDate = itemView.findViewById<TextView>(R.id.txtDate)
        val txtCategory = itemView.findViewById<TextView>(R.id.txtCategory)
        val txtDescription = itemView.findViewById<TextView>(R.id.txtDescription)
        val txtAmount = itemView.findViewById<TextView>(R.id.txtAmount)
        val imgExpense = itemView.findViewById<ImageView>(R.id.imgExpense)

        txtDate.text = expense.date
        txtCategory.text = expense.category
        txtDescription.text = expense.description
        txtAmount.text = String.format(Locale.getDefault(), "R%.2f", expense.amount)

        // Robust decoding and rendering logic for expense images
        if (!expense.photoUri.isNullOrEmpty()) {
            imgExpense.visibility = View.VISIBLE
            try {
                val base64String = expense.photoUri.trim()
                // Ensure we handle potential data URIs or whitespace gracefully
                val pureBase64 = if (base64String.contains(",")) base64String.substringAfter(",") else base64String
                val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)

                Glide.with(this)
                    .asBitmap()
                    .load(imageBytes)
                    .error(android.R.drawable.ic_menu_report_image) // Fallback if Glide fails
                    .into(imgExpense)
            } catch (e: Exception) {
                android.util.Log.e("ReportFragment", "Failed to decode image Base64", e)
                imgExpense.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            imgExpense.visibility = View.GONE
        }

        expensesContainer.addView(itemView)
    }

    private fun showDatePicker(editText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            editText.setText(String.format(Locale.getDefault(), "%d-%02d-%02d", y, m + 1, d))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }
}