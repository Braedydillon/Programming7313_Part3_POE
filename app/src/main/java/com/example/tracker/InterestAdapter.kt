package com.example.tracker

import Data.InterestAccount
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class InterestAdapter(private val accounts: List<InterestAccount>) :
    RecyclerView.Adapter<InterestAdapter.InterestViewHolder>() {

    class InterestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tv_acc_name)
        val tvPrincipal: TextView = view.findViewById(R.id.tv_acc_principal)
        val tvRate: TextView = view.findViewById(R.id.tv_acc_rate)
        val tvInterest: TextView = view.findViewById(R.id.tv_acc_interest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_interest_account, parent, false)
        return InterestViewHolder(view)
    }

    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        val account = accounts[position]
        holder.tvName.text = account.name
        holder.tvPrincipal.text = String.format(Locale.getDefault(), "Principal: R%.2f", account.principal)
        holder.tvRate.text = String.format(Locale.getDefault(), "Rate: %.1f%%", account.rate)
        
        val annualInterest = account.principal * (account.rate / 100)
        holder.tvInterest.text = String.format(Locale.getDefault(), "Annual Interest: R%.2f", annualInterest)
    }

    override fun getItemCount() = accounts.size
}