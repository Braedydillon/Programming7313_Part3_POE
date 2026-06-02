package com.example.tracker

import Data.Expense
import Data.MonthlyGoal
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale

class Expenses : AppCompatActivity() {

    private lateinit var edtCategory: EditText
    private lateinit var edtAmount: EditText
    private lateinit var edtDate: EditText
    private lateinit var edtDescription: EditText
    private lateinit var btnPhoto: Button
    private lateinit var btnSave: Button
    private lateinit var edtMinimumgoal: EditText
    private lateinit var edtMaximumgoal: EditText
    private lateinit var btnSave2: Button

    private val databaseInstance by lazy { FirebaseDatabase.getInstance() }
    private val databaseRef by lazy { databaseInstance.getReference("users") }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var selectedPhotoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_expenses)

        edtCategory = findViewById(R.id.edtCategory)
        edtAmount = findViewById(R.id.edtAmount)
        edtDate = findViewById(R.id.edtDate)
        edtDescription = findViewById(R.id.edtDescription)
        btnPhoto = findViewById(R.id.btnPhoto)
        btnSave = findViewById(R.id.btnSave)
        edtMinimumgoal = findViewById(R.id.edtMinimumgoal)
        edtMaximumgoal = findViewById(R.id.edtMaximumgoal)
        btnSave2 = findViewById(R.id.btnSave2)

        edtDate.setOnClickListener { showDatePicker() }
        btnPhoto.setOnClickListener { imagePickerLauncher.launch(arrayOf("image/*")) }
        btnSave.setOnClickListener { saveExpense() }
        btnSave2.setOnClickListener { saveGoals() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            selectedPhotoUri = uri
            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveExpense() {
        val category = edtCategory.text.toString().trim()
        val amountText = edtAmount.text.toString().trim()
        val date = edtDate.text.toString().trim()
        val description = edtDescription.text.toString().trim()

        if (category.isEmpty() || amountText.isEmpty() || date.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull() ?: return
        val userId = auth.currentUser?.uid

        if (userId.isNullOrEmpty()) {
            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
            return
        }

        val key = databaseRef.child(userId).child("expenses").push().key ?: return

        val base64ImageString = selectedPhotoUri?.let { convertUriToBase64(it) }

        val expense = Expense(
            category = category,
            amount = amount,
            date = date,
            description = description,
            photoUri = base64ImageString
        )

        databaseRef.child(userId).child("expenses").child(key).setValue(expense)
            .addOnSuccessListener {
                Toast.makeText(this, "Expense synced globally!", Toast.LENGTH_SHORT).show()
                edtCategory.text.clear()
                edtAmount.text.clear()
                edtDate.text.clear()
                edtDescription.text.clear()
                selectedPhotoUri = null
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Database write error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun convertUriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 500, 500, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveGoals() {
        val minText = edtMinimumgoal.text.toString().trim()
        val maxText = edtMaximumgoal.text.toString().trim()

        val minGoal = minText.toDoubleOrNull()
        val maxGoal = maxText.toDoubleOrNull()

        if (minGoal == null || maxGoal == null || minGoal > maxGoal) {
            Toast.makeText(this, "Please enter valid goal parameters", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        val goal = MonthlyGoal(minGoal = minGoal, maxGoal = maxGoal)

        databaseRef.child(userId).child("goals").setValue(goal)
            .addOnSuccessListener {
                Toast.makeText(this, "Goal updated successfully!", Toast.LENGTH_SHORT).show()
                edtMinimumgoal.text.clear()
                edtMaximumgoal.text.clear()
            }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val formattedMonth = String.format(Locale.getDefault(), "%02d", selectedMonth + 1)
                val formattedDay = String.format(Locale.getDefault(), "%02d", selectedDay)
                edtDate.setText("$selectedYear-$formattedMonth-$formattedDay")
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}