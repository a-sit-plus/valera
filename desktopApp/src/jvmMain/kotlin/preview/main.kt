package preview

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import at.asitplus.wallet.lib.data.ConstantIndex
import navigation.InformationPage
import navigation.NavigationStack
import navigation.Page
import ui.composables.AttributeAvailability
import ui.composables.PersonalDataCategory
import ui.views.AuthenticationConsentPage
import ui.views.AuthenticationConsentView
import ui.views.AuthenticationQrCodeScannerPage
import ui.views.AuthenticationQrCodeScannerView
import ui.views.AuthenticationSPInfoPage
import ui.views.AuthenticationSPInfoView
import ui.views.HomePage
import ui.views.MyDataView
import ui.views.SettingsView
import ui.views.ShowDataPage
import ui.views.ShowDataView

private fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        PreviewNavigationScreen()
    }
}

//@Composable
//fun AdmissionDataScreen() {
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = {
//                    Text("Zulassungsdaten")
//                },
//                navigationIcon = {
//                    IconButton(
//                        onClick = {
//                        }
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.ArrowBack,
//                            contentDescription = "Navigate Up",
//                        )
//                    }
//                }
//            )
//        },
//        bottomBar = {
//            BottomAppBar {
//                Row(
//                    horizontalArrangement = Arrangement.Center,
//                    modifier = Modifier.fillMaxWidth(),
//                ) {
//                    OutlinedButton(onClick = {
//
//                    }) {
//                        Icon(
//                            imageVector = Icons.Default.Delete,
//                            contentDescription = "Delete",
//                        )
//                        Text("Löschen")
//                    }
//                    Button(
//                        onClick = {
//                        }
//                    ) {
//                        Icon(
//                            imageVector = Icons.Default.Refresh,
//                            contentDescription = "Refresh",
//                        )
//                        Text("Erneuern")
//                    }
//                }
//            }
//        }
//    ) {
//        Column {
//            Text("Besitzer")
//            Column(
//                modifier = Modifier.padding(horizontal = 32.dp)
//            ) {
//                Text("Besitzer")
//                Text("Besitzer")
//                Text("Besitzer")
//            }
//        }
//    }
//}
//
//@Preview
//@Composable
//fun AdmissionDataScreenPreview() {
//    AdmissionDataScreen()
//}

private enum class Route(
    val title: String,
    val icon: @Composable () -> Unit,
    val destination: Page,
    val isActive: (Page) -> Boolean
) {
    MY_DATA_SCREEN(
        title = "Meine Daten",
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Meine Daten ansehen",
            )
        },
        destination = HomePage(),
        isActive = {
            when(it) {
                is HomePage -> true
                else -> false
            }
        },
    ),
    SHOW_DATA_SCREEN(
        title = "Daten Vorzeigen",
        icon = {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Daten Vorzeigen",
            )
        },
        destination = ShowDataPage(),
        isActive = {
            when(it) {
                is ShowDataPage -> true
                else -> false
            }
        },
    ),
    INFORMATION_SCREEN(
        title = "Informationen",
        icon = {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Weitere Information",
            )
        },
        destination = InformationPage(),
        isActive = {
            when(it) {
                is InformationPage -> true
                else -> false
            }
        },
    ),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreviewNavigationScreen() {
    val navigationData = { page: Page ->
        when(page) {
            is HomePage -> {
                Route.MY_DATA_SCREEN
            }
            is ShowDataPage -> {
                Route.SHOW_DATA_SCREEN
            }
            is InformationPage -> {
                Route.INFORMATION_SCREEN
            }
            else -> null
        }
    }

    val navigationStack = rememberSaveable(
        saver = listSaver<NavigationStack<Page>, Page>(
            restore = { NavigationStack(*it.toTypedArray()) },
            save = { it.stack },
        )
    ) {
        NavigationStack(HomePage())
    }
    val globalBack = {
        navigationStack.back()
    }


    Scaffold(
        bottomBar = {
            val (_, page) = navigationStack.lastWithIndex()
            val pageNavigationData = navigationData(page)
            if (pageNavigationData != null) {
                NavigationBar {
                    for (route in listOf(
                        Route.MY_DATA_SCREEN,
                        Route.SHOW_DATA_SCREEN,
                        Route.INFORMATION_SCREEN,
                    )) {
                        NavigationBarItem(
                            icon = route.icon,
                            label = {
                                Text(route.title)
                            },
                            onClick = {
                                navigationStack.push(route.destination)
                            },
                            selected = route.isActive(page)
                        )
                    }
                }
            }
        },
    ) {
        Box(modifier = Modifier.padding(it)) {
            AnimatedContent(targetState = navigationStack.lastWithIndex()) { (_, page) ->
                when (page) {
                    is HomePage -> {
                        MyDataView(
                            refreshCredentials = {}
                        )
                    }

                    is ShowDataPage -> {
                        ShowDataView(
                            navigateToAuthenticationAtSp = {
//                                navigationStack.push(AuthenticationQrCodeScannerPage())
                                navigationStack.push(
                                    AuthenticationConsentPage(
                                        spName = "Post-Schalter#3",
                                        spLocation = "St. Peter Hauptstraße\n8010, Graz",
                                        requestedAttributes = mapOf(
                                            PersonalDataCategory.IdentityData to listOf(
                                                AttributeAvailability(
                                                    attributeName = "Vorname",
                                                    isAvailable = false,
                                                ),
                                                AttributeAvailability(
                                                    attributeName = "Nachname",
                                                    isAvailable = false,
                                                ),
                                                AttributeAvailability(
                                                    attributeName = "Aktuelles Foto aus zentralem Identitätsdokumentenregister",
                                                    isAvailable = false,
                                                ),
                                            ),
                                            PersonalDataCategory.ResidencyData to listOf(
                                                AttributeAvailability(
                                                    attributeName = "Straße",
                                                    isAvailable = false,
                                                ),
                                                AttributeAvailability(
                                                    attributeName = "Hausnummer",
                                                    isAvailable = false,
                                                ),
                                                AttributeAvailability(
                                                    attributeName = "Postleitzahl",
                                                    isAvailable = true,
                                                ),
                                                AttributeAvailability(
                                                    attributeName = "Ort",
                                                    isAvailable = true,
                                                ),
                                            ),
                                        ).toList()
                                    )
                                )
                            },
                            navigateToShowDataToExecutive = {},
                            navigateToShowDataToOtherCitizen = {},
                        )
                    }

                    is InformationPage -> {
                        SettingsView(
                            host = "http://www.example.com",
                            onChangeHost = {},
                            credentialRepresentation = ConstantIndex.CredentialRepresentation.PLAIN_JWT,
                            onChangeCredentialRepresentation = {},
                            isSaveEnabled = false,
                            onChangeIsSaveEnabled = {},
                            onClickSaveConfiguration = {},
                            stage = "T",
                            version = "1.0.0 / 2389237",
                            onClickFAQs = {},
                            onClickDataProtectionPolicy = {},
                            onClickLicenses = {},
                            onClickShareLogFile = {},
                            onClickResetApp = {},
                        )
                    }

                    is AuthenticationQrCodeScannerPage -> {
                        AuthenticationQrCodeScannerView(
                            navigateUp = globalBack,
                            navigateToConsentScreenWithResult = { name, location ->
                                navigationStack.push(AuthenticationSPInfoPage(
                                    spName = name,
                                    spLocation = location,
                                ))
                            },
                        )
                    }

                    is AuthenticationSPInfoPage -> {
                        AuthenticationSPInfoView(
                            navigateUp = globalBack,
                            cancelAuthentication = {},
                            authenticateAtSp = {},
                            spName = page.spName,
                            spLocation = page.spLocation,
                        )
                    }

                    is AuthenticationConsentPage -> {
                        val bottomSheetState = rememberModalBottomSheetState()
                        var showBottomSheet by remember { mutableStateOf(false) }

                        AuthenticationConsentView(
                            navigateUp = globalBack,
                            cancelAuthentication = globalBack,
                            consentToDataTransmission = {
                                showBottomSheet = true
                            },
                            loadMissingData = {
                                globalBack()

                                navigationStack.push(AuthenticationConsentPage(
                                    spName = page.spName,
                                    spLocation = page.spLocation,
                                    requestedAttributes = page.requestedAttributes.map {
                                        it.copy(second = it.second.map {
                                            it.copy(isAvailable = true)
                                        })
                                    }
                                ))
                            },
                            requestedAttributes = page.requestedAttributes,
                            spName = page.spName,
                            spLocation = page.spLocation,
                            bottomSheetState = bottomSheetState,
                            showBottomSheet = showBottomSheet,
                            onBottomSheetDismissRequest = {
                                showBottomSheet = !showBottomSheet
                            }
                        )
                    }
                }
            }
        }
    }
}