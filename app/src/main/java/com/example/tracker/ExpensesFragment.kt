package com.example.tracker

import Data.Expense
import Data.MonthlyGoal
import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
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
import java.io.ByteArrayOutputStream
import java.io.InputStream
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
        database = FirebaseDatabase.getInstance().getReference("users")

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
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountText.toDoubleOrNull() ?: return
        val userId = auth.currentUser?.uid

        if (userId.isNullOrEmpty()) {
            Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
            return
        }

        val key = database.child(userId).child("expenses").push().key ?: return

        // Convert the image to a global Base64 string if an image is selected
        val base64ImageString = selectedPhotoUri?.let { convertUriToBase64(it) }

        val expense = Expense(
            category = category,
            amount = amount,
            date = date,
            description = description,
            photoUri = base64ImageString // This now holds the actual cloud-syncable image string!
        )

        database.child(userId).child("expenses").child(key).setValue(expense)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Expense synced globally!", Toast.LENGTH_SHORT).show()
                    clearExpenseFields()
                }
            }
            .addOnFailureListener { e ->
                if (isAdded) {
                    Toast.makeText(context, "Database error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Helper method to downscale, compress, and encode the binary file to string
    private fun convertUriToBase64(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context?.contentResolver?.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)

            // Downscale to maximum 500x500 pixels to protect database performance thresholds
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 500, 500, true)

            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val byteArray = outputStream.toByteArray()

            Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
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

        val userId = auth.currentUser?.uid ?: return
        val goal = MonthlyGoal(minGoal = minGoal, maxGoal = maxGoal)

        database.child(userId).child("goals").setValue(goal)
            .addOnSuccessListener {
                if (isAdded) {
                    Toast.makeText(context, "Goals updated successfully!", Toast.LENGTH_SHORT).show()
                    database.child(userId).child("settings").child("monthlyGoal").setValue(maxGoal)
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