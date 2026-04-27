package ui.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_location
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_name
import at.asitplus.valera.resources.heading_label_authenticate_at_device_screen
import at.asitplus.valera.resources.heading_label_show_data_third_party
import at.asitplus.valera.resources.prompt_send_above_data
import at.asitplus.valera.resources.section_heading_data_recipient
import at.asitplus.wallet.app.common.extractConsentData
import at.asitplus.wallet.app.common.toCredentialQueryUiModel
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import org.jetbrains.compose.resources.stringResource
import ui.composables.DataDisplaySection
import ui.composables.PresentationRequestPreview
import ui.composables.ScreenHeading

@Composable
fun AuthenticationReceivedStartPageContent(
    authenticateAtRelyingParty: Boolean,
    serviceProviderLogo: ImageBitmap?,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    additionalDataView: @Composable (() -> Unit)? = null,
    onAbort: () -> Unit,
    onContinue: () -> Unit,
    presentationRequest: CredentialPresentationRequest?,
    onError: (Throwable) -> Unit,
) {
    Scaffold(
        bottomBar = {
            CommonBottomButtonsAbortContinue(
                text = stringResource(Res.string.prompt_send_above_data),
                onAbort = onAbort,
                onContinue = onContinue,
            )
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                val title = if (authenticateAtRelyingParty) {
                    stringResource(Res.string.heading_label_authenticate_at_device_screen)
                } else {
                    stringResource(Res.string.heading_label_show_data_third_party)
                }
                ScreenHeading(title)

                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState()).padding(bottom = 8.dp),
                ) {
                    if (serviceProviderLogo != null) {
                        Box(Modifier.Companion.fillMaxWidth(), contentAlignment = Alignment.Companion.Center) {
                            Image(
                                bitmap = serviceProviderLogo,
                                contentDescription = null,
                                contentScale = ContentScale.Companion.Fit,
                                modifier = Modifier.Companion.height(64.dp),
                            )
                        }
                    }

                    DataDisplaySection(
                        title = stringResource(Res.string.section_heading_data_recipient),
                        data = listOfNotNull(
                            serviceProviderLocalizedName?.let {
                                stringResource(Res.string.attribute_friendly_name_data_recipient_name) to serviceProviderLocalizedName
                            },
                            stringResource(Res.string.attribute_friendly_name_data_recipient_location) to serviceProviderLocalizedLocation,
                        ),
                    )


                    when(presentationRequest) {
                        is CredentialPresentationRequest.DCQLRequest -> Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presentationRequest.dcqlQuery.credentials.associate {
                                it.id to try { // TODO: improve on this by storing it somewhere?
                                    it.extractConsentData()
                                } catch (e: Throwable) {
                                    return@Box LaunchedEffect(Unit) {
                                        onError(e)
                                    }
                                }.toCredentialQueryUiModel()
                            }.forEach { (_, credentialQueryUiModel) ->
                                CredentialSetQueryOptionSelectionCard(
                                    credentialRepresentationLocalized = credentialQueryUiModel.credentialRepresentationLocalized,
                                    credentialSchemeLocalized = credentialQueryUiModel.credentialSchemeLocalized,
                                    credentialAttributesLocalized = credentialQueryUiModel.requestedAttributesLocalized?.let {
                                        it.attributesLocalized to it.otherAttributes
                                    },
                                )
                            }
                        }

                        is CredentialPresentationRequest.PresentationExchangeRequest -> PresentationRequestPreview(presentationRequest, onError = onError)

                        null -> {
                            // TODO?
                        }
                    }

                    if (additionalDataView != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        additionalDataView()
                    }
                }
            }
        }
    }
}