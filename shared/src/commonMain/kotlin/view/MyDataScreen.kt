package view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.idaustria.IdAustriaCredential
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialStatus
import at.asitplus.wallet.lib.data.VerifiableCredential
import at.asitplus.wallet.lib.data.VerifiableCredentialJws
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import ui.views.LoadDataView
import ui.views.MyCredentialsView
import kotlin.time.Duration

@Composable
fun MyDataScreen(
    walletMain: WalletMain,
    onClickShowCredentialDetails: (String) -> Unit,
    refreshCredentials: () -> Unit,
) {
    val storeContainerState by walletMain.subjectCredentialStore.observeStoreContainer().collectAsState(null)
    val credentialRepresentationFlow by walletMain.walletConfig.credentialRepresentation.collectAsState(null)

    storeContainerState?.let { storeContainer ->
        credentialRepresentationFlow?.let { credentialRepresentation ->
//            val credentials = storeContainer.credentials
            val credentials = VerifiableCredentialJws(
                expiration = Clock.System.now(),
                issuer = "self",
                jwtId = "jwtId",
                notBefore = Clock.System.now(),
                subject = "Test",
                vc = VerifiableCredential(
                    "string1",
                    "test1",
//                    arrayOf("test1", "test2"),
                    Duration.ZERO,
                    credentialStatus = CredentialStatus("test", 0),
                    credentialSubject = IdAustriaCredential(
                        firstname = "firstname",
                        lastname = "lastname",
                        bpk = "bpk",
                        dateOfBirth = LocalDate.fromEpochDays(0),
                        id = "id",
                    ),
                    "string2",
                )
            ).let {
                listOf(
                    SubjectCredentialStore.StoreEntry.Vc(
                        scheme = at.asitplus.wallet.idaustria.IdAustriaScheme,
                        vc = it,
                        vcSerialized = it.toString(),
                    )
                )
            }

            if (credentials.isEmpty()) {
                LoadDataView(
                    loadData = refreshCredentials,
                    navigateUp = null,
                )
            } else {
                MyCredentialsView(
                    credentials = credentials,
                    onCredential = onClickShowCredentialDetails,
                    decodeImage = walletMain.platformAdapter::decodeImage,
                )
//                MyDataView(
//                    refreshCredentials = refreshCredentials,
//                    identityData = null, // TODO("Create from credential attributes")
//                    navigateToIdentityData = null, // TODO("Create from credential attributes")
//                    ageData = null, // TODO("Create from credential attributes")
//                    navigateToAgeData = null, // TODO("Create from credential attributes")
//                    drivingData = null, // TODO("Create from credential attributes")
//                    navigateToDrivingData = null, // TODO("Create from credential attributes")
//                )
            }
        }
    }
}
