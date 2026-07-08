package at.asitplus.wallet.app.common.relyingParty

import androidx.compose.ui.text.intl.Locale
import at.asitplus.catchingUnwrapped
import at.asitplus.data.NonEmptyList.Companion.toNonEmptyList
import at.asitplus.dif.InputDescriptor
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment.NameSegment
import at.asitplus.openid.dcql.DCQLCredentialQuery
import at.asitplus.openid.dcql.DCQLIsoMdocCredentialMetadataAndValidityConstraints
import at.asitplus.openid.dcql.DCQLSdJwtCredentialMetadataAndValidityConstraints
import at.asitplus.wallet.app.common.referenceValues
import at.asitplus.wallet.app.common.relyingParty.data.WrpCredentialMeta
import at.asitplus.wallet.app.common.relyingParty.data.WrpLangStringDto
import at.asitplus.wallet.app.common.vctConstraint
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.ISO_MDOC
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.SD_JWT
import data.credentials.JsonClaimReference
import data.credentials.MdocClaimReference


fun NormalizedJsonPath.toSingleClaimReference(representation: ConstantIndex.CredentialRepresentation) =
    catchingUnwrapped {
        when (representation) {
            SD_JWT -> {
                JsonClaimReference(this)
            }

            ISO_MDOC -> {
                val path = this
                val memberName = (path.segments.last() as NameSegment).memberName
                val namespace = (path.segments.first() as NameSegment).memberName
                MdocClaimReference(namespace = namespace, claimName = memberName)
            }

            else -> {
                throw Throwable("Unsupported representation type")
            }
        }
    }.getOrNull()

fun DCQLCredentialQuery.getMetadata(): WrpCredentialMeta =
    when (val meta = this.meta) {
        is DCQLIsoMdocCredentialMetadataAndValidityConstraints -> WrpCredentialMeta.WrpDocType(meta.doctypeValue)
        is DCQLSdJwtCredentialMetadataAndValidityConstraints -> WrpCredentialMeta.WrpVctType(vctValues = meta.vctValues.toNonEmptyList())
        else -> throw IllegalStateException("")
    }

fun InputDescriptor.getMetadata(): WrpCredentialMeta =
    when {
        this.format?.msoMdoc != null -> {
            WrpCredentialMeta.WrpDocType(doctypeValue = this.id)
        }

        this.format?.sdJwt != null -> {
            this.vctConstraint()?.filter?.referenceValues()?.let {
                if (it.isNotEmpty()) {
                    WrpCredentialMeta.WrpVctType(
                        vctValues = it.toList().toNonEmptyList()
                    )
                } else {
                    throw Throwable("Reference values empty!")
                }
            } ?: throw Throwable("Reference values null")
        }

        else -> throw IllegalStateException("Unable to build WrprcCredentialMeta")
    }

fun List<WrpLangStringDto>.getCurrentLocalization(): String? =
    this.firstOrNull {
        it.lang.lowercase().contains(Locale.current.language.lowercase())
    }?.value ?: this.firstOrNull()?.value