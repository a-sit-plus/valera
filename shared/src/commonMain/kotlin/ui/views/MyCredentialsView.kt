package ui.views

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.CredentialExtractor
import ui.composables.AdmissionData
import ui.composables.AgeData
import ui.composables.DrivingData
import ui.composables.FloatingActionButtonHeightSpacer
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
fun ColumnScope.MyCredentialsView(
    credentials: List<SubjectCredentialStore.StoreEntry>,
    decodeImage: (image: ByteArray) -> ImageBitmap,
    navigateToIdentityData: (() -> Unit)? = null,
    navigateToAgeData: (() -> Unit)? = null,
    navigateToResidenceData: (() -> Unit)? = null,
    navigateToDrivingData: (() -> Unit)? = null,
    navigateToAdmissionData: (() -> Unit)? = null,
) {
    val credentialExtractor = CredentialExtractor(credentials)
    MyDataView(
        identityData = credentialExtractor.preIdentityData.toIdentityData(decodeImage),
        ageData = credentialExtractor.ageData,
        residenceData = credentialExtractor.residenceData,
        navigateToIdentityData = navigateToIdentityData,
        navigateToAgeData = navigateToAgeData,
        navigateToResidenceData = navigateToResidenceData,
    )
}

@Composable
fun ColumnScope.MyDataView(
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
    val gapSize = 20.dp
    val paddingModifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = gapSize)

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
                modifier = paddingModifier,
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
                modifier = paddingModifier,
            )
        }
    }
    if (residenceData != null) {
        if (
            listOf(
                residenceData.villageName != null,
                residenceData.postalCode != null,
                residenceData.streetName != null,
                residenceData.houseNumber != null,
                residenceData.stairName != null,
                residenceData.doorName != null,
            ).any { it }
        ) {
            PersonResidenceDataDetailCard(
                residenceData = residenceData,
                modifier = paddingModifier,
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
                modifier = paddingModifier,
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
    FloatingActionButtonHeightSpacer(
        externalPadding = gapSize,
    )
}
