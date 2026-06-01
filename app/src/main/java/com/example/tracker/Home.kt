package com.example.tracker

import Data.Expense
import Data.MonthlyGoal
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class Home : AppCompatActivity() {

    private lateinit var btnExpenses: Button
    private lateinit var btnReports: Button
    private lateinit var btnLogout: Button
    private lateinit var tvWelcomeUser: TextView

    // Chart components setup
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart

    // Global variables to hold goals fetched from Firebase
    private var currentMinGoal: Double = 0.0
    private var currentMaxGoal: Double = 0.0

    // Database path references
    private val databaseInstance by lazy { FirebaseDatabase.getInstance() }
    private val expensesRef by lazy { databaseInstance.getReference("expenses") }
    private val goalsRef by lazy { databaseInstance.getReference("monthlyGoals") }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        btnExpenses = findViewById(R.id.btnExpenses)
        btnReports = findViewById(R.id.btnReports)
        btnLogout = findViewById(R.id.btnLogout)
        tvWelcomeUser = findViewById(R.id.textView5) // Links to your welcome text view

        pieChart = findViewById(R.id.pieChart)
        barChart = findViewById(R.id.barChart)

        // Set username if passed from login screen
        val username = intent.getStringExtra("username") ?: "User"
        tvWelcomeUser.text = "Welcome, $username"

        btnExpenses.setOnClickListener {
            startActivity(Intent(this, Expenses::class.java))
        }

        btnReports.setOnClickListener {
            startActivity(Intent(this, Report::class.java))
        }

        btnLogout.setOnClickListener {
            Toast.makeText(this, "Logging you out", Toast.LENGTH_SHORT).show()
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Fetch the goal values FIRST, then load chart data
        loadGoalsAndChartData()
    }

    private fun loadGoalsAndChartData() {
        // Fetch Goals
        goalsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(goalSnapshot: DataSnapshot) {
                val goalData = goalSnapshot.getValue(MonthlyGoal::class.java)
                if (goalData != null) {
                    currentMinGoal = goalData.minGoal
                    currentMaxGoal = goalData.maxGoal
                }

                // 2. Load the expense data after goals are loaded so chart can draw lines correctly
                loadCloudChartData()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FIREBASE_GOAL", "Failed to load goals: ${error.message}")
            }
        })
    }

    private fun loadCloudChartData() {
        expensesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val pieEntries = ArrayList<PieEntry>()
                val barEntries = ArrayList<BarEntry>()
                val labels = ArrayList<String>()
                var index = 0f

                for (dataSnapshot in snapshot.children) {
                    val item = dataSnapshot.getValue(Expense::class.java)
                    if (item != null) {
                        val numericalValue = item.amount.toFloat()

                        pieEntries.add(PieEntry(numericalValue, item.category))
                        barEntries.add(BarEntry(index, numericalValue))
                        labels.add(item.category)
                        index++
                    }
                }
                setupPieChart(pieEntries)
                setupBarChart(barEntries, labels)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Home, "Cloud Sync Failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupPieChart(entries: List<PieEntry>) {
        val dataSet = PieDataSet(entries, "Expense Categories")
        dataSet.colors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.YELLOW)
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.BLACK

        pieChart.data = PieData(dataSet)
        pieChart.description.isEnabled = false
        pieChart.centerText = "Expenses Breakdown"
        pieChart.setUsePercentValues(true)
        pieChart.animateY(1000)
        pieChart.invalidate()
    }

    @SuppressLint("UseKtx")
    private fun setupBarChart(entries: List<BarEntry>, labels: List<String>) {
        val dataSet = BarDataSet(entries, "Amount (R)")
        dataSet.color = Color.parseColor("#4285F4")
        dataSet.valueTextSize = 12f

        val data = BarData(dataSet)
        data.barWidth = 0.6f

        barChart.data = data
        barChart.setFitBars(true)
        barChart.description.isEnabled = false
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.granularity = 1f
        barChart.xAxis.setDrawLabels(true)

        // --- NEW POE COMPLIANCE REQUIREMENT: GOAL LIMIT LINES ---
        // Clear any old lines from previous data changes to prevent duplicates
        barChart.axisLeft.removeAllLimitLines()

        // Only draw lines if goals have been configured (greater than 0)
        if (currentMinGoal > 0) {
            val minLine = LimitLine(currentMinGoal.toFloat(), "Minimum Goal").apply {
                lineWidth = 2f
                enableDashedLine(10f, 10f, 0f)
                lineColor = Color.GREEN
                textColor = Color.BLACK
                textSize = 10f
            }
            barChart.axisLeft.addLimitLine(minLine)
        }

        if (currentMaxGoal > 0) {
            val maxLine = LimitLine(currentMaxGoal.toFloat(), "Maximum Limit").apply {
                lineWidth = 2f
                enableDashedLine(10f, 10f, 0f)
                lineColor = Color.RED
                textColor = Color.BLACK
                textSize = 10f
            }
            barChart.axisLeft.addLimitLine(maxLine)
        }


        barChart.animateY(1000)
        barChart.invalidate()
    }
}