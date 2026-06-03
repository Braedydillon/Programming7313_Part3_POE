package com.example.tracker

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class Home : AppCompatActivity() {

    private lateinit var navItems: List<Pair<LinearLayout, Fragment>>
    private var currentActiveIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        setupCustomBottomNav()

        findViewById<View>(R.id.btn_top_profile).setOnClickListener {
            replaceFragment(ProfileFragment())
        }

        if (savedInstanceState == null) {
            setActiveTab(0) // Default to Home
        }
    }

    private fun setupCustomBottomNav() {
        navItems = listOf(
            findViewById<LinearLayout>(R.id.btn_nav_home) to HomeFragment(),
            findViewById<LinearLayout>(R.id.btn_nav_expenses) to ExpensesFragment(),
            findViewById<LinearLayout>(R.id.btn_nav_scanner) to AutoScannerFragment(),
            findViewById<LinearLayout>(R.id.btn_nav_budget) to BudgetFragment(),
            findViewById<LinearLayout>(R.id.btn_nav_interest) to InterestFragment(),
            findViewById<LinearLayout>(R.id.btn_nav_report) to ReportFragment(),
            findViewById<LinearLayout>(R.id.btn_nav_profile) to ProfileFragment()
        )

        navItems.forEachIndexed { index, pair ->
            pair.first.setOnClickListener {
                setActiveTab(index)
            }
        }
    }

    private fun setActiveTab(index: Int) {
        if (currentActiveIndex == index) return
        currentActiveIndex = index

        navItems.forEachIndexed { i, pair ->
            val isSelected = i == index
            val color = if (isSelected) R.color.bg_blue else R.color.card_dark
            
            // Update UI state
            val container = pair.first
            val icon = container.getChildAt(0) as ImageView
            val text = container.getChildAt(1) as TextView
            
            icon.setColorFilter(ContextCompat.getColor(this, color))
            text.setTextColor(ContextCompat.getColor(this, color))
            
            if (isSelected) {
                replaceFragment(pair.second, false)
            }
        }
    }

    fun replaceFragment(fragment: Fragment, updateNav: Boolean = true) {
        if (updateNav) {
            val index = when (fragment) {
                is HomeFragment -> 0
                is ExpensesFragment -> 1
                is AutoScannerFragment -> 2
                is BudgetFragment -> 3
                is InterestFragment -> 4
                is ReportFragment -> 5
                is ProfileFragment -> 6
                else -> -1
            }
            if (index != -1) {
                setActiveTab(index)
                return // setActiveTab calls replaceFragment(..., false)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}