package com.example.tracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment

class MoreFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_more, container, false)

        view.findViewById<Button>(R.id.btn_budget).setOnClickListener {
            (activity as? Home)?.replaceFragment(BudgetFragment())
        }

        view.findViewById<Button>(R.id.btn_interest).setOnClickListener {
            (activity as? Home)?.replaceFragment(InterestFragment())
        }

        view.findViewById<Button>(R.id.btn_profile).setOnClickListener {
            (activity as? Home)?.replaceFragment(ProfileFragment())
        }

        return view
    }
}