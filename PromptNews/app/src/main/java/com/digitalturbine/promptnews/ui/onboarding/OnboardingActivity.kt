package com.digitalturbine.promptnews.ui.onboarding

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.Alignment
import com.digitalturbine.promptnews.MainActivity
import com.digitalturbine.promptnews.data.Interest
import com.digitalturbine.promptnews.data.InterestCatalog
import com.digitalturbine.promptnews.data.UserInterestRepositoryImpl

class OnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = UserInterestRepositoryImpl.getInstance(this)
        if (repo.isOnboardingComplete()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContent {
            MaterialTheme {
                OnboardingScreen(
                    interests = InterestCatalog.interests,
                    onContinue = { selected ->
                        repo.saveSelectedInterests(selected)
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun OnboardingScreen(
    interests: List<Interest>,
    onContinue: (List<Interest>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(setOf<String>()) }
    val canContinue = selectedIds.size >= 3

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Choose your interests") },
            colors = TopAppBarDefaults.topAppBarColors()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Pick at least 3 topics to personalize your Following feed.",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                interests.forEach { interest ->
                    val selected = selectedIds.contains(interest.id)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            selectedIds = if (selected) {
                                selectedIds - interest.id
                            } else {
                                selectedIds + interest.id
                            }
                        },
                        label = { Text(text = interest.displayName) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${selectedIds.size} selected",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val selected = interests.filter { selectedIds.contains(it.id) }
                        onContinue(selected)
                    },
                    enabled = canContinue,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    Text(text = "Continue")
                }
            }
        }
    }
}
