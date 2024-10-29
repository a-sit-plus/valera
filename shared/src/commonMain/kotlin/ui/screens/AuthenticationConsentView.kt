package ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import at.asitplus.dif.InputDescriptor
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.misc.getRequestOptionParameters
import at.asitplus.wallet.app.common.third_parts.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.cor.CertificateOfResidenceScheme
import at.asitplus.wallet.eprescription.EPrescriptionScheme
import at.asitplus.wallet.eupid.EuPidScheme
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import at.asitplus.wallet.por.PowerOfRepresentationScheme
import compose_wallet_app.shared.generated.resources.Res
import compose_wallet_app.shared.generated.resources.biometric_authentication_prompt_for_data_transmission_consent_subtitle
import compose_wallet_app.shared.generated.resources.biometric_authentication_prompt_for_data_transmission_consent_title
import compose_wallet_app.shared.generated.resources.error_authentication_at_sp_failed
import data.RequestOptionParameters
import data.credentials.CertificateOfResidenceCredentialAttributeTranslator
import data.credentials.EPrescriptionCredentialAttributeTranslator
import data.credentials.EuPidCredentialAttributeTranslator
import data.credentials.IdAustriaCredentialAttributeTranslator
import data.credentials.MobileDrivingLicenceCredentialAttributeTranslator
import data.credentials.PowerOfRepresentationCredentialAttributeTranslator
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import ui.views.AuthenticationConsentView

@Composable
fun StatefulAuthenticationConsentView(
    vm: AuthenticationConsentViewModel
) {
    val vm = remember { vm }

    vm.walletMain.cryptoService.onUnauthenticated = vm.navigateUp

    val descriptors: Collection<InputDescriptor> = vm.authenticationRequest.parameters.presentationDefinition?.inputDescriptors ?: listOf()

    val list: List<RequestOptionParameters> = descriptors.mapNotNull {
        it.getRequestOptionParameters()
    }

    val consentAttributes: List<ConsentAttributes> = list.mapNotNull { params ->
        val att = params.attributes?.map { NormalizedJsonPath() + it }?.mapNotNull {
            when (params.resolved?.first) {
                is MobileDrivingLicenceScheme -> { MobileDrivingLicenceCredentialAttributeTranslator.translate(it) }
                is CertificateOfResidenceScheme -> { CertificateOfResidenceCredentialAttributeTranslator.translate(it) }
                is PowerOfRepresentationScheme -> { PowerOfRepresentationCredentialAttributeTranslator.translate(it) }
                is EPrescriptionScheme -> { EPrescriptionCredentialAttributeTranslator.translate(it) }
                is EuPidScheme -> { EuPidCredentialAttributeTranslator.translate(it) }
                is IdAustriaScheme -> { IdAustriaCredentialAttributeTranslator.translate(it) }
                else -> { IdAustriaCredentialAttributeTranslator.translate(it) }
            }
        }
        val format = params.resolved?.second?.name
        val scheme = params.resolved?.first?.let { it.sdJwtType ?: it.isoDocType ?: it.vcType } ?: "Unknown"
        if (format != null && att != null) {
            ConsentAttributes(scheme = scheme, format = format, attributes = att)
        } else {
            null
        }
    }

    AuthenticationConsentView(
        spName = vm.spName,
        spLocation = vm.spLocation,
        spImage = vm.spImage,
        consentAttributes = consentAttributes,
        navigateUp = vm.navigateUp,
        cancelAuthentication = vm.navigateUp,
        consentToDataTransmission = {
            vm.walletMain.scope.launch {
                try {
                    Napier.d { "signed!" }
                    vm.walletMain.cryptoService.promptText =
                        getString(Res.string.biometric_authentication_prompt_for_data_transmission_consent_title)
                    vm.walletMain.cryptoService.promptSubtitle =
                        getString(Res.string.biometric_authentication_prompt_for_data_transmission_consent_subtitle)
                    vm.walletMain.presentationService.startSiop(vm.authenticationRequest)
                    vm.navigateUp()
                    vm.navigateToAuthenticationSuccessPage()
                } catch (e: Throwable) {
                    vm.walletMain.errorService.emit(e)
                    vm.walletMain.snackbarService.showSnackbar(getString(Res.string.error_authentication_at_sp_failed))
                }
            }
        }
    )
}

class ConsentAttributes(val scheme: String, val format: String, val attributes: List<StringResource>)