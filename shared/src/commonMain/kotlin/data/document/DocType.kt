package data.document

import data.document.Namespace.EU_PID_NAMESPACE
import data.document.Namespace.MDL_NAMESPACE

object DocType {
    const val MDL_DOCTYPE = "org.iso.18013.5.1.mDL"
    const val IDENTITY = "org.iso.18013.5.1.identity"
    const val AGE_VERIFICATION = "org.iso.18013.5.1.ageverification"
    const val EU_PID_DOCTYPE = "eu.europa.ec.eudi.pid.1"

    val documentTypeToNameSpace: Map<String, String> = mapOf(
        MDL_DOCTYPE to MDL_NAMESPACE,
        IDENTITY to MDL_NAMESPACE,
        AGE_VERIFICATION to MDL_NAMESPACE,
        EU_PID_DOCTYPE to EU_PID_NAMESPACE
    )
}
