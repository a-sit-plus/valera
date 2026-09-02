package at.asitplus.wallet.app.common.relyingParty.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import at.asitplus.valera.resources.info_text_registration_cert_invalid
import at.asitplus.valera.resources.info_text_registration_cert_requested_claim_invalid
import at.asitplus.valera.resources.info_text_registration_cert_requested_claim_valid
import at.asitplus.valera.resources.info_text_registration_cert_typ_invalid
import at.asitplus.valera.resources.info_text_registration_cert_typ_valid
import at.asitplus.valera.resources.info_text_registration_cert_valid
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
import at.asitplus.valera.resources.label_registration_cert_valid
import at.asitplus.wallet.app.common.relyingParty.getCurrentLocalization
import at.asitplus.wallet.app.common.relyingParty.validation.WrpDisplayInfo
import at.asitplus.wallet.app.common.relyingParty.validation.WrpValidationResult
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import ui.composables.ExpandableCard
import ui.composables.LabeledHyperlinkText
import ui.composables.LabeledText
import ui.theme.LocalExtendedColors

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
    val expanded = remember { mutableStateOf(!data.validity) }
    Column(
        modifier = Modifier
            .clickable(onClick = { expanded.value = !expanded.value })
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            val (color, icon) = when (data.validity) {
                true -> Pair(LocalExtendedColors.current.validationDark.valid, Icons.Outlined.Check)
                else -> Pair(LocalExtendedColors.current.validationDark.invalid, Icons.Outlined.Clear)
            }

            Column {
                Row {
                    Text(text = stringResource(data.text), color = color)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.alpha(0.5f)
                        )
                    }
                }
                val density = LocalDensity.current
                AnimatedVisibility(
                    visible = expanded.value,
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
                    Column(modifier = Modifier.padding(horizontal = 0.dp)) {
                        Text(
                            when (data.validity) {
                                true -> stringResource(data.infoValid)
                                else -> stringResource(data.infoInvalid)
                            }
                        )
                    }

                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(Modifier.alpha(0.2f), DividerDefaults.Thickness, DividerDefaults.color)
            }
        }
    }
}


@Composable
fun WrprcRequestValidationHeading(data: WrprcRequestValidationHeadingData, content: @Composable () -> Unit) {
    ExpandableCard(
        text = stringResource(data.text),
        icon = data.icon,
        expanded = data.expanded,
        lightColor = data.lightColor,
        darkColor = data.darkColor,
        content = content
    )
}

@Composable
fun WrprcRequestValidation(wrpValidationResult: WrpValidationResult? = null) {
    wrpValidationResult?.displayInfo?.let {
        WrprcDetailsCard(it)
    }
    wrpValidationResult?.toWrprcRequestValidationData()?.let { list ->
        val expanded = list.any {
            !it.validity
        }
        Column(modifier = Modifier.padding(start = 0.dp)) {
            val extendedColors = LocalExtendedColors.current
            val data = when (expanded) {
                true -> WrprcRequestValidationHeadingData(
                    text = Res.string.label_registration_cert_invalid,
                    icon = Icons.Outlined.Warning,
                    darkColor = extendedColors.validationDark.invalid,
                    lightColor = extendedColors.validationLight.invalid,
                    expanded = expanded
                )

                else -> WrprcRequestValidationHeadingData(
                    text = Res.string.label_registration_cert_valid,
                    icon = Icons.Outlined.Check,
                    darkColor = extendedColors.validationDark.valid,
                    lightColor = extendedColors.validationLight.valid,
                    expanded = expanded
                )
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
    val text: StringResource,
    val icon: ImageVector,
    val darkColor: Color,
    val lightColor: Color,
    val expanded: Boolean
)

data class WrprcRequestValidationData(
    val text: StringResource,
    val validity: Boolean,
    val infoValid: StringResource,
    val infoInvalid: StringResource
)


fun WrpValidationResult.toWrprcRequestValidationData(): List<WrprcRequestValidationData> = listOf(
    WrprcRequestValidationData(
        text = Res.string.label_registration_cert,
        validity = this.wrpacValid == true && this.wrprcValid == true,
        infoValid = Res.string.info_text_registration_cert_valid,
        infoInvalid = Res.string.info_text_registration_cert_invalid
    ),
    WrprcRequestValidationData(
        text = Res.string.label_registration_cert_credential_typ,
        validity = this.validCredentialType,
        infoValid = Res.string.info_text_registration_cert_typ_valid,
        infoInvalid = Res.string.info_text_registration_cert_typ_invalid
    ),
    WrprcRequestValidationData(
        text = Res.string.label_registration_cert_attributes,
        validity = this.validAttributes,
        infoValid = Res.string.info_text_registration_cert_requested_claim_valid,
        infoInvalid = Res.string.info_text_registration_cert_requested_claim_invalid
    ),
)