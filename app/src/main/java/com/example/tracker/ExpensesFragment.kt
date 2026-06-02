package com.example.tracker

import Data.Expense
import Data.MonthlyGoal
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class ExpensesFragment : Fragment() {

    private lateinit var edtCategory: EditText
    private lateinit var edtAmount: EditText
    private lateinit var edtDate: EditText
    private lateinit var edtDescription: EditText
    private lateinit var btnPhoto: Button
    private lateinit var btnSave: Button
    private lateinit var edtMinimumgoal: EditText
    private lateinit var edtMaximumgoal: EditText
    private lateinit var btnSave2: Button
    private lateinit var imgPreview: ImageView

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private var selectedPhotoUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            imgPreview.visibility = View.VISIBLE
            Glide.with(this).load(uri).into(imgPreview)
            Toast.makeText(context, "Image selected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_expenses, container, false)

        edtCategory = view.findViewById(R.id.edtCategory)
        edtAmount = view.findViewById(R.id.edtAmount)
        edtDate = view.findViewById(R.id.edtDate)
        edtDescription = view.findViewById(R.id.edtDescription)
        btnPhoto = view.findViewById(R.id.btnPhoto)
        btnSave = view.findViewById(R.id.btnSave)
        edtMinimumgoal = view.findViewById(R.id.edtMinimumgoal)
        edtMaximumgoal = view.findViewById(R.id.edtMaximumgoal)
        btnSave2 = view.findViewById(R.id.btnSave2)
        imgPreview = view.findViewById(R.id.imgPreview)

        auth = FirebaseAuth.getInstance()
        // Reverting to default instance to use bucket from google-services.json
        storage = FirebaseStorage.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance().getReference("users").child(userId)

        edtDate.setOnClickListener { showDatePicker() }
        btnPhoto.setOnClickListener { imagePickerLauncher.launch("image/*") }
        btnSave.setOnClickListener { saveExpense() }
        btnSave2.setOnClickListener { saveGoals() }

        return view
    }

    private fun saveExpense() {
        val category = edtCategory.text.toString().trim()
        val amountText = edtAmount.text.toString().trim()
        val date = edtDate.text.toString().trim()
        val description = edtDescription.text.toString().trim()

        if (category.isEmpty() || amountText.isEmpty() || date.isEmpty() || description.isEmpty()) {
            Toast.makeText(context, "Please fill in all the required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull() ?: return
        val key = database.child("expenses").push().key ?: return

        if (selectedPhotoUri != null) {
            uploadImageAndSaveExpense(key, category, amount, date, description)
        } else {
            val expense = Expense(
                category = category,
                amount = amount,
                date = date,
                description = description,
                photoUri = null
            )
            saveExpenseToDatabase(key, expense)
        }
    }

    private fun uploadImageAndSaveExpense(
        key: String,
        category: String,
        amount: Double,
        date: String,
        description: String
    ) {
        val userId = auth.currentUser?.uid ?: return
        val storageRef = storage.reference.child("users/$userId/expenses/$key.jpg")

        selectedPhotoUri?.let { uri ->
            storageRef.putFile(uri).continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { throw it }
                }
                storageRef.downloadUrl
            }.addOnSuccessListener { downloadUri ->
                if (!isAdded) return@addOnSuccessListener
                val expense = Expense(
                    category = category,
                    amount = amount,
                    date = date,
                    description = description,
                    photoUri = downloadUri.toString()
                )
                saveExpenseToDatabase(key, expense)
            }.addOnFailureListener { e ->
                if (isAdded) {
                    Log.e("ExpensesFragment", "Upload failed", e)
                    Toast.makeText(context, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun saveExpenseToDatabase(key: String, expense: Expense) {
        database.child("expenses").child(key).setValue(expense)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Expense saved successfully", Toast.LENGTH_SHORT).show()
                    clearExpenseFields()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun clearExpenseFields() {
        edtCategory.text.clear()
        edtAmount.text.clear()
        edtDate.text.clear()
        edtDescription.text.clear()
        selectedPhotoUri = null
        imgPreview.visibility = View.GONE
    }

    private fun saveGoals() {
        val minGoal = edtMinimumgoal.text.toString().toDoubleOrNull()
        val maxGoal = edtMaximumgoal.text.toString().toDoubleOrNull()

        if (minGoal == null || maxGoal == null) {
            Toast.makeText(context, "Please enter valid goals", Toast.LENGTH_SHORT).show()
            return
        }

        val goal = MonthlyGoal(minGoal = minGoal, maxGoal = maxGoal)
        database.child("goals").setValue(goal)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Goals saved successfully", Toast.LENGTH_SHORT).show()
                    // Also update the monthlyGoal for HomeFragment balance calculation
                    database.child("settings").child("monthlyGoal").setValue(maxGoal)
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Error saving goals: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = String.format(Locale.getDefault(), "%d-%02d-%02d", year, month + 1, day)
                edtDate.setText(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
}
