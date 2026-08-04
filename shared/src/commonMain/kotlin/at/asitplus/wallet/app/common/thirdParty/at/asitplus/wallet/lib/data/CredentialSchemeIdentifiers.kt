package at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data

import at.asitplus.wallet.eupid.EU_PID_DOCTYPE
import at.asitplus.wallet.eupidsdjwt.EU_PID_SD_JWT_VCT
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.mdl.MDL_DOCTYPE

// Schemes resolved from type-metadata are Extracted*CredentialScheme, not the (deprecated, non-CredentialScheme)
// EuPidScheme/MobileDrivingLicenceScheme objects. Dispatch on the stable identifiers instead of the type.

/** EU PID in ISO mdoc representation (docType `eu.europa.ec.eudi.pid.1`). */
val CredentialScheme?.isEuPidIso: Boolean get() = this?.isoDocType == EU_PID_DOCTYPE

/** EU PID in SD-JWT representation (vct `urn:eudi:pid:1`). */
val CredentialScheme?.isEuPidSdJwt: Boolean get() = this?.sdJwtType == EU_PID_SD_JWT_VCT

/** EU PID in either representation. */
val CredentialScheme?.isEuPid: Boolean get() = isEuPidIso || isEuPidSdJwt

/** Mobile Driving Licence (ISO mdoc, docType `org.iso.18013.5.1.mDL`). */
val CredentialScheme?.isMdl: Boolean get() = this?.isoDocType == MDL_DOCTYPE
