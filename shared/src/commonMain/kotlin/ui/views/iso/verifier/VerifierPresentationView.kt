package ui.views.iso.verifier

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_mdl_driving_privilege_category_expired
import at.asitplus.valera.resources.error_mdl_driving_privilege_category_not_yet_valid
import at.asitplus.valera.resources.heading_label_received_data
import at.asitplus.valera.resources.info_text_credential_status_valid
import at.asitplus.wallet.app.common.decodeImage
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.iconLabel
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.healthid.HealthIdScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.IsoDocumentParsed
import at.asitplus.wallet.mdl.DrivingPrivilege
import at.asitplus.wallet.mdl.MobileDrivingLicenceDataElements
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.credentials.EuPidCredentialIsoMdocAdapter
import data.credentials.HealthIdCredentialIsoMdocAdapter
import data.credentials.MobileDrivingLicenceCredentialIsoMdocAdapter
import data.document.DrivingPrivilegeValidator
import data.document.DrivingPrivilegeValidator.getDrivingPrivilegeStatus
import data.document.RequestDocumentBuilder
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import ui.composables.BigSuccessText
import ui.composables.ErrorText
import ui.composables.LabeledText
import ui.composables.Logo
import ui.composables.PersonAttributeDetailCardHeading
import ui.composables.PersonAttributeDetailCardHeadingIcon
import ui.composables.buttons.NavigateUpButton
import ui.composables.credentials.CredentialCardLayout
import ui.composables.credentials.EuPidCredentialViewFromAdapter
import ui.composables.credentials.HealthIdViewFromAdapter
import ui.composables.credentials.MainCredentialIssue
import ui.composables.credentials.MobileDrivingLicenceCredentialViewFromAdapter
import ui.models.toCredentialFreshnessSummaryModel
import ui.theme.LocalExtendedColors
import ui.viewmodels.iso.VerifierViewModel
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class, ExperimentalResourceApi::class
)
@Composable
fun VerifierPresentationView(vm: VerifierViewModel) {
    val decodeImage: (ByteArray) -> Result<ImageBitmap> = { vm.walletMain.platformAdapter.decodeImage(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(Res.string.heading_label_received_data),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Logo(onClick = vm.onClickLogo)
                    }
                },
                navigationIcon = { NavigateUpButton(vm.navigateUp) }
            )
        }
    ) { scaffoldPadding ->
        Box(modifier = Modifier.padding(scaffoldPadding)) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                vm.responseDocumentList.forEach { isoDocParsed ->
                    IsoMdocCredentialView(isoDocParsed, decodeImage)
                }
            }
        }
    }
}

@Composable
fun IsoMdocCredentialView(
    isoDocParsed: IsoDocumentParsed,
    decodeImage: (ByteArray) -> Result<ImageBitmap>
) {
    val isCredentialFresh = isoDocParsed.freshnessSummary.isFresh

    CredentialCardLayout(
        colors = if (isCredentialFresh) {
            val extendedColors = LocalExtendedColors.current
            CardDefaults.elevatedCardColors(
                containerColor = extendedColors.successContainer,
                contentColor = extendedColors.onSuccessContainer
            )
        } else {
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        },
        modifier = Modifier.padding(end = 16.dp, start = 16.dp, bottom = 16.dp)
    ) {
        isoDocParsed.document.issuerSigned.namespaces?.forEach { (namespaceKey, entries) ->
            val sortedEntries = entries.entries
                .sortedBy { it.value.elementIdentifier }
                .associate { it.value.elementIdentifier to it.value.elementValue }
            val namespaces = mapOf(namespaceKey to sortedEntries)
            val scheme = RequestDocumentBuilder.getDocTypeConfig(isoDocParsed.document.docType)?.scheme

            PersonAttributeDetailCardHeading(
                icon = { PersonAttributeDetailCardHeadingIcon(scheme.iconLabel()) },
                title = {
                    LabeledText(
                        label = ConstantIndex.CredentialRepresentation.ISO_MDOC.uiLabel(),
                        text = scheme.uiLabel()
                    )
                }
            )

            when (scheme) {
                is MobileDrivingLicenceScheme -> {
                    MobileDrivingLicenceCredentialViewFromAdapter(
                        MobileDrivingLicenceCredentialIsoMdocAdapter(namespaces, decodeImage)
                    )
                    val namespace = namespaces[MobileDrivingLicenceScheme.isoNamespace]
                    @Suppress("UNCHECKED_CAST")
                    val drivingPrivileges = namespace?.get(MobileDrivingLicenceDataElements.DRIVING_PRIVILEGES)
                        ?.let { it as? Array<DrivingPrivilege> }
                    drivingPrivileges?.forEach {
                        when(getDrivingPrivilegeStatus(it)) {
                            DrivingPrivilegeValidator.Status.EXPIRED ->
                                ErrorText(stringResource(Res.string.error_mdl_driving_privilege_category_expired, it.vehicleCategoryCode))
                            DrivingPrivilegeValidator.Status.NOT_YET_VALID ->
                                ErrorText(stringResource(Res.string.error_mdl_driving_privilege_category_not_yet_valid, it.vehicleCategoryCode))
                            DrivingPrivilegeValidator.Status.VALID -> {}
                        }
                    }
                }
                is EuPidScheme -> EuPidCredentialViewFromAdapter(
                    EuPidCredentialIsoMdocAdapter(namespaces, decodeImage, scheme)
                )
                is HealthIdScheme -> HealthIdViewFromAdapter(
                    HealthIdCredentialIsoMdocAdapter(namespaces)
                )
                else -> throw IllegalArgumentException("Unsupported scheme: $scheme")
            }
        }

        if (isCredentialFresh) {
            BigSuccessText(stringResource(Res.string.info_text_credential_status_valid))
        } else {
            MainCredentialIssue(
                isoDocParsed.freshnessSummary.toCredentialFreshnessSummaryModel()
            )
        }
    }
}
