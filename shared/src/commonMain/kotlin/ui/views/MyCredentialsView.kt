package ui.views

import Resources
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.ageLowerBounds
import data.ageUpperBounds
import data.dateOfBirth
import data.drivingPermissions
import data.firstname
import data.lastname
import data.portrait
import io.ktor.util.decodeBase64Bytes
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import ui.composables.AgeData
import ui.composables.DrivingData
import ui.composables.IdentityData
import ui.composables.PersonAgeDataDetailCard
import ui.composables.PersonDrivingDataDetailCard
import ui.composables.PersonIdentityDataDetailCard


@Composable
fun MyCredentialsView(
    credentials: List<SubjectCredentialStore.StoreEntry>,
    onRefreshCredentials: () -> Unit,
    decodeImage: (image: ByteArray) -> ImageBitmap,
    navigateToIdentityData: (() -> Unit)? = null,
    navigateToAgeData: (() -> Unit)? = null,
    navigateToDrivingData: (() -> Unit)? = null,
) {
    MyDataView(
        refreshCredentials = onRefreshCredentials,
        identityData = IdentityData(
            firstname = credentials.firstNotNullOfOrNull { it.firstname },
            lastname = credentials.firstNotNullOfOrNull { it.lastname },
            dateOfBirth = credentials.firstNotNullOfOrNull { it.dateOfBirth },
            portrait = credentials.firstNotNullOfOrNull { it.portrait }?.let(decodeImage),
        ),
        ageData = AgeData(
            ageLowerBounds = credentials.flatMap { it.ageLowerBounds },
            ageUpperBounds = credentials.flatMap { it.ageUpperBounds },
        ),
        drivingData = DrivingData(
            drivingPermissions = credentials.flatMap { it.drivingPermissions },
        ),
        navigateToIdentityData = navigateToIdentityData,
        navigateToAgeData = navigateToAgeData,
        navigateToDrivingData = navigateToDrivingData,
    )
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text(
//                        text = "Meine Daten",
//                        style = MaterialTheme.typography.headlineLarge,
//                    )
//                }
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = onRefreshCredentials,
//            ) {
//                Icon(
//                    imageVector = Icons.Default.Refresh,
//                    contentDescription = "Refresh Credentials",
//                )
//            }
//        },
//    ) { scaffoldPadding ->
//        Box(modifier = Modifier.padding(scaffoldPadding).fillMaxSize()) {
////            val state = rememberLazyListState()
////            LazyColumn(
////                flingBehavior = rememberSnapFlingBehavior(lazyListState = state),
////                state = state,
////                modifier = Modifier.fillMaxSize(),
////            ) {
////                items(credentials.size) {
////                    val credential = credentials[it]
////                    val credentialId = when (credential) {
////                        is SubjectCredentialStore.StoreEntry.Vc -> credential.vc.jwtId
////                        is SubjectCredentialStore.StoreEntry.SdJwt -> credential.sdJwt.jwtId
////                        else -> null
////                    }
////
////                    if (credentialId != null) {
////                        CredentialCard(
////                            credential = credential,
////                            decodeImage = decodeImage,
////                            modifier = Modifier.padding(bottom = 8.dp, end = 8.dp, start = 8.dp)
////                                .fillMaxWidth()
////                                .clickable {
////                                    onCredential(credentialId)
////                                },
////                        )
////                    }
////                }
////            }
//        }
//    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialCard(
    credential: SubjectCredentialStore.StoreEntry,
    decodeImage: (image: ByteArray) -> ImageBitmap,
    modifier: Modifier = Modifier,
) {
    val credentialScheme = when (credential) {
        is SubjectCredentialStore.StoreEntry.Vc -> credential.scheme
        is SubjectCredentialStore.StoreEntry.SdJwt -> credential.scheme
        is SubjectCredentialStore.StoreEntry.Iso -> credential.scheme
    }

    ElevatedCard(
        modifier = modifier,
    ) {
        Column {
            TopAppBar(
                title = {
                    Text(
                        credentialScheme.vcType,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    IconButton(
                        onClick = {}
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Show Action Menu",
                        )
                    }
                }
            )
            when (credentialScheme) {
                is IdAustriaScheme -> {
                    IdAustriaCredentialCardContent(
                        credential = credential,
                        decodeImage = decodeImage,
                    )
                }

                else -> {}
            }
        }
    }
}


data class IdAustriaCredentialUiState(
    val firstname: String,
    val lastname: String,
//    val dateOfBirth: LocalDate,
    val portrait: ByteArray?,
)

@Composable
private fun IdAustriaCredentialCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    decodeImage: (image: ByteArray) -> ImageBitmap,
    modifier: Modifier = Modifier,
) {
    val data: IdAustriaCredentialUiState? = when (credential) {
        is SubjectCredentialStore.StoreEntry.Vc -> {
            when (val credentialSubject = credential.vc.vc.credentialSubject) {
                is IdAustriaCredential -> IdAustriaCredentialUiState(
                    firstname = credentialSubject.firstname,
                    lastname = credentialSubject.lastname,
//                    dateOfBirth = credentialSubject.dateOfBirth,
                    portrait = credentialSubject.portrait,
                )

                else -> null
            }
        }

        is SubjectCredentialStore.StoreEntry.SdJwt -> {
            val firstname = credential.disclosures.filter { it.value?.claimName == "firstname" }
                .firstNotNullOf { it.value?.claimValue } as String
            val lastname = credential.disclosures.filter { it.value?.claimName == "lastname" }
                .firstNotNullOf { it.value?.claimValue } as String
//            val dateOfBirth = credential.disclosures.filter { it.value?.claimName == "dateOfBirth" }
//                .firstNotNullOf { it.value?.claimValue } as LocalDate
            val portraitEncoded =
                credential.disclosures.filter { it.value?.claimName == "portrait" }
                    .firstNotNullOf { it.value?.claimValue } as String
            val portrait = portraitEncoded.decodeBase64Bytes()
            IdAustriaCredentialUiState(
                firstname = firstname,
                lastname = lastname,
//                dateOfBirth = dateOfBirth,
                portrait = portrait,
            )
        }

        else -> null
    }

    if (data != null) {
        IdAustriaCredentialCardContent(
            name = "${data.firstname} ${data.lastname}",
//            dateOfBirth = data.dateOfBirth,
            portrait = data.portrait?.let { decodeImage(it) },
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun IdAustriaCredentialCardContent(
    name: String,
//    dateOfBirth: LocalDate,
    portrait: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            IconButton(
                onClick = {},
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = contentColorFor(MaterialTheme.colorScheme.secondaryContainer),
                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    disabledContentColor = contentColorFor(MaterialTheme.colorScheme.secondaryContainer),
                ),
                enabled = false,
            ) {
                if (portrait != null) {
                    Icon(
                        bitmap = portrait,
                        contentDescription = null,
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painterResource("3d-casual-life-smiling-face-with-smiling-eyes.png"),
                            contentDescription = null,
                            Modifier.size(150.dp),
                            contentScale = ContentScale.Crop,
                        )
//                        Text(
//                            text = "ID",
//                            fontWeight = FontWeight.SemiBold,
//                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    Resources.ID_AUSTRIA_CREDENTIAL,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}


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
                        modifier = paddingModifier,
                        onDetailClick = navigateToIdentityData,
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
                        modifier = paddingModifier,
                        onDetailClick = navigateToAgeData,
                    )
                }
            }
            if (drivingData != null) {
                if (drivingData.drivingPermissions.isNotEmpty()) {
                    PersonDrivingDataDetailCard(
                        drivingData = drivingData,
                        modifier = paddingModifier,
                        onDetailClick = navigateToDrivingData,
                    )
                }
            }
        }
    }
}
