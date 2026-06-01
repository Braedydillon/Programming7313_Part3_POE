package com.example.tracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth // 1. Added Firebase Import

class MainActivity : AppCompatActivity() {

    // Global declarations
    private lateinit var editTextText: EditText
    private lateinit var editTextTextPassword: EditText
    private lateinit var button: Button
    private lateinit var textView: Button

    // 2. Swapped Room Database for Firebase Auth
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)// Ensure this layout matches your activity_main setup

        // 3. Initialize Firebase Auth instance
        auth = FirebaseAuth.getInstance()

        editTextText = findViewById(R.id.editTextText)
        editTextTextPassword = findViewById(R.id.editTextTextPassword)
        button = findViewById(R.id.button)
        textView = findViewById(R.id.textView)

        button.setOnClickListener {
            loginUser()
        }

        textView.setOnClickListener {
            // Switches over to your register view
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loginUser() {
        val username = editTextText.text.toString().trim()
        val password = editTextTextPassword.text.toString().trim()

        // Validation checks
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both username and password", Toast.LENGTH_SHORT).show()
            return
        }

        // 4. Firebase Authentication handles background threads natively
        auth.signInWithEmailAndPassword(username, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Login successful!
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()

                    // Route directly to your home screen
                    openHomePage(username)
                } else {
                    // Login failed. Displays the exact message from Firebase (e.g. wrong password)
                    val errorMessage = task.exception?.message ?: "Invalid username or password."
                    Toast.makeText(this, "Login Failed: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun openHomePage(username: String) {
        // Point this to whatever Activity acts as your main chart dashboard workspace
        val intent = Intent(this, Home::class.java)
        intent.putExtra("username", username)
        startActivity(intent)
        finish() // Closes this login screen so pressing 'back' doesn't log them back out
    }
}