package com.example.tracker

import Data.database.AppDatabase
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import Data.User
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class Register : AppCompatActivity() {

    //global declarations
    //These variables are declared globally so we can use them in multiple functions

    private lateinit var editTextText2: EditText
    private lateinit var editTextTextPassword2: EditText
    private lateinit var editTextTextPassword3: EditText
    private lateinit var button2: Button
    private lateinit var button3: Button
    private lateinit var db: AppDatabase

    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        //typecasting (connecting the document to kotlin)
        editTextText2 = findViewById(R.id.editTextText2)
        editTextTextPassword2 = findViewById(R.id.editTextTextPassword2)
        editTextTextPassword3 = findViewById(R.id.editTextTextPassword3)
        button2 = findViewById(R.id.button2)
        button3 = findViewById(R.id.button3)

        auth = FirebaseAuth.getInstance()


        //button click event
        //When the user clickers the register button, it will activate the code within

        button2.setOnClickListener {
            registerUser() //calling function to handle register
        }

        //Button for when users already have an account
        button3.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java)) //user will be directed to the login screen
        }



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    //function to handle the logic when a user registers
    private fun registerUser() {
        // 1. Get text from input fields and remove extra spaces
        val username = editTextText2.text.toString().trim()
        val password = editTextTextPassword2.text.toString().trim()
        val confirmPassword = editTextTextPassword3.text.toString().trim()

        // 2. Validation Checks
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all the fields", Toast.LENGTH_SHORT).show()
            return // stops the function if validation fails
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return // stops the function if passwords don't match
        }

        // 3. Firebase Authentication operation
        // Note: Firebase runs asynchronously natively, so you no longer need lifecycleScope.launch
        auth.createUserWithEmailAndPassword(username, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Successfully registered", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()

                } else {
                    // Check if the error is due to a duplicate email/username
                    if (task.exception is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                        editTextText2.error = "This email is already registered. Try logging in!"
                    } else {
                        val errorMessage = task.exception?.message ?: "Unknown error"
                        Toast.makeText(
                            this,
                            "Registration failed: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
    }
}