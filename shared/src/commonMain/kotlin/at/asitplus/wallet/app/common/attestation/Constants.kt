package at.asitplus.wallet.app.common.attestation

import kotlin.time.Duration.Companion.days

const val KS_ALIAS_WIA = "ALIAS_WIA"
const val WALLET_SOLUTION_OID = "2.25.210184084534939142470042512176304499012"
const val PATH_CHALLENGE = "api/v1/challenge"
const val PATH_INSTANCE = "api/v1/instanceAttestation"
const val PATH_UNIT = "api/v1/keyAttestation"
const val PATH_NONCE = "api/v1/nonce"
val PREFERRED_DEFAULT_TTL = 31.days
