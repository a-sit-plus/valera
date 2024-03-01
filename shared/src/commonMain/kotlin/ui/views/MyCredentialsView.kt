package ui.views

import Resources
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.CredentialExtractor
import ui.composables.AdmissionData
import ui.composables.AgeData
import ui.composables.DrivingData
import ui.composables.FabHeightSpacer
import ui.composables.IdentityData
import ui.composables.PersonAdmissionDataDetailCard
import ui.composables.PersonAgeDataDetailCard
import ui.composables.PersonDrivingDataDetailCard
import ui.composables.PersonIdentityDataDetailCard
import ui.composables.PersonResidenceDataDetailCard
import ui.composables.ResidenceData
import ui.composables.ageData
import ui.composables.preIdentityData
import ui.composables.residenceData


@Composable
fun MyCredentialsView(
    credentials: List<SubjectCredentialStore.StoreEntry>,
    onRefreshCredentials: () -> Unit,
    decodeImage: (image: ByteArray) -> ImageBitmap,
    navigateToIdentityData: (() -> Unit)? = null,
    navigateToAgeData: (() -> Unit)? = null,
    navigateToResidenceData: (() -> Unit)? = null,
    navigateToDrivingData: (() -> Unit)? = null,
    navigateToAdmissionData: (() -> Unit)? = null,
) {
    val credentialExtractor = CredentialExtractor(credentials)
    MyDataView(
        refreshCredentials = onRefreshCredentials,
        identityData = credentialExtractor.preIdentityData.toIdentityData(decodeImage),
        ageData = credentialExtractor.ageData,
        residenceData = credentialExtractor.residenceData,
        navigateToIdentityData = navigateToIdentityData,
        navigateToAgeData = navigateToAgeData,
        navigateToResidenceData = navigateToResidenceData,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDataView(
    refreshCredentials: () -> Unit,
    identityData: IdentityData? = null,
    ageData: AgeData? = null,
    residenceData: ResidenceData? = null,
    drivingData: DrivingData? = null,
    admissionData: AdmissionData? = null,
    navigateToIdentityData: (() -> Unit)? = null,
    navigateToAgeData: (() -> Unit)? = null,
    navigateToResidenceData: (() -> Unit)? = null,
    navigateToDrivingData: (() -> Unit)? = null,
    navigateToAdmissionData: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        Resources.HEADING_LABEL_MY_DATA_OVERVIEW,
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
                    contentDescription = Resources.CONTENT_DESCRIPTION_REFRESH_CREDENTIALS,
                )
            }
        }
    ) {
        Column(modifier = Modifier.padding(it).verticalScroll(state = rememberScrollState())) {
            val paddingModifier = Modifier.padding(horizontal = 16.dp)
            val gapSize = 20.dp
            if (identityData != null) {
                if (
                    listOf(
                        identityData.lastname != null,
                        identityData.firstname != null,
                        identityData.portrait != null,
                        identityData.dateOfBirth != null,
                    ).any { it }
                ) {
                    PersonIdentityDataDetailCard(
                        identityData = identityData,
                        modifier = paddingModifier.padding(bottom = gapSize),
                    )
                }
            }
            if (ageData != null) {
                if (
                    listOf(
                        ageData.ageUpperBounds.isNotEmpty(),
                        ageData.ageLowerBounds.isNotEmpty(),
                    ).any { it }
                ) {
                    PersonAgeDataDetailCard(
                        ageData = ageData,
                        modifier = paddingModifier.padding(bottom = gapSize),
                    )
                }
            }
            if (residenceData != null) {
                if (
                    listOf(
                        residenceData.mainAddress != null,
                    ).any { it }
                ) {
                    PersonResidenceDataDetailCard(
                        residenceData = residenceData,
                        modifier = paddingModifier.padding(bottom = gapSize),
                    )
                }
            }
            if (drivingData != null) {
                if (
                    listOf(
                        drivingData.drivingPermissions.isNotEmpty(),
                    ).any { it }
                ) {
                    PersonDrivingDataDetailCard(
                        drivingData = drivingData,
                        modifier = paddingModifier.padding(bottom = gapSize),
                    )
                }
            }
            if (admissionData != null) {
                if (
                    listOf(
                        admissionData.carModel != null,
                        admissionData.licensePlateNumber != null,
                    ).any { it }
                ) {
                    PersonAdmissionDataDetailCard(
                        admissionData = admissionData,
                        modifier = paddingModifier,
                    )
                }
            }
            // make sufficient scroll space for FAB
            FabHeightSpacer()
        }
    }
}
