package ui.views

import Resources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import io.ktor.util.decodeBase64Bytes
import navigation.Page
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import ui.composables.AgeData
import ui.composables.DrivingData
import ui.composables.IdentityData
import ui.composables.PersonAgeDataDetailCard
import ui.composables.PersonDrivingDataDetailCard
import ui.composables.PersonIdentityDataDetailCard

class HomePage : Page

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MyCredentialsView(
    credentials: List<SubjectCredentialStore.StoreEntry>,
    onCredential: (id: String) -> Unit,
    decodeImage: (image: ByteArray) -> ImageBitmap,
) {
    val state = rememberLazyListState()
    LazyRow(flingBehavior = rememberSnapFlingBehavior(lazyListState = state), state = state) {
        items(credentials.size) {
            val credentialId = when (val credential = credentials[it]) {
                is SubjectCredentialStore.StoreEntry.Vc -> credential.vc.jwtId
                is SubjectCredentialStore.StoreEntry.SdJwt -> credential.sdJwt.jwtId
                else -> null
            }

            if (credentialId != null) {
                IdAustriaCredentialCard(
                    credential = credentials[it],
                    onCredential = {
                        onCredential(credentialId)
                    },
                    decodeImage = decodeImage,
                    modifier = Modifier.fillParentMaxWidth(),
                )
            }
        }
    }
}



data class IdAustriaCredentialData(
    val firstname: String,
    val lastname: String,
    val portrait: ByteArray?,
)

@Composable
private fun IdAustriaCredentialCard(
    credential: SubjectCredentialStore.StoreEntry,
    onCredential: () -> Unit,
    decodeImage: (image: ByteArray) -> ImageBitmap,
    modifier: Modifier = Modifier,
) {
    val data: IdAustriaCredentialData? = when (credential) {
        is SubjectCredentialStore.StoreEntry.Vc -> {
            when (val credentialSubject = credential.vc.vc.credentialSubject) {
                is IdAustriaCredential -> IdAustriaCredentialData(
                    firstname = credentialSubject.firstname,
                    lastname = credentialSubject.lastname,
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
            val portraitEncoded =
                credential.disclosures.filter { it.value?.claimName == "portrait" }
                    .firstNotNullOf { it.value?.claimValue } as String
            val portrait = portraitEncoded.decodeBase64Bytes()
            IdAustriaCredentialData(
                firstname = firstname,
                lastname = lastname,
                portrait = portrait,
            )
        }

        else -> null
    }

    if (data != null) {
        IdAustriaCredentialCard(
            onCredential = onCredential,
            firstname = data.firstname,
            lastname = data.lastname,
            portrait = data.portrait?.let { decodeImage(it) },
            modifier = modifier,
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun IdAustriaCredentialCard(
    firstname: String,
    lastname: String,
    portrait: ImageBitmap?,
    onCredential: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier.padding(16.dp)
            .clickable(onClick = { onCredential() })
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Resources.CREDENTIAL, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.size(15.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
            Spacer(Modifier.size(15.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
                if (portrait != null) {
                    Image(
                        bitmap = portrait,
                        contentDescription = "Portrait",
                    )
                } else {
                    Image(
                        painterResource("3d-casual-life-smiling-face-with-smiling-eyes.png"),
                        contentDescription = null,
                        Modifier.size(150.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Spacer(Modifier.size(30.dp))
            Text(
                "$firstname $lastname",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(Modifier.size(10.dp))
            Text(Resources.ID_AUSTRIA_CREDENTIAL, fontSize = 12.sp)
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
                PersonIdentityDataDetailCard(
                    identityData = identityData,
                    modifier = paddingModifier,
                    onDetailClick = navigateToIdentityData,
                )
            }
            if (ageData != null) {
                PersonAgeDataDetailCard(
                    ageData = ageData,
                    modifier = paddingModifier,
                    onDetailClick = navigateToAgeData,
                )
            }
            if (drivingData != null) {
                PersonDrivingDataDetailCard(
                    drivingData = drivingData,
                    modifier = paddingModifier,
                    onDetailClick = navigateToDrivingData,
                )
            }
        }
    }
}
