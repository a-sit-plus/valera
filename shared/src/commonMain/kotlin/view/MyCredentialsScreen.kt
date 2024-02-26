package view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import ui.views.LoadDataView
import ui.views.MyCredentialsView

@Composable
fun MyCredentialsScreen(
    credentials: List<SubjectCredentialStore.StoreEntry>,
    refreshCredentials: () -> Unit,
    startProvisioning: () -> Unit,
    startProvisioningFromQrCode: () -> Unit,
    decodeImage: (image: ByteArray) -> ImageBitmap,
) {
//    val credentials: List<SubjectCredentialStore.StoreEntry> = VerifiableCredentialJws(
//        expiration = Clock.System.now(),
//        issuer = "self",
//        jwtId = "jwtId",
//        notBefore = Clock.System.now(),
//        subject = "Test",
//        vc = VerifiableCredential(
//            id = "string1",
//            issuer = "test1",
//            Duration.ZERO,
//            credentialStatus = CredentialStatus("test", 0),
//            credentialSubject = IdAustriaCredential(
//                firstname = "VornameValue",
//                lastname = "NachnameValue",
//                bpk = "bpkValue",
//                dateOfBirth = LocalDate.fromEpochDays(0),
//                id = "idValue",
//                ageOver14 = true,
//                ageOver16 = false,
//                ageOver18 = false,
//                ageOver21 = false,
//                mainAddress = "mainAddressValue"
//            ),
//            "string2",
//        )
//    ).let {
//        SubjectCredentialStore.StoreEntry.Vc(
//            scheme = at.asitplus.wallet.idaustria.IdAustriaScheme,
//            vc = it,
//            vcSerialized = it.toString(),
//        )
//    }.let {
//        listOf(it)
//    }

    if (credentials.isEmpty()) {
        LoadDataView(
            loadData = startProvisioning,
            navigateUp = null,
            onLoadDataFromQrCode = startProvisioningFromQrCode,
        )
    } else {
        MyCredentialsView(
            credentials = credentials,
            onRefreshCredentials = refreshCredentials,
            decodeImage = decodeImage,
        )
//                MyDataView(
//                    refreshCredentials = refreshCredentials,
//                    identityData = IdentityData(
//                        name = credentials.map { credential ->
//                            when (val credentialSubject = credential.vc.vc.credentialSubject) {
//                                is IdAustriaCredential -> credentialSubject.firstname
//                                else -> null
//                            }
//                        }.filterNotNull().firstOrNull() ?: "",
//                        birthdate = credentials.map { credential ->
//                            when (val credentialSubject = credential.vc.vc.credentialSubject) {
//                                is IdAustriaCredential -> credentialSubject.dateOfBirth
//                                else -> null
//                            }
//                        }.filterNotNull().firstOrNull(),
//                        portrait = credentials.map { credential ->
//                            when (val credentialSubject = credential.vc.vc.credentialSubject) {
//                                is IdAustriaCredential -> credentialSubject.portrait
//                                else -> null
//                            }
//                        }.filterNotNull().firstOrNull(),
//                    ), // TODO("Create from credential attributes")
//                    navigateToIdentityData = null, // TODO("Create from credential attributes")
//                    ageData = null, // TODO("Create from credential attributes")
//                    navigateToAgeData = null, // TODO("Create from credential attributes")
//                    drivingData = null, // TODO("Create from credential attributes")
//                    navigateToDrivingData = null, // TODO("Create from credential attributes")
//                )
    }
}
