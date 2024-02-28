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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
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
import ui.composables.admissionData
import ui.composables.ageData
import ui.composables.drivingData
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
    MyDataView(
        refreshCredentials = onRefreshCredentials,
        identityData = credentials.preIdentityData.toIdentityData(decodeImage),
        ageData = credentials.ageData,
        residenceData = credentials.residenceData,
        drivingData = credentials.drivingData,
        admissionData = credentials.admissionData,
        navigateToIdentityData = navigateToIdentityData,
        navigateToAgeData = navigateToAgeData,
        navigateToResidenceData = navigateToResidenceData,
        navigateToDrivingData = navigateToDrivingData,
        navigateToAdmissionData = navigateToAdmissionData,
    )
//    val attributes = credentials.flatMap {
//        when (it) {
//            is SubjectCredentialStore.StoreEntry.Vc -> {
//                when (val credentialSubject = it.vc.vc.credentialSubject) {
//                    is IdAustriaCredential -> {
//                        listOf(
//                            "credentialSubject.id: ${credentialSubject.id}",
//                            "credentialSubject.bpk: ${credentialSubject.bpk}",
//                            "credentialSubject.firstname: ${credentialSubject.firstname}",
//                            "credentialSubject.lastname: ${credentialSubject.lastname}",
//                            "credentialSubject.dateOfBirth: ${credentialSubject.dateOfBirth}",
//                            "credentialSubject.portrait: ${credentialSubject.portrait}",
//                            "credentialSubject.mainAddress: ${credentialSubject.mainAddress}",
//                            "credentialSubject.ageOver14: ${credentialSubject.ageOver14}",
//                            "credentialSubject.ageOver16: ${credentialSubject.ageOver16}",
//                            "credentialSubject.ageOver18: ${credentialSubject.ageOver18}",
//                            "credentialSubject.ageOver21: ${credentialSubject.ageOver21}",
//                        )
//                    }
//
//                    else -> listOf()
//                }
//            }
//
//            is SubjectCredentialStore.StoreEntry.SdJwt -> {
//                it.disclosures.entries.map {
//                    "${it.value?.claimName}: ${it.value?.claimValue}"
//                }.toList()
//            }
//
//            else -> listOf()
//        }
//    }
//    LazyColumn {
//        items(attributes.size) {
//            val attribute = attributes[it]
//
//            Text(attribute)
//        }
//    }
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
            val paddingModifier = Modifier.padding(horizontal = 16.dp)
            val gapSize = 20.dp
            if (identityData != null) {
                if (
                    listOf(
                        identityData.lastname != null,
                        identityData.firstname != null,
                        identityData.portrait != null,
                        identityData.dateOfBirth != null,
                    ).any()
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
                    ).any()
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
//                        residenceData.postalCode != null,
//                        residenceData.townName != null,
                    ).any()
                ) {
                    PersonResidenceDataDetailCard(
                        residenceData = residenceData,
                        modifier = paddingModifier.padding(bottom = gapSize),
                    )
                }
            }
            if (drivingData != null) {
                if (drivingData.drivingPermissions.isNotEmpty()) {
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
                    ).any()
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
