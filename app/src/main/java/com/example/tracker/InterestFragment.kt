package com.example.tracker

import Data.InterestAccount
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class InterestFragment : Fragment() {

    private lateinit var adapter: InterestAdapter
    private val accountList = mutableListOf<InterestAccount>()
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_interest, container, false)

        val etName = view.findViewById<EditText>(R.id.et_account_name)
        val etPrincipal = view.findViewById<EditText>(R.id.et_principal)
        val etRate = view.findViewById<EditText>(R.id.et_rate)
        val btnSave = view.findViewById<Button>(R.id.btn_save_interest)
        val rvAccounts = view.findViewById<RecyclerView>(R.id.rv_interest_accounts)

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance().getReference("users").child(userId).child("interest_accounts")

        // Setup RecyclerView
        adapter = InterestAdapter(accountList)
        rvAccounts.layoutManager = LinearLayoutManager(context)
        rvAccounts.adapter = adapter

        btnSave.setOnClickListener {
            val name = etName.text.toString()
            val principal = etPrincipal.text.toString().toDoubleOrNull() ?: 0.0
            val rate = etRate.text.toString().toDoubleOrNull() ?: 0.0

            if (name.isNotEmpty()) {
                val accountId = database.push().key ?: ""
                val account = InterestAccount(accountId, name, principal, rate)
                database.child(accountId).setValue(account)
                    .addOnSuccessListener {
                        if (isAdded) {
                            Toast.makeText(context, "Account saved!", Toast.LENGTH_SHORT).show()
                            etName.text.clear()
                            etPrincipal.text.clear()
                            etRate.text.clear()
                        }
                    }
            } else {
                Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
            }
        }

        loadInterestAccounts()

        return view
    }

    private fun loadInterestAccounts() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                accountList.clear()
                for (child in snapshot.children) {
                    child.getValue(InterestAccount::class.java)?.let {
                        accountList.add(it)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                if (isAdded) {
                    Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
