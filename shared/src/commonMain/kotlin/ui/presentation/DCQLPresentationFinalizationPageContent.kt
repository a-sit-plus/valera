package ui.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLQuery
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_location
import at.asitplus.valera.resources.attribute_friendly_name_data_recipient_name
import at.asitplus.valera.resources.heading_label_authenticate_at_device_screen
import at.asitplus.valera.resources.heading_label_show_data_third_party
import at.asitplus.valera.resources.prompt_send_above_data
import at.asitplus.valera.resources.section_heading_data_recipient
import org.jetbrains.compose.resources.stringResource
import ui.composables.DataDisplaySection
import ui.composables.ScreenHeading

@Composable
fun DCQLPresentationFinalizationPageContent(
    authenticateAtRelyingParty: Boolean,
    serviceProviderLocalizedName: String?,
    serviceProviderLocalizedLocation: String,
    dcqlQuery: DCQLQuery,
    selections: Map<DCQLCredentialQueryIdentifier, List<SelectableCredentialSubmissionCard>>,
    onAbort: () -> Unit,
    onSubmit: () -> Unit,
    serviceProviderLogo: ImageBitmap? = null,
) {
    Scaffold(
        bottomBar = {
            CommonBottomButtonsAbortContinue(
                text = stringResource(Res.string.prompt_send_above_data),
                onAbort = onAbort,
                onContinue = onSubmit,
            )
        }
    ) {
        Column(
            modifier = Modifier.padding(it).fillMaxSize(),
        ) {
            val title = if (authenticateAtRelyingParty) {
                stringResource(Res.string.heading_label_authenticate_at_device_screen)
            } else {
                stringResource(Res.string.heading_label_show_data_third_party)
            }
            ScreenHeading(title, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(32.dp),
                modifier = Modifier.verticalScroll(state = rememberScrollState()).padding(16.dp),
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

                selections.entries.sortedBy {
                    it.key.string
                }.flatMap {
                    it.value
                }.forEach { card ->
                    // TODO: good enough or should we have separate cards for final submissions?
                    //  - if these cards should be reused, then allowMultiSelection shouldn't be relevant with (isSelected, onToggleSelection) = (true, null)
                    //  - Cards should therefore implicitly handle the case (true, *, null) to show the card without any selection specific semantics UI
                    card(
                        isSelected = true,
                        allowMultiSelection = false,
                        onToggleSelection = null
                    )
                }
            }
        }
    }
}