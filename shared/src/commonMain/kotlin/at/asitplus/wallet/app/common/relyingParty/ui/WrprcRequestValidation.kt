package at.asitplus.wallet.app.common.relyingParty.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.ui.semantics.Role
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_registration_cert_suspended
import at.asitplus.valera.resources.info_text_registration_cert_status_unknown
import at.asitplus.valera.resources.info_text_registration_cert_status_invalid
import at.asitplus.valera.resources.info_text_registration_cert_revoked
import at.asitplus.valera.resources.info_text_registration_cert_not_linked
import at.asitplus.valera.resources.label_wrp_technical_details
import at.asitplus.valera.resources.heading_wrp_access_cert_invalid
import at.asitplus.valera.resources.heading_wrp_access_cert_valid
import at.asitplus.valera.resources.heading_wrp_attributes_invalid
import at.asitplus.valera.resources.heading_wrp_credential_type_invalid
import at.asitplus.valera.resources.heading_wrp_more_failures
import at.asitplus.valera.resources.heading_wrp_registration_cert_invalid
import at.asitplus.valera.resources.heading_wrp_registration_cert_not_evaluated
import at.asitplus.valera.resources.heading_wrp_valid
import at.asitplus.valera.resources.info_text_access_cert_invalid
import at.asitplus.valera.resources.info_text_access_cert_missing
import at.asitplus.valera.resources.info_text_access_cert_valid
import at.asitplus.valera.resources.info_text_registration_cert_invalid
import at.asitplus.valera.resources.info_text_registration_cert_missing
import at.asitplus.valera.resources.info_text_registration_cert_not_evaluated
import at.asitplus.valera.resources.info_text_registration_cert_requested_claim_invalid
import at.asitplus.valera.resources.info_text_registration_cert_requested_claim_valid
import at.asitplus.valera.resources.info_text_registration_cert_typ_invalid
import at.asitplus.valera.resources.info_text_registration_cert_typ_valid
import at.asitplus.valera.resources.info_text_registration_cert_valid
import at.asitplus.valera.resources.label_access_cert
import at.asitplus.valera.resources.label_registration_cert
import at.asitplus.valera.resources.label_registration_cert_attributes
import at.asitplus.valera.resources.label_registration_cert_credential_typ
import at.asitplus.valera.resources.label_registration_cert_details_country
import at.asitplus.valera.resources.label_registration_cert_details_purpose
import at.asitplus.valera.resources.label_registration_cert_details_trade_name
import at.asitplus.valera.resources.label_registration_cert_info_uri
import at.asitplus.valera.resources.label_registration_cert_invalid
import at.asitplus.valera.resources.label_registration_cert_more_details
import at.asitplus.valera.resources.label_registration_cert_support_uri
import at.asitplus.wallet.app.common.relyingParty.WrpDisplayInfo
import at.asitplus.wallet.app.common.relyingParty.WrpValidationResult
import at.asitplus.wallet.app.common.relyingParty.WrprcCertificateValidation
import at.asitplus.wallet.app.common.relyingParty.WrpTokenStatus
import at.asitplus.wallet.app.common.relyingParty.getCurrentLocalization
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.ExpandableCard
import ui.composables.TrustState
import ui.composables.statusColors
import ui.composables.statusIcon
import ui.composables.LabeledHyperlinkText
import ui.composables.LabeledText

@Composable
fun WrprcRequestValidationSummary(list: List<WrprcRequestValidationData>) {
    Column {
        list.forEach {
            WrprcRequestValidationDataCard(it)
        }
    }
}

@Composable
fun WrprcRequestValidationDataCard(data: WrprcRequestValidationData) {
    // Only the technical errors are collapsed, the explanation is always shown
    val errorsExpanded = remember { mutableStateOf(false) }
    Column {
        Spacer(modifier = Modifier.height(8.dp))
        // The colors of the trust status and of the heading above, as foreground on the card
        val (color, icon) = when (data.validity) {
            true -> Pair(MaterialTheme.colorScheme.primary, Icons.Outlined.Check)
            false -> Pair(MaterialTheme.colorScheme.error, Icons.Outlined.Close)
            null -> Pair(MaterialTheme.colorScheme.onSurfaceVariant, Icons.Outlined.Info)
        }
        Row {
            Text(text = stringResource(data.text), color = color, modifier = Modifier.weight(1f))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.alpha(0.5f)
            )
        }
        Text(
            when (data.validity) {
                true -> stringResource(data.infoValid)
                false -> stringResource(data.infoInvalid)
                null -> stringResource(data.infoMissing ?: data.infoInvalid)
            }
        )
        if (data.errors.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable(
                        role = Role.Button,
                        onClick = { errorsExpanded.value = !errorsExpanded.value },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.label_wrp_technical_details),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = if (errorsExpanded.value) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val density = LocalDensity.current
            AnimatedVisibility(
                visible = errorsExpanded.value,
                enter = slideInVertically {
                    with(density) { -20.dp.roundToPx() }
                } + expandVertically(
                    expandFrom = Alignment.Top
                ) + fadeIn(
                    initialAlpha = 0.3f
                ),
                exit = slideOutVertically {
                    with(density) { 20.dp.roundToPx() }
                } + shrinkVertically(
                    shrinkTowards = Alignment.Bottom
                ) + fadeOut(
                    targetAlpha = 0f
                )
            ) {
                Column {
                    data.errors.forEach {
                        val text = when (it) {
                            is WrpValidationError.Message -> it.text
                            is WrpValidationError.Resource ->
                                stringResource(it.text) + (it.detail?.let { detail -> ": $detail" } ?: "")
                        }
                        Text(text = text, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(Modifier.alpha(0.2f), DividerDefaults.Thickness, DividerDefaults.color)
    }
}


@Composable
fun WrprcRequestValidationHeading(data: WrprcRequestValidationHeadingData, content: @Composable () -> Unit) {
    ExpandableCard(
        text = data.text,
        icon = data.icon,
        expanded = data.expanded,
        containerColor = data.containerColor,
        contentColor = data.contentColor,
        content = content
    )
}

@Composable
fun WrprcRequestValidation(wrpValidationResult: WrpValidationResult? = null) {
    wrpValidationResult?.displayInfo?.let {
        WrprcDetailsCard(it)
    }
    wrpValidationResult?.toWrprcRequestValidationData()?.let { list ->
        val failed = list.any {
            it.validity == false
        }
        Column(modifier = Modifier.padding(start = 0.dp)) {
            // The colors of the trust status next to it, with red for an invalid request, which is a fault
            // Closed by default, like the trust card
            val data = when (failed) {
                true -> WrprcRequestValidationHeadingData(
                    text = list.headingText(),
                    // A fault like an invalid signature, so the same icon
                    icon = Icons.Filled.Close,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    expanded = false
                )

                else -> {
                    val (containerColor, contentColor) = TrustState.TRUSTED.statusColors()
                    WrprcRequestValidationHeadingData(
                        text = list.headingText(),
                        icon = TrustState.TRUSTED.statusIcon,
                        containerColor = containerColor,
                        contentColor = contentColor,
                        expanded = false
                    )
                }
            }

            WrprcRequestValidationHeading(data = data) {
                Column(modifier = Modifier.padding(start = 20.dp)) {
                    WrprcRequestValidationSummary(list)
                }
            }
        }

    }
}

@Composable
fun WrprcDetailsCard(displayInfo: WrpDisplayInfo) {
    ExpandableCard(
        stringResource(Res.string.label_registration_cert_more_details),
        icon = Icons.Outlined.Info,
        expanded = false
    ) {
        Column(modifier = Modifier.padding(start = 20.dp, top = 16.dp)) {
            val paddingModifier = Modifier.padding(bottom = 16.dp)
            displayInfo.name?.let { name ->
                LabeledText(
                    label = stringResource(Res.string.label_registration_cert_details_trade_name),
                    text = name,
                    modifier = paddingModifier,
                )
            }
            displayInfo.purpose?.getCurrentLocalization()?.let { name ->
                LabeledText(
                    label = stringResource(Res.string.label_registration_cert_details_purpose),
                    text = name,
                    modifier = paddingModifier,
                    maxLines = 2
                )
            }
            displayInfo.country?.let { country ->
                LabeledText(
                    label = stringResource(Res.string.label_registration_cert_details_country),
                    text = country,
                    modifier = paddingModifier,
                    maxLines = 2
                )
            }
            displayInfo.infoUri?.let { infoUri ->
                LabeledHyperlinkText(
                    label = stringResource(Res.string.label_registration_cert_info_uri),
                    text = infoUri,
                    url = infoUri,
                    modifier = paddingModifier,
                )
            }
            displayInfo.supportUri?.let { supportUri ->
                LabeledHyperlinkText(
                    label = stringResource(Res.string.label_registration_cert_support_uri),
                    text = supportUri,
                    url = supportUri,
                    modifier = paddingModifier,
                )
            }
        }
    }
}

data class WrprcRequestValidationHeadingData(
    val text: String,
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
    val expanded: Boolean
)

data class WrprcRequestValidationData(
    val text: StringResource,
    /** `null` if the request does not contain what is validated, which is not an error */
    val validity: Boolean?,
    val infoValid: StringResource,
    val infoInvalid: StringResource,
    val infoMissing: StringResource? = null,
    /** Why the validation failed, e.g. that the certificate could not be parsed */
    val errors: List<WrpValidationError> = emptyList(),
    /** Short description of the failure, shown in the heading when this is the first check that failed */
    val headline: StringResource? = null,
)

sealed interface WrpValidationError {
    /** Message of an exception */
    data class Message(val text: String) : WrpValidationError

    data class Resource(val text: StringResource, val detail: String? = null) : WrpValidationError
}


fun WrpValidationResult.toWrprcRequestValidationData(): List<WrprcRequestValidationData> {
    val accessCertificateData = WrprcRequestValidationData(
        text = Res.string.label_access_cert,
        validity = if (accessCertificateMissing) null else accessCertificate?.validLinkage == true,
        infoValid = Res.string.info_text_access_cert_valid,
        infoInvalid = Res.string.info_text_access_cert_invalid,
        infoMissing = Res.string.info_text_access_cert_missing,
        errors = listOfNotNull(accessCertificateError?.let { WrpValidationError.Message(it) }),
        headline = Res.string.heading_wrp_access_cert_invalid,
    )
    if (registrationCertificateMissing) return listOf(
        accessCertificateData,
        WrprcRequestValidationData(
            text = Res.string.label_registration_cert,
            validity = null,
            infoValid = Res.string.info_text_registration_cert_valid,
            infoInvalid = Res.string.info_text_registration_cert_invalid,
            infoMissing = Res.string.info_text_registration_cert_missing,
        ),
    )
    val registrationCertificate = registrationCertificate ?: return listOf(
        accessCertificateData,
        WrprcRequestValidationData(
            text = Res.string.label_registration_cert,
            validity = false,
            infoValid = Res.string.info_text_registration_cert_valid,
            infoInvalid = Res.string.info_text_registration_cert_not_evaluated,
            errors = listOfNotNull(registrationCertificateError?.let { WrpValidationError.Message(it) }),
            headline = Res.string.heading_wrp_registration_cert_not_evaluated,
        ),
    )
    val requestResults = registrationCertificate.requestDataValidationResults
    return listOf(
        accessCertificateData,
        WrprcRequestValidationData(
            text = Res.string.label_registration_cert,
            validity = registrationCertificate.certificates.all { it.valid },
            infoValid = Res.string.info_text_registration_cert_valid,
            infoInvalid = Res.string.info_text_registration_cert_invalid,
            errors = registrationCertificate.certificates.flatMap { it.errors() },
            headline = Res.string.heading_wrp_registration_cert_invalid,
        ),
        WrprcRequestValidationData(
            text = Res.string.label_registration_cert_credential_typ,
            validity = requestResults.all { it.validity?.credentialTypeValidity == true },
            infoValid = Res.string.info_text_registration_cert_typ_valid,
            infoInvalid = Res.string.info_text_registration_cert_typ_invalid,
            errors = requestResults.mapNotNull { result -> result.error?.let { WrpValidationError.Message(it) } },
            headline = Res.string.heading_wrp_credential_type_invalid,
        ),
        WrprcRequestValidationData(
            text = Res.string.label_registration_cert_attributes,
            validity = requestResults.all { result ->
                result.validity?.credentialAttributesValidity?.all { it.second } == true
            },
            infoValid = Res.string.info_text_registration_cert_requested_claim_valid,
            infoInvalid = Res.string.info_text_registration_cert_requested_claim_invalid,
            headline = Res.string.heading_wrp_attributes_invalid,
        ),
    )
}

/**
 * Text of the heading above [this] list of checks: the first failure and how many other checks failed, or what
 * could be verified.
 */
@Composable
internal fun List<WrprcRequestValidationData>.headingText(): String {
    val failures = filter { it.validity == false }
    val first = failures.firstOrNull() ?: return stringResource(
        if (any { it.text == Res.string.label_registration_cert && it.validity == true }) {
            Res.string.heading_wrp_valid
        } else {
            Res.string.heading_wrp_access_cert_valid
        }
    )
    val headline = stringResource(first.headline ?: Res.string.label_registration_cert_invalid)
    return if (failures.size > 1) {
        stringResource(Res.string.heading_wrp_more_failures, headline, failures.size - 1)
    } else {
        headline
    }
}

private fun WrprcCertificateValidation.errors(): List<WrpValidationError> = error?.let {
    listOf(WrpValidationError.Message(it))
} ?: listOfNotNull(
    WrpValidationError.Resource(Res.string.info_text_registration_cert_not_linked).takeUnless { validLinkage },
    when (tokenStatus) {
        WrpTokenStatus.REVOKED -> WrpValidationError.Resource(Res.string.info_text_registration_cert_revoked)
        WrpTokenStatus.SUSPENDED -> WrpValidationError.Resource(Res.string.info_text_registration_cert_suspended)
        WrpTokenStatus.OTHER -> WrpValidationError.Resource(Res.string.info_text_registration_cert_status_invalid)
        WrpTokenStatus.VALID, null -> null
    },
    tokenStatusError?.let {
        WrpValidationError.Resource(Res.string.info_text_registration_cert_status_unknown, detail = it)
    },
)
