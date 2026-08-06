package ui.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import at.asitplus.catchingUnwrapped
import at.asitplus.dif.InputDescriptor
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLCredentialSetQuery
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_location
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_name
import at.asitplus.valera.resources.heading_label_authenticate_at_device_screen
import at.asitplus.valera.resources.heading_label_show_data_third_party
import at.asitplus.valera.resources.prompt_send_above_data
import at.asitplus.valera.resources.section_heading_data_recipient
import at.asitplus.valera.resources.section_heading_requested_data
import at.asitplus.valera.resources.text_label_credential_request_and
import at.asitplus.valera.resources.text_label_credential_request_or
import at.asitplus.valera.resources.text_label_mandatory_dataset
import at.asitplus.valera.resources.text_label_optional_dataset
import at.asitplus.wallet.app.common.DcqlConsentData
import at.asitplus.wallet.app.common.extractConsentData
import at.asitplus.wallet.app.common.toCredentialQueryUiModel
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import ui.composables.DataDisplaySection
import ui.composables.InputDescriptorPreview
import ui.composables.PresentationRequestPreview
import ui.composables.PresentationRequestLoadingIndicator
import ui.composables.ScreenHeading

@Composable
fun AuthenticationReceivedStartPageContent(
    authenticateAtRelyingParty: Boolean,
    serviceProviderLogo: ImageBitmap?,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String?,
    additionalDataView: @Composable (() -> Unit)? = null,
    onAbort: () -> Unit,
    onContinue: () -> Unit,
    presentationRequest: CredentialPresentationRequest?,
    inputDescriptors: List<InputDescriptor>? = null,
    credentialQueryIdsSelectedForPresentation: Set<DCQLCredentialQueryIdentifier> = emptySet(),
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
                    modifier = Modifier.fillMaxSize().verticalScroll(state = rememberScrollState())
                        .padding(bottom = 8.dp),
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
                            serviceProviderLocalizedLocation?.takeIf { value -> value.isNotBlank() }?.let {
                                stringResource(Res.string.attribute_friendly_name_data_recipient_location) to it
                            },
                        ),
                    )

                    DataDisplaySection(
                        title = stringResource(Res.string.section_heading_requested_data),
                    ) {
                        when (presentationRequest) {
                            is CredentialPresentationRequest.DCQLRequest -> Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Resolved in a coroutine: scheme resolution may fetch type metadata
                                // when the in-memory scheme index is still cold (fresh process).
                                val consentData by produceState<Map<DCQLCredentialQueryIdentifier, DcqlConsentData>?>(
                                    null,
                                    presentationRequest,
                                ) {
                                    value = withContext(Dispatchers.Default) {
                                        catchingUnwrapped {
                                            presentationRequest.dcqlQuery.credentials.associate {
                                                it.id to it.extractConsentData()
                                            }
                                        }
                                    }.onFailure(onError).getOrNull()
                                }
                                if (consentData == null) {
                                    PresentationRequestLoadingIndicator()
                                }
                                consentData?.mapValues { it.value.toCredentialQueryUiModel() }?.let { uiModels ->
                                    RequestedDcqlCredentialSets(
                                        credentialSets = presentationRequest.dcqlQuery.requestedCredentialSetQueries,
                                        credentialUiModels = uiModels,
                                        selectedCredentialQueryIds = credentialQueryIdsSelectedForPresentation,
                                    )
                                }
                            }

                            is CredentialPresentationRequest.PresentationExchangeRequest -> PresentationRequestPreview(
                                presentationRequest = presentationRequest,
                                onError = onError
                            )

                            null -> if (inputDescriptors != null) {
                                InputDescriptorPreview(inputDescriptors, onError = onError)
                            } else {
                                PresentationRequestLoadingIndicator()
                            }
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

@Composable
private fun RequestedDcqlCredentialSets(
    credentialSets: Collection<DCQLCredentialSetQuery>,
    credentialUiModels: Map<DCQLCredentialQueryIdentifier, DCQLCredentialQueryUiModel>,
    selectedCredentialQueryIds: Set<DCQLCredentialQueryIdentifier>,
) {
    credentialSets.forEachIndexed { setIndex, credentialSet ->
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(
                        if (credentialSet.required) {
                            Res.string.text_label_mandatory_dataset
                        } else {
                            Res.string.text_label_optional_dataset
                        }
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.secondary,
                )

                credentialSet.options.forEachIndexed { optionIndex, option ->
                    if (optionIndex > 0) {
                        CredentialRequestRelation(Res.string.text_label_credential_request_or)
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            option.forEachIndexed { queryIndex, queryIdentifier ->
                                credentialUiModels[queryIdentifier]?.let { credentialQueryUiModel ->
                                    CredentialSetQueryOptionSelectionCard(
                                        credentialRepresentationLocalized = credentialQueryUiModel.credentialRepresentationLocalized,
                                        credentialSchemeLocalized = credentialQueryUiModel.credentialSchemeLocalized,
                                        credentialAttributesLocalized = credentialQueryUiModel.requestedAttributesLocalized?.let {
                                            it.attributesLocalized to it.otherAttributes
                                        },
                                        isSelectedForPresentation = queryIdentifier in selectedCredentialQueryIds,
                                        isFaded = selectedCredentialQueryIds.isNotEmpty() &&
                                                queryIdentifier !in selectedCredentialQueryIds,
                                    )
                                }
                                if (queryIndex < option.lastIndex) {
                                    CredentialRequestRelation(Res.string.text_label_credential_request_and)
                                }
                            }
                        }
                    }
                }
            }
        }

        val nextCredentialSet = credentialSets.elementAtOrNull(setIndex + 1)
        if (credentialSet.required && nextCredentialSet?.required == true) {
            CredentialRequestRelation(Res.string.text_label_credential_request_and)
        } else if (nextCredentialSet != null) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CredentialRequestRelation(label: org.jetbrains.compose.resources.StringResource) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}
