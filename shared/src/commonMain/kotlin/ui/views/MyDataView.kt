package ui.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import navigation.Page
import ui.composables.AgeData
import ui.composables.DrivingData
import ui.composables.IdentityData
import ui.composables.PersonAgeDataDetailCard
import ui.composables.PersonDrivingDataDetailCard
import ui.composables.PersonIdentityDataDetailCard

class HomePage : Page

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDataView(
    refreshCredentials: () -> Unit,
    identityData: IdentityData? = null,
    ageData: AgeData? = null,
    drivingData: DrivingData? = null,
    navigateToDrivingData: (() -> Unit)? = null,
    navigateToIdentityData: (() -> Unit)? = null,
    navigateToAgeData: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Meine Daten",
                        style = MaterialTheme.typography.headlineLarge
                    )
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = refreshCredentials,
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Credentials",
                )
            }
        }
    ) {
        Column(modifier = Modifier.padding(it).verticalScroll(state = rememberScrollState())) {
            val paddingModifier = Modifier.padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
            if (identityData != null) {
                PersonIdentityDataDetailCard(
                    identityData = identityData,
                    modifier = paddingModifier,
                    onDetailClick = navigateToIdentityData,
                )
            }
            if (ageData != null) {
                PersonAgeDataDetailCard(
                    ageData = ageData, modifier = paddingModifier,
                    onDetailClick = navigateToAgeData,
                )
            }
            if (drivingData != null) {
                PersonDrivingDataDetailCard(
                    drivingData = drivingData, modifier = paddingModifier,
                    onDetailClick = navigateToDrivingData,
                )
            }
        }
    }
}
