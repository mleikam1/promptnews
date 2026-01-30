package com.digitalturbine.promptnews.ui.onboarding

import android.Manifest
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.digitalturbine.promptnews.MainActivity
import com.digitalturbine.promptnews.data.Interest
import com.digitalturbine.promptnews.data.InterestCatalog
import com.digitalturbine.promptnews.data.UserInterestRepositoryImpl
import com.digitalturbine.promptnews.R
import com.digitalturbine.promptnews.data.UserLocation
import com.digitalturbine.promptnews.util.HomePrefs
import com.google.android.material.button.MaterialButton
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class OnboardingActivity : ComponentActivity() {
    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        HomePrefs.setLocationPrompted(this, true)
        if (granted) {
            resolveAndStoreLocation()
        } else {
            HomePrefs.setUserLocation(this, null)
        }
        showInterestSelection()
    }

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

        val locationContainer = findViewById<ViewGroup>(R.id.locationPromptContainer)
        val interestsContainer = findViewById<ViewGroup>(R.id.interestsContainer)
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.onboardingToolbar)
        val selectionBar = findViewById<ViewGroup>(R.id.selectionBar)
        val enableLocationButton = findViewById<MaterialButton>(R.id.enableLocationButton)

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
            repo.saveSelectedInterests(selected.toSet())
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        updateSelectionCount(selectedIds, selectedCountView, continueButton)

        enableLocationButton.setOnClickListener {
            requestLocationPermission()
        }
        if (HomePrefs.wasLocationPrompted(this) || hasLocationPermission()) {
            HomePrefs.setLocationPrompted(this, true)
            if (hasLocationPermission()) {
                resolveAndStoreLocation()
            }
            showInterestSelection()
        } else {
            toolbar.title = getString(R.string.onboarding_location_title)
            locationContainer.isVisible = true
            interestsContainer.isVisible = false
            selectionBar.isVisible = false
        }
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

    private fun showInterestSelection() {
        val locationContainer = findViewById<ViewGroup>(R.id.locationPromptContainer)
        val interestsContainer = findViewById<ViewGroup>(R.id.interestsContainer)
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.onboardingToolbar)
        val selectionBar = findViewById<ViewGroup>(R.id.selectionBar)
        toolbar.title = getString(R.string.onboarding_interest_title)
        locationContainer.isVisible = false
        interestsContainer.isVisible = true
        selectionBar.isVisible = true
    }

    private fun requestLocationPermission() {
        if (hasLocationPermission()) {
            HomePrefs.setLocationPrompted(this, true)
            resolveAndStoreLocation()
            showInterestSelection()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        return coarseGranted || fineGranted
    }

    private fun resolveAndStoreLocation() {
        val locationManager = getSystemService(LocationManager::class.java) ?: return
        if (!hasLocationPermission()) return
        lifecycleScope.launch {
            val location = getCoarseLocation(locationManager)
            if (location != null) {
                val userLocation = withContext(Dispatchers.IO) { reverseGeocode(location) }
                if (userLocation != null) {
                    HomePrefs.setUserLocation(this@OnboardingActivity, userLocation)
                }
            }
        }
    }

    private suspend fun getCoarseLocation(locationManager: LocationManager): Location? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return suspendCancellableCoroutine { cont ->
                val provider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    LocationManager.NETWORK_PROVIDER
                } else {
                    LocationManager.GPS_PROVIDER
                }
                locationManager.getCurrentLocation(provider, null, mainExecutor) { loc ->
                    cont.resume(loc)
                }
            }
        }
        return withContext(Dispatchers.IO) {
            val providers = locationManager.getProviders(true)
            val provider = providers.firstOrNull() ?: return@withContext null
            locationManager.getLastKnownLocation(provider)
        }
    }

    @Suppress("DEPRECATION")
    private fun reverseGeocode(location: Location): UserLocation? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(this, Locale.getDefault())
        val results = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        val address = results?.firstOrNull() ?: return null
        val city = address.locality ?: address.subAdminArea ?: address.subLocality
        val state = address.adminArea ?: address.subAdminArea
        val resolvedCity = city?.takeIf { it.isNotBlank() }
        val resolvedState = state?.takeIf { it.isNotBlank() }
        if (resolvedCity.isNullOrBlank() || resolvedState.isNullOrBlank()) {
            return null
        }
        return UserLocation(city = resolvedCity, state = resolvedState)
    }
}

private class InterestAdapter(
    private val items: List<Interest>,
    private val selectedIds: Set<String>,
    private val onSelectionChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<InterestAdapter.InterestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterestViewHolder {
        val view = layoutInflater(parent).inflate(
            R.layout.item_interest_pill,
            parent,
            false
        ) as TextView
        return InterestViewHolder(view)
    }

    override fun onBindViewHolder(holder: InterestViewHolder, position: Int) {
        val interest = items[position]
        holder.bind(interest, selectedIds.contains(interest.id), onSelectionChanged)
    }

    override fun getItemCount(): Int = items.size

    inner class InterestViewHolder(private val pill: TextView) : RecyclerView.ViewHolder(pill) {
        fun bind(
            interest: Interest,
            isSelected: Boolean,
            onSelectionChanged: (String, Boolean) -> Unit
        ) {
            pill.text = interest.displayName
            pill.isSelected = isSelected
            pill.setOnClickListener {
                val position = adapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                onSelectionChanged(interest.id, !isSelected)
                notifyItemChanged(position)
            }
        }
    }

    private fun layoutInflater(parent: ViewGroup): android.view.LayoutInflater {
        return android.view.LayoutInflater.from(parent.context)
    }
}
