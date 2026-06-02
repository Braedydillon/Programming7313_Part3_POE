package com.example.tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class BudgetFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_budget, container, false)

        val etGoal = view.findViewById<EditText>(R.id.et_monthly_goal)
        val btnSave = view.findViewById<Button>(R.id.btn_save_budget)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val dbRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("settings")

        // Load existing goal
        dbRef.child("monthlyGoal").get().addOnSuccessListener { snapshot ->
            if (!isAdded) return@addOnSuccessListener
            val currentGoal = snapshot.getValue(Double::class.java)
            if (currentGoal != null) {
                etGoal.setText(currentGoal.toString())
            }
        }

        btnSave.setOnClickListener {
            val goalText = etGoal.text.toString()
            if (goalText.isNotEmpty()) {
                val goal = goalText.toDouble()
                dbRef.child("monthlyGoal").setValue(goal)
                    .addOnSuccessListener {
                        if (isAdded) {
                            Toast.makeText(context, "Budget updated!", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        if (isAdded) {
                            Toast.makeText(context, "Failed to update budget", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        return view
    }
}
