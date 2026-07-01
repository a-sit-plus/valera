package data.credentials

import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.jwt_claim_label_aud
import at.asitplus.valera.resources.jwt_claim_label_exp
import at.asitplus.valera.resources.jwt_claim_label_iat
import at.asitplus.valera.resources.jwt_claim_label_iss
import at.asitplus.valera.resources.jwt_claim_label_nbf
import at.asitplus.valera.resources.jwt_claim_label_sub
import org.jetbrains.compose.resources.StringResource

fun jwtClaimLabel(claimName: String): StringResource? = when (claimName) {
    "exp" -> Res.string.jwt_claim_label_exp
    "iss" -> Res.string.jwt_claim_label_iss
    "sub" -> Res.string.jwt_claim_label_sub
    "nbf" -> Res.string.jwt_claim_label_nbf
    "iat" -> Res.string.jwt_claim_label_iat
    "aud" -> Res.string.jwt_claim_label_aud
    else -> null
}
