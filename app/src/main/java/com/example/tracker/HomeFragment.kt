package com.example.tracker

import Data.Expense
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Base64 // ADDED
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var tvBalance: TextView
    private lateinit var tvGoalStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var pieChart: PieChart
    private lateinit var recentActivityContainer: LinearLayout
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        tvBalance = view.findViewById(R.id.tv_balance)
        tvGoalStatus = view.findViewById(R.id.tv_goal_status)
        progressBar = view.findViewById(R.id.goal_progress_bar)
        pieChart = view.findViewById(R.id.pieChart)
        recentActivityContainer = view.findViewById(R.id.recent_activity_container)

        view.findViewById<View>(R.id.card_pie_chart).setOnClickListener {
            (activity as? Home)?.replaceFragment(ReportFragment())
        }

        view.findViewById<View>(R.id.btn_quick_add).setOnClickListener {
            (activity as? Home)?.replaceFragment(ExpensesFragment())
        }

        view.findViewById<View>(R.id.btn_quick_goal).setOnClickListener {
            (activity as? Home)?.replaceFragment(BudgetFragment())
        }

        auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        database = FirebaseDatabase.getInstance().getReference("users").child(userId)

        setupPieChart()
        loadDashboardData()

        return view
    }

    private fun setupPieChart() {
        pieChart.description.isEnabled = false
        pieChart.isDrawHoleEnabled = true
        pieChart.setHoleColor(Color.TRANSPARENT)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.legend.isEnabled = false
    }

    private fun loadDashboardData() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                val monthlyGoal = snapshot.child("settings").child("monthlyGoal").getValue(Double::class.java) ?: 0.0

                val expenses = mutableListOf<Expense>()
                snapshot.child("expenses").children.forEach {
                    it.getValue(Expense::class.java)?.let { exp -> expenses.add(exp) }
                }

                updateDashboardUI(monthlyGoal, expenses)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateDashboardUI(monthlyGoal: Double, expenses: List<Expense>) {
        val totalExpenses = expenses.sumOf { it.amount }
        val balance = monthlyGoal - totalExpenses

        tvBalance.text = String.format(Locale.getDefault(), "R%.2f", balance)

        if (monthlyGoal > 0) {
            val progress = ((totalExpenses / monthlyGoal) * 100).toInt()
            progressBar.progress = progress.coerceAtMost(100)
            tvGoalStatus.text = String.format(Locale.getDefault(), "R%.2f / R%.2f", totalExpenses, monthlyGoal)
        } else {
            progressBar.progress = 0
            tvGoalStatus.text = "R0 / R0"
        }

        // Update Recent Activity
        recentActivityContainer.removeAllViews()
        val recentExpenses = expenses.sortedByDescending { it.date }.take(3)
        if (recentExpenses.isEmpty()) {
            val noData = TextView(context).apply {
                text = "No recent transactions"
                alpha = 0.5f
                setPadding(0, 20, 0, 0)
            }
            recentActivityContainer.addView(noData)
        } else {
            for (expense in recentExpenses) {
                addRecentItem(expense)
            }
        }

        // Update Pie Chart
        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val entries = categoryTotals.map { PieEntry(it.value.toFloat(), it.key) }
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.BLACK

        pieChart.data = PieData(dataSet)
        pieChart.invalidate()
    }

    private fun addRecentItem(expense: Expense) {
        val context = context ?: return
        val inflater = LayoutInflater.from(context)
        val itemView = inflater.inflate(R.layout.item_expense_report, recentActivityContainer, false)
        itemView.findViewById<TextView>(R.id.txtDate).text = expense.date
        itemView.findViewById<TextView>(R.id.txtCategory).text = expense.category
        itemView.findViewById<TextView>(R.id.txtDescription).text =expense.description
        itemView.findViewById<TextView>(R.id.txtAmount).text = String.format(Locale.getDefault(), "R%.2f", expense.amount)

        val img = itemView.findViewById<ImageView>(R.id.imgExpense)

        // Robust decoding for dashboard recent activity
        if (!expense.photoUri.isNullOrEmpty()) {
            img.visibility = View.VISIBLE
            try {
                val heightInPx = (120 * resources.displayMetrics.density).toInt()
                img.layoutParams.height = heightInPx

                val base64String = expense.photoUri.trim()
                val pureBase64 = if (base64String.contains(",")) base64String.substringAfter(",") else base64String
                val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)

                Glide.with(this)
                    .asBitmap()
                    .load(imageBytes)
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(img)
            } catch (e: Exception) {
                android.util.Log.e("HomeFragment", "Dashboard image decode failed", e)
                img.visibility = View.GONE
            }
        } else {
            img.visibility = View.GONE
        }

        recentActivityContainer.addView(itemView)
    }
}