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
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import at.asitplus.valera.resources.content_description_certificate_details
import ui.composables.CertificateDetailsDialog
import at.asitplus.signum.indispensable.pki.X509Certificate
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.width
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.info_text_registration_cert_suspended
import at.asitplus.valera.resources.info_text_registration_cert_status_unknown
import at.asitplus.valera.resources.info_text_registration_cert_status_invalid
import at.asitplus.valera.resources.info_text_registration_cert_revoked
import at.asitplus.valera.resources.info_text_registration_cert_not_linked
import at.asitplus.valera.resources.heading_wrp_evaluating
import at.asitplus.valera.resources.heading_wrp_not_provided
import at.asitplus.valera.resources.trust_signer
import at.asitplus.valera.resources.label_wrp_signers
import at.asitplus.valera.resources.info_text_wrp_signers_not_checked
import at.asitplus.valera.resources.heading_wrp_signers_not_checked
import at.asitplus.valera.resources.info_text_wrp_different_relying_parties
import at.asitplus.valera.resources.heading_wrp_different_relying_parties
import at.asitplus.valera.resources.label_wrp_registered_credentials
import at.asitplus.valera.resources.info_text_wrp_credentials_not_covered
import at.asitplus.valera.resources.text_wrp_non_eudi_signers
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
import at.asitplus.wallet.lib.agent.validation.relyingParty.registrationCertificate.WrpCredentialRequest
import at.asitplus.wallet.lib.data.SingleClaimReference
import at.asitplus.wallet.lib.data.MdocClaimReference
import at.asitplus.wallet.lib.data.JsonClaimReference
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
        list.forEachIndexed { index, data ->
            WrprcRequestValidationDataCard(data, showDivider = index < list.lastIndex)
        }
    }
}

@Composable
fun WrprcRequestValidationDataCard(data: WrprcRequestValidationData, showDivider: Boolean = true) {
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
        var showCertificate by remember { mutableStateOf(false) }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.alpha(0.5f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(data.text), color = color, modifier = Modifier.weight(1f))
            // Like the signer cards of the trust card
            data.certificate?.let {
                IconButton(onClick = { showCertificate = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = stringResource(
                            Res.string.content_description_certificate_details,
                            stringResource(data.text),
                        ),
                    )
                }
            }
        }
        data.certificate?.takeIf { showCertificate }?.let {
            CertificateDetailsDialog(
                signerId = data.certificateSignerId,
                certificate = it,
                onDismiss = { showCertificate = false },
            )
        }
        // Aligned with the title, next to the icon
        Column(modifier = Modifier.padding(start = 32.dp)) {
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
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (showDivider) {
            HorizontalDivider(Modifier.alpha(0.2f), DividerDefaults.Thickness, DividerDefaults.color)
        }
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

/** Shown while the relying party is evaluated, like the trust evaluation. */
@Composable
fun WrprcRequestValidationEvaluating() {
    val (containerColor, contentColor) = TrustState.EVALUATING.statusColors()
    ExpandableCard(
        text = stringResource(Res.string.heading_wrp_evaluating),
        icon = TrustState.EVALUATING.statusIcon,
        expanded = false,
        containerColor = containerColor,
        contentColor = contentColor,
        content = null,
    )
}

/**
 * Shown for a request without any certificate of the relying party, which is not an error: the registration
 * certificate is optional, see the rows for what is missing.
 */
@Composable
private fun WrprcRequestValidationNotProvided(wrpValidationResult: WrpValidationResult?) {
    val rows = if (wrpValidationResult == null || wrpValidationResult.multiSigned) {
        listOf(missingRegistrationCertificateData())
    } else {
        wrpValidationResult.toWrprcRequestValidationData()
    }
    ExpandableCard(
        text = stringResource(Res.string.heading_wrp_not_provided),
        icon = Icons.Outlined.Info,
        expanded = false,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        WrprcRequestValidationCards {
            WrprcRequestValidationGroupCard(
                WrprcRequestValidationGroup(
                    clientId = wrpValidationResult?.clientId?.takeUnless { wrpValidationResult.multiSigned },
                    displayInfo = null,
                    rows = rows,
                )
            )
            wrpValidationResult?.NonEudiSigners()
        }
    }
}

/** Content of the request card, laid out like the signer cards of the trust card. */
@Composable
private fun WrprcRequestValidationCards(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, top = 12.dp, bottom = 12.dp)) {
        content()
    }
}

/** The checks of one relying party identity in one card, like its signer card in the trust card. */
@Composable
private fun WrprcRequestValidationGroupCard(group: WrprcRequestValidationGroup, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            group.clientId?.let { clientId ->
                Text(
                    text = stringResource(Res.string.trust_signer),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = clientId,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            group.displayInfo?.let { WrprcDetailsCard(it) }
            WrprcRequestValidationSummary(group.rows)
        }
    }
}

@Composable
private fun WrpValidationResult.NonEudiSigners() {
    signers.filterNot { it.identity.isEudiIdentity }.takeIf { it.isNotEmpty() }?.let {
        Text(
            text = stringResource(Res.string.text_wrp_non_eudi_signers, it.joinToString { it.clientId }),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
fun WrprcRequestValidation(wrpValidationResult: WrpValidationResult? = null) {
    if (wrpValidationResult == null || wrpValidationResult.nothingProvided) {
        return WrprcRequestValidationNotProvided(wrpValidationResult)
    }
    val groups = wrpValidationResult.toWrprcRequestValidationGroups()
    val requestRows = wrpValidationResult.toRequestValidationData()
    val list = groups.flatMap { it.rows } + requestRows
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
            WrprcRequestValidationCards {
                groups.forEachIndexed { index, group ->
                    WrprcRequestValidationGroupCard(
                        group,
                        modifier = Modifier.padding(bottom = if (index < groups.lastIndex) 8.dp else 0.dp),
                    )
                }
                // checks of the request as a whole, apart from those of each relying party identity
                if (requestRows.isNotEmpty()) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        WrprcRequestValidationSummary(requestRows)
                    }
                }
                wrpValidationResult.NonEudiSigners()
            }
        }
    }
}

/** Checks of one identity, headed by its client identifier for a signer of a multisigned request. */
data class WrprcRequestValidationGroup(
    val clientId: String?,
    /** Details from the registration certificate, only if it is bound to a valid access certificate. */
    val displayInfo: WrpDisplayInfo?,
    val rows: List<WrprcRequestValidationData>,
)

/**
 * The single identity of a request, or every EU identity of a multisigned request, see
 * [WrpValidationResult.isEudiIdentity]: each of them has to pass all checks.
 */
fun WrpValidationResult.toWrprcRequestValidationGroups(): List<WrprcRequestValidationGroup> =
    if (multiSigned) {
        eudiSigners.map { signer ->
            val identity = signer.identity
            WrprcRequestValidationGroup(
                clientId = signer.clientId,
                displayInfo = identity.displayInfo.takeIf {
                    identity.accessCertificate != null &&
                            identity.registrationCertificate?.certificates?.all { it.valid } == true
                },
                rows = identity.toWrprcRequestValidationData(),
            )
        }
    } else {
        listOf(
            WrprcRequestValidationGroup(
                clientId = clientId,
                displayInfo = displayInfo,
                rows = toWrprcRequestValidationData(),
            )
        )
    }

/** Checks of the request as a whole, shown only if they fail or have something to point out. */
fun WrpValidationResult.toRequestValidationData(): List<WrprcRequestValidationData> = listOfNotNull(
    signersError?.let {
        WrprcRequestValidationData(
            text = Res.string.label_wrp_signers,
            validity = false,
            infoValid = Res.string.info_text_wrp_signers_not_checked,
            infoInvalid = Res.string.info_text_wrp_signers_not_checked,
            errors = listOf(WrpValidationError.Message(it)),
            headline = Res.string.heading_wrp_signers_not_checked,
        )
    },
    WrprcRequestValidationData(
        text = Res.string.label_wrp_signers,
        validity = false,
        infoValid = Res.string.info_text_wrp_different_relying_parties,
        infoInvalid = Res.string.info_text_wrp_different_relying_parties,
        headline = Res.string.heading_wrp_different_relying_parties,
    ).takeIf { differentRelyingParties },
    // Only information: a request may also ask for credentials of other trust frameworks, which no registration
    // certificate of the EU covers, see OpenID4VP 1.0, A.3.2.2
    WrprcRequestValidationData(
        text = Res.string.label_wrp_registered_credentials,
        validity = null,
        infoValid = Res.string.info_text_wrp_credentials_not_covered,
        infoInvalid = Res.string.info_text_wrp_credentials_not_covered,
        infoMissing = Res.string.info_text_wrp_credentials_not_covered,
        errors = uncoveredCredentialQueryIds.map {
            WrpValidationError.Message("$it: not covered by a registration certificate")
        },
    ).takeIf { uncoveredCredentialQueryIds.isNotEmpty() },
)

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
    /** Certificate that was checked, to look at it, e.g. the access certificate. */
    val certificate: X509Certificate? = null,
    /** The signer using [certificate], if it is the signer's own certificate, see [CertificateDetailsDialog]. */
    val certificateSignerId: String? = null,
)

sealed interface WrpValidationError {
    /** Message of an exception */
    data class Message(val text: String) : WrpValidationError

    data class Resource(val text: StringResource, val detail: String? = null) : WrpValidationError
}


/** Identifier of [this] credential request in technical details: its DCQL query id or ISO doc type. */
private fun WrpCredentialRequest.displayId(): String = when (this) {
    is WrpCredentialRequest.WrpDcqlCredentialQuery -> query.id.string
    is WrpCredentialRequest.WrpDocRequest -> query.itemsRequest.value.docType
}

private fun SingleClaimReference.displayPath(): String = when (this) {
    is MdocClaimReference -> "$namespace/$claimName"
    is JsonClaimReference -> normalizedJsonPath.toString()
}

/** Row for a registration certificate the request does not carry, which is not an error. */
fun missingRegistrationCertificateData() = WrprcRequestValidationData(
    text = Res.string.label_registration_cert,
    validity = null,
    infoValid = Res.string.info_text_registration_cert_valid,
    infoInvalid = Res.string.info_text_registration_cert_invalid,
    infoMissing = Res.string.info_text_registration_cert_missing,
)

fun WrpValidationResult.toWrprcRequestValidationData(): List<WrprcRequestValidationData> {
    val accessCertificateData = WrprcRequestValidationData(
        text = Res.string.label_access_cert,
        validity = if (accessCertificateMissing) null else accessCertificate?.validLinkage == true,
        infoValid = Res.string.info_text_access_cert_valid,
        infoInvalid = Res.string.info_text_access_cert_invalid,
        infoMissing = Res.string.info_text_access_cert_missing,
        errors = listOfNotNull(accessCertificateError?.let { WrpValidationError.Message(it) }),
        headline = Res.string.heading_wrp_access_cert_invalid,
        certificate = accessCertificateLeaf,
        certificateSignerId = clientId,
    )
    if (registrationCertificateMissing) return listOf(accessCertificateData, missingRegistrationCertificateData())
    val registrationCertificate = registrationCertificate ?: return listOf(
        accessCertificateData,
        WrprcRequestValidationData(
            text = Res.string.label_registration_cert,
            validity = false,
            infoValid = Res.string.info_text_registration_cert_valid,
            infoInvalid = Res.string.info_text_registration_cert_not_evaluated,
            errors = listOfNotNull(registrationCertificateError?.let { WrpValidationError.Message(it) }),
            headline = Res.string.heading_wrp_registration_cert_not_evaluated,
            certificate = registrationCertificateSigner,
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
            certificate = registrationCertificateSigner,
        ),
        WrprcRequestValidationData(
            text = Res.string.label_registration_cert_credential_typ,
            validity = requestResults.all { it.validity?.credentialTypeValidity == true },
            infoValid = Res.string.info_text_registration_cert_typ_valid,
            infoInvalid = Res.string.info_text_registration_cert_typ_invalid,
            errors = requestResults.mapNotNull { result -> result.error?.let { WrpValidationError.Message(it) } } +
                requestResults.filter { it.validity?.credentialTypeValidity == false }
                    .map { WrpValidationError.Message("${it.request.displayId()}: credential type not registered") },
            headline = Res.string.heading_wrp_credential_type_invalid,
        ),
        WrprcRequestValidationData(
            text = Res.string.label_registration_cert_attributes,
            validity = requestResults.all { result ->
                result.validity?.credentialAttributesValidity?.all { it.second } == true
            },
            infoValid = Res.string.info_text_registration_cert_requested_claim_valid,
            infoInvalid = Res.string.info_text_registration_cert_requested_claim_invalid,
            errors = requestResults.flatMap { result ->
                result.validity?.credentialAttributesValidity.orEmpty().filterNot { it.second }
                    .map { (claim, _) ->
                        WrpValidationError.Message("${result.request.displayId()}: ${claim.displayPath()} not registered")
                    }
            },
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
} ?: failedChecks.map { WrpValidationError.Message(it) } + listOfNotNull(
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
