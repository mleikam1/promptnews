package com.digitalturbine.promptnews.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.digitalturbine.promptnews.MainActivity
import com.digitalturbine.promptnews.data.Interest
import com.digitalturbine.promptnews.data.InterestCatalog
import com.digitalturbine.promptnews.data.UserInterestRepositoryImpl
import com.digitalturbine.promptnews.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = UserInterestRepositoryImpl.getInstance(this)
        if (repo.isOnboardingComplete()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.onboarding_activity)

        val rootView = findViewById<ViewGroup>(R.id.onboardingRoot)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val interests = InterestCatalog.interests
        val selectedIds = mutableSetOf<String>()
        val selectedCountView = findViewById<TextView>(R.id.selectedCount)
        val continueButton = findViewById<MaterialButton>(R.id.continueButton)
        val interestsList = findViewById<RecyclerView>(R.id.interestsList)
        val adapter = InterestAdapter(
            items = interests,
            selectedIds = selectedIds
        ) { id, selected ->
            if (selected) {
                selectedIds.add(id)
            } else {
                selectedIds.remove(id)
            }
            updateSelectionCount(selectedIds, selectedCountView, continueButton)
        }

        interestsList.layoutManager = GridLayoutManager(this, 2)
        interestsList.adapter = adapter

        continueButton.setOnClickListener {
            val selected = interests.filter { selectedIds.contains(it.id) }
            repo.saveSelectedInterests(selected)
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        updateSelectionCount(selectedIds, selectedCountView, continueButton)
    }

    private fun updateSelectionCount(
        selectedIds: Set<String>,
        selectedCountView: TextView,
        continueButton: MaterialButton
    ) {
        val count = selectedIds.size
        selectedCountView.text = "$count selected"
        continueButton.isEnabled = count >= 3
    }
}

private class InterestAdapter(
    private val items: List<Interest>,
    private val selectedIds: Set<String>,
    private val onSelectionChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<InterestAdapter.InterestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        val chip = Chip(parent.context).apply {
            isCheckable = true
            isClickable = true
            isFocusable = true
            layoutParams = ViewGroup.MarginLayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                val margin = (8 * resources.displayMetrics.density).toInt()
                setMargins(margin, margin, margin, margin)
            }
        }
        return InterestViewHolder(chip)
    }

    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        val interest = items[position]
        holder.bind(interest, selectedIds.contains(interest.id), onSelectionChanged)
    }

    override fun getItemCount(): Int = items.size

    class InterestViewHolder(private val chip: Chip) : RecyclerView.ViewHolder(chip) {
        fun bind(
            interest: Interest,
            isSelected: Boolean,
            onSelectionChanged: (String, Boolean) -> Unit
        ) {
            chip.text = interest.displayName
            chip.isChecked = isSelected
            chip.setOnCheckedChangeListener(null)
            chip.isChecked = isSelected
            chip.setOnCheckedChangeListener { _: CompoundButton, checked: Boolean ->
                onSelectionChanged(interest.id, checked)
            }
        }
    }
}
